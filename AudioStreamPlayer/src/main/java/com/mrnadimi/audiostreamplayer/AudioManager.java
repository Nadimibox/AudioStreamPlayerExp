package com.mrnadimi.audiostreamplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.util.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Developer: Mohamad Nadimi
 * Company: Saghe
 * Website: https://www.mrnadimi.com
 * Created on 04 July 2021
 * <p>
 * Description: Class morede nazar shomast ke manande yek ghif amal mikonad va hame
 * faliathaye shoma dar in class khahad bud
 */
public class AudioManager{

    //Tag marbut be log
    private final static String TAG = "AudioManager";

    //Channeli ke in class be an eshare mikonad
    private RadioChannel mCurrentChannel;

    // Exo Player
    protected SimpleExoPlayer player;

    //Service ma ke baese play shodan stream ha mishavad
    private PlayerService playerService;
    //Intent mortabet ba service
    private Intent serviceIntent;
    //in object be ma neshan midahad ke amaliat buffering dar che vaziati ast
    private boolean isBound = false;


    private long lastTotalRxBytes = 0;
    /**
     * Daryafte
     */
    private long lastTimeStamp = 0;
    /**
     * Daryafte akharin vaziate taghir faal
     */
    private long lastBarActiveTimeStamp = 0;

    protected boolean isBuffering = false;

    private final ServiceConnection serviceConnection;

    protected int mPlaybackStatus;

    protected static boolean isCheckingThreadRunning = false;
    protected Thread checkingThread;

    public static final int CHECK_INTERVAL = 1000;

    Handler handler = new Handler(Looper.getMainLooper());

    WeakReference<Activity> mActivityWeakReference;

    private final AudioManagerListener audioManagerListener;

    /**
     * Thread test sorate net
     */
    Runnable checkingRunnable = () -> {
        while (isCheckingThreadRunning) {
            try {
                if (isBuffering)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getBufferingInfo(mActivityWeakReference.get());
                        }
                    });
                }
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };




    private static AudioManager audioManager;

    public static AudioManager getInstance(Activity activity , AudioManagerListener listener){
        if (audioManager == null){
            audioManager = new AudioManager(activity , listener);
        }
        return audioManager;
    }

    private AudioManager(Activity activity, AudioManagerListener listener) {
        mActivityWeakReference = new WeakReference<>(activity);
        this.audioManagerListener = listener;
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                PlayerService.LocalBinder binder = (PlayerService.LocalBinder) iBinder;
                playerService = binder.getService();
                isBound = true;
                initPlayer();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                isBound = false;
                //Stop service notification
                playerService.stopForeground(true);
                //Stop service
                playerService.stopSelf();
                playerService = null;
            }
        };
        startCheckingThread();
    }

    /**
     * Tanzimate avalie player
     */
    private void initPlayer() {
        if (isBound) {
            player = playerService.getPlayerInstance();
            player.addListener(listener);
        }
    }
    //Pull all links from the body for easy retrieval
    private List<String> pullLinks(String text) {
        ArrayList<String> links = new ArrayList<>();

        String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        while(m.find()) {
            String urlStr = m.group();
            if (urlStr.startsWith("(") && urlStr.endsWith(")"))
            {
                urlStr = urlStr.substring(1, urlStr.length() - 1);
            }
            links.add(urlStr);
        }
        return links;
    }


    private final Player.Listener listener = new Player.Listener() {
        public void onPlaybackStateChanged(int state) {
            Log.d(TAG, "onPlayerStateChanged:  playbackState:" + state);
            switch (state) {
                case Player.STATE_BUFFERING:
                    mPlaybackStatus = PlaybackStatus.LOADING;
                    showBufferingInfo();
                    onBuffering();
                    break;
                case Player.STATE_ENDED:
                    hideBufferingInfo();
                    mPlaybackStatus = PlaybackStatus.STOPPED;
                    onLoading(false);
                    onStoping();
                    break;
                case Player.STATE_READY:
                    mPlaybackStatus = PlaybackStatus.PLAYING;
                    hideBufferingInfo();
                    onLoading(false);
                    onPlaying();
                    break;
                //Zamani ke paksh ba moshkel movajeh shavad
                case Player.STATE_IDLE:
                default:
                    if (mPlaybackStatus == PlaybackStatus.PAUSED || mPlaybackStatus == PlaybackStatus.STOPPED){
                        return;
                    }
                    /**
                     * Ersal request va daryafte etelaate pakhsh
                     */
                    Log.e("Sending request:" , "onSending");
                    Handler handler = new Handler(Looper.getMainLooper());
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                URL link = new URL(mCurrentChannel.url);
                                HttpURLConnection http = (HttpURLConnection)link.openConnection();
                                http.setReadTimeout(10000);
                                Log.w("Status" , " "+http.getResponseCode() );
                                if (http.getResponseCode() != 200){
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mPlaybackStatus = PlaybackStatus.IDLE;
                                            hideBufferingInfo();
                                            onLoading(false);
                                            onError();
                                        }
                                    });
                                    return;
                                }
                                StringBuilder l = new StringBuilder();
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(http.getInputStream())))) {
                                    for (int ch; (ch = reader.read()) != -1; ) {
                                        l.append((char) ch);
                                    }
                                }
                                String result = l.toString();
                                http.disconnect();
                                Log.e("Result"," " + result);
                                List<String> links = pullLinks(result);
                                Log.e("links" , links+"");
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (links.size() == 0){
                                            mPlaybackStatus = PlaybackStatus.IDLE;
                                            hideBufferingInfo();
                                        }else{
                                            RadioChannel radioChannel = new RadioChannel(mCurrentChannel.name , links.get(0));
                                            play( radioChannel);
                                        }
                                    }
                                });



                            }catch (Exception ex){
                                ex.printStackTrace();
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPlaybackStatus = PlaybackStatus.IDLE;
                                        hideBufferingInfo();
                                        onError();
                                    }
                                });
                            }
                        }
                    });
                    t.setPriority(Thread.MAX_PRIORITY);
                    t.start();
                    break;
            }
        }
    };

    /**
     * Gozaresh play shodan
     */
    private void onPlaying(){
        try {
            if (this.audioManagerListener != null) {
                this.audioManagerListener.onPlaying();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * Gozaresh buffering
     */
    private void onBuffering(){
        try {
            if (this.audioManagerListener != null) {
                this.audioManagerListener.onBuffering();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * Gozaresh stop
     */
    protected void onStoping(){
        try {
            if (this.audioManagerListener != null) {
                this.audioManagerListener.onStoping();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     *
     * @param loading true bashad dar hale loading ast va false bashad loading tamam shode ast
     */
    private void onLoading(boolean loading){
        try {
            if (audioManagerListener != null) {
                audioManagerListener.onLoading(loading);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }

    /**
     * Gozaresh error
     */
    private void onError(){
        try {
            if (this.audioManagerListener != null) {
                this.audioManagerListener.onError();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     *
     * @param context
     * @return
     * Daryafte sorate internet
     */
    private long getNetSpeed(Context context) {

        long nowTotalRxBytes = TrafficStats.getUidRxBytes(context.getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 : TrafficStats.getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long calculationTime = (nowTimeStamp - lastTimeStamp);
        if (calculationTime == 0) {
            return calculationTime;
        }

        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime);
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return speed;
    }

    /**
     *
     * @param context Etelaate buffer ha ra bar asase sorat net daryaft mikonad
     */
    private void getBufferingInfo(Context context) {
        String bufferingInfo = getNetSpeedText(getNetSpeed(context));
        //netSpeedBar.setText(bufferingInfo);
        //netSpeedBall.setText(bufferingInfo);
        Log.e("Net Speed" , ""+bufferingInfo);
    }

    /**
     *
     * @param speed Sorate net
     * @return Sorate internet ra bar hasbe string ersal mikonad
     */
    public String getNetSpeedText(long speed) {
        String text = "";
        if (speed >= 0 && speed < 1024) {
            text = speed + "B/s";
        } else if (speed >= 1024 && speed < (1024 * 1024)) {
            text = speed / 1024 + "K/s";
        } else if (speed >= (1024 * 1024) && speed < (1024 * 1024 * 1024)) {
            text = speed / (1024 * 1024) + "M/s";
        }
        return text;
    }

    public void play( RadioChannel station){
        if (mPlaybackStatus == PlaybackStatus.PLAYING && station.equals(mCurrentChannel)){
            return;
        }
        onLoading(true);
        if (mCurrentChannel != null && (mPlaybackStatus != PlaybackStatus.STOPPED)){
            stopPlaying();
            onLoading(true);
        }

        Log.e("type" , " "+mPlaybackStatus);
        mCurrentChannel = station;
        setLastBarActiveTimeStamp();

        switch (mPlaybackStatus) {
            case PlaybackStatus.LOADING:
            case PlaybackStatus.IDLE:
            case PlaybackStatus.PAUSED:
            case PlaybackStatus.STOPPED:
                if (null != mCurrentChannel && !mCurrentChannel.url.isEmpty()) {
                    insidePlay(mActivityWeakReference.get() , mCurrentChannel);
                }
                break;
            case PlaybackStatus.PLAYING:
                pausePlaying();
                break;
            default:
                Log.e("Error" , "Bad status: "+mPlaybackStatus);
                break;
        }
    }

    /**
     *
     * @param activity Objecti az activity
     * @param channel channali ke gharar ast baraye pakhsh amade shavad
     * Dar in method tanzimate avalie ghabl az pakhsh etefagh mioftad
     */
    public void insidePlay(Activity activity, RadioChannel channel) {
        mCurrentChannel = channel;

        isBuffering = true;

        addChannel(channel);
        setLastBarActiveTimeStamp();
        if (serviceIntent == null) {
            serviceIntent = new Intent(activity, PlayerService.class);
            Bundle serviceBundle = new Bundle();
            serviceBundle.putParcelable("Station", mCurrentChannel);
            serviceBundle.putSerializable("activity_class" , activity.getClass());
            serviceIntent.putExtra("Bundle", serviceBundle);
            activity.bindService(serviceIntent, serviceConnection, 0);
            Util.startForegroundService(activity, serviceIntent);
        } else if (playerService != null) {
            playerService.play(mCurrentChannel, activity.getClass());
        }
    }

    public void pausePlaying() {

        if (mPlaybackStatus == PlaybackStatus.PAUSED || mPlaybackStatus == PlaybackStatus.STOPPED){
            return;
        }

        if (playerService != null ) {
            mPlaybackStatus = PlaybackStatus.PAUSED;
            playerService.stopPlaying();
            onStoping();
        }
    }

    /**
     * Baraye stop
     */
    public void stopPlaying() {
        if (mPlaybackStatus == PlaybackStatus.STOPPED){
            return;
        }
        if (playerService != null) {
            mPlaybackStatus = PlaybackStatus.STOPPED;
            playerService.stopPlaying();
            onStoping();
        }
        mCurrentChannel = null;
    }


    public void addChannel(RadioChannel channel) {
        if (channel != null) {
            channel.lastPlayTime = new Date();
        }
    }

    /**
     * Akharin time ke active bude ast ra barmigardanad
     */
    public void setLastBarActiveTimeStamp() {
        this.lastBarActiveTimeStamp = System.currentTimeMillis();
    }

    /**
     *
     * @return Vaziate playback ra ersal mikonad
     */
    public int getPlaybackStatus(){
        return mPlaybackStatus;
    }

    /**
     * Etelaat buffer ha ra show mikonad
     */
    private void showBufferingInfo () {
        isBuffering = true;
    }

    /**
     * Information buffer ha ra hide mikonad
     */
    private  void hideBufferingInfo () {
        isBuffering = false;
    }

    /**
     * Bayad dar zamani ke kar ma ba application tamam shod in ra farakhani konim
     */
    public void releasePlayer() {
        stopCheckingThread();
        stopPlaying();
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        if (serviceIntent != null){
            mActivityWeakReference.get().stopService(serviceIntent);
            mActivityWeakReference.get().unbindService(serviceConnection);
        }
        serviceIntent = null;
        mActivityWeakReference = null;
        playerService = null;
        handler = null;
        audioManager = null;
        Looper.loop();
        Looper.myLooper().quit();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * Start Thread teste sorate internet
     */
    private void startCheckingThread() {
        if (!isCheckingThreadRunning && (checkingThread == null || !checkingThread.isAlive())) {
            isCheckingThreadRunning = true;
            checkingThread = new Thread(checkingRunnable);
            checkingThread.start();
        }
    }

    /**
     * Stop Thread test sorate net
     */
    private void stopCheckingThread() {
        isCheckingThreadRunning = false;
    }


    public static class PlaybackStatus {
        static final int IDLE = 0;
        static final int LOADING = 1;
        static final int PLAYING = 2;
        static final int PAUSED = 3;
        static final int STOPPED = 4;
    }
}
