package com.mrnadimi.audiostreamplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLivePlaybackSpeedControl;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsManifest;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import org.jetbrains.annotations.NotNull;


import static com.mrnadimi.audiostreamplayer.Constant.MEDIA_SESSION_TAG;
import static com.mrnadimi.audiostreamplayer.Constant.PLAYBACK_CHANNEL_ID;
import static com.mrnadimi.audiostreamplayer.Constant.PLAYBACK_NOTIFICATION_ID;

/**
 * Developer: Mohamad Nadimi
 * Company: Saghe
 * Website: https://www.mrnadimi.com
 * Created on 04 July 2021
 * <p>
 * Description: ...
 *
 * https://exoplayer.dev/customization.html
 */

public class PlayerService extends Service {

    private static final String TAG = "PlayerService";

    //Service Binder
    private final IBinder mBinder = new LocalBinder();
    //Object exoplayer
    private SimpleExoPlayer player;
    //Baraye notification
    private PlayerNotificationManager playerNotificationManager;
    private MediaSessionCompat mediaSession;
    private MediaSessionConnector mediaSessionConnector;
    //Baraye estefade dar exoplayer
    private DefaultDataSourceFactory dataSourceFactory;
    //Channeli ke service be an eshare mikonad
    private RadioChannel mCurrentStation;

    /**
     * In class activity shoma ast ke auto shenasai mishavad baraye ertebat ba activity
     */
    private Class<?> activityClass;



    private boolean isListenerAdded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        final Context context = this;

        if (null == player) {
            final DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);


            player = new SimpleExoPlayer.Builder(this)
                    /**
                     * Dar pakhsh kardan format HLS moshhkel vojud dasht va bad az 11 sanie ghat mishod
                     * Ba ezafe kardan in khat code moshkel hal shod
                     *
                     * https://exoplayer.dev/live-streaming.html
                     */
                    .setLivePlaybackSpeedControl(
                            new DefaultLivePlaybackSpeedControl.Builder()
                                    .setFallbackMaxPlaybackSpeed(1.04f)
                                    .build())
                    .build();
        }

        // Produces DataSource instances through which media data is loaded.
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, context.getPackageName()));

        if (mCurrentStation != null && activityClass!=null) {
            play(mCurrentStation, activityClass);
        }

        playerNotificationManager = new PlayerNotificationManager.Builder(
                context,
                PLAYBACK_NOTIFICATION_ID,
                PLAYBACK_CHANNEL_ID,
                new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @NotNull
                    @Override
                    public String getCurrentContentTitle(@NotNull Player player) {

                        String title = "";
                        if (mCurrentStation != null) {
                            title = mCurrentStation.name;
                        }

                        return title;
                    }

                    @SuppressLint("UnspecifiedImmutableFlag")
                    @Override
                    public PendingIntent createCurrentContentIntent(@NotNull Player player) {

                        Intent notificationIntent = new Intent(context, activityClass);
                        return PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    @Override
                    public String getCurrentContentText(@NotNull Player player) {

                        String description = "";
                        if (mCurrentStation != null) {
                            //description = StationUtil.getStationDescription(mCurrentStation);
                        }

                        return description;
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(@NotNull Player player, @NotNull PlayerNotificationManager.BitmapCallback callback) {
                        return BitmapFactory.decodeResource(getResources(), R.drawable.app_icon);
                    }
                }
        )
                .setChannelDescriptionResourceId(R.string.app_description)
                .setChannelNameResourceId(R.string.playback_channel_name)
                .setNotificationListener(
                        new PlayerNotificationManager.NotificationListener() {

                            @Override
                            public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                                stopSelf();
                            }

                            @Override
                            public void onNotificationPosted(int notificationId, @NotNull Notification notification, boolean ongoing) {
                                startForeground(notificationId, notification);
                            }
                        }
                )
                .build();

        playerNotificationManager.setPlayer(player);

        mediaSession = new MediaSessionCompat(context, MEDIA_SESSION_TAG);
        mediaSession.setActive(true);
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
        playerNotificationManager.setSmallIcon(R.drawable.app_icon);

        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setQueueNavigator(new TimelineQueueNavigator(mediaSession) {
            @NotNull
            @Override
            public MediaDescriptionCompat getMediaDescription(@NotNull Player player, int windowIndex) {
                return new MediaDescriptionCompat.Builder()
                        .setMediaId(mCurrentStation.name)
                        .setTitle(mCurrentStation.name)
                        .setSubtitle(mCurrentStation.country)
                        .setDescription(mCurrentStation.desc)
                        .setExtras(null)
                        .build();
            }
        });
        mediaSessionConnector.setPlayer(player);
    }

    @Override
    public void onDestroy() {
        mediaSession.release();
        mediaSessionConnector.setPlayer(null);
        playerNotificationManager.setPlayer(null);
        player.release();
        isListenerAdded = false;
        player = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Bundle serviceBundle = intent.getBundleExtra("Bundle");
        assert serviceBundle != null;
        RadioChannel stationData = serviceBundle.getParcelable("Station");
        Class<?> activityClass = (Class<?>) serviceBundle.getSerializable("activity_class");

        if (stationData != null) {
            play(stationData , activityClass);
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }



    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }


    public SimpleExoPlayer getPlayerInstance() {
        return player;
    }

    public void stopPlaying() {
        if (player != null) {
            player.stop();
        }
    }

    public void play(RadioChannel station , Class<?> activityClass) {
        mCurrentStation = station;
        this.activityClass = activityClass;
        play(station.url);
    }

    protected void play(final String url)
    {
        if (url == null || url.isEmpty())
            return;

        Uri uri = Uri.parse(url);
        if (player.isPlaying()) {
            player.stop();
        }

        // This is the MediaSource representing the media to be played.
        MediaSource mediaSource;
        MediaItem mediaItem =  MediaItem.fromUri(uri);

        // Makes a best guess to infer the type from a Uri.
        int mediaType = Util.inferContentType(uri);
        switch (mediaType) {
            case C.TYPE_DASH:
                Log.e(TAG, "MediaType: DASH,  URL: " + url);
                mediaSource = new DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                break;
            case C.TYPE_SS:
                Log.e(TAG, "MediaType: SS,  URL: " + url);
                mediaSource = new SsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                break;
            case C.TYPE_HLS:
                Log.e(TAG, "MediaType: HLS,  URL: " + url);
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory).setAllowChunklessPreparation(true).createMediaSource(mediaItem);
                //mediaSource = new HlsMediaSource(uri, dataSourceFactory, 1800000, new Handler(), new DefaultDrmSessionManager());
                break;
            case C.TYPE_OTHER:
            case C.TYPE_RTSP:
            default:
                Log.e(TAG, "MediaType: OTHER,  URL: " + url);
                mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                break;
        }

        // Prepare the player with the source.
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        playerNotificationManager.setSmallIcon(R.drawable.app_icon);
        playerNotificationManager.setPlayer(player);
        if (!isListenerAdded){
            isListenerAdded = true;
        }else{
            return;
        }
        player.addListener(new Player.Listener() {

            //HlsManifest hlsManifest;

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isPlaying){
                    AudioManager.getInstance(null , null).onStoping();
                }
            }



            /*@Override
            public void onTimelineChanged(Timeline timeline, int reason) {
                Object manifest = player.getCurrentManifest();
                if (manifest instanceof HlsManifest){
                    hlsManifest = (HlsManifest) manifest;
                    // Do something with the manifest.
                    Log.e("------" , hlsManifest.mediaPlaylist.startTimeUs+"");
                    Log.e("------" , hlsManifest.mediaPlaylist.targetDurationUs+"");
                    Log.e("------" , hlsManifest.mediaPlaylist.getEndTimeUs()+"");
                    //player.seekToDefaultPosition();
                    //player.seekTo(0);
                    //player.prepare();
                }else{
                    hlsManifest = null;
                }
            }*/

        });


    }
}
