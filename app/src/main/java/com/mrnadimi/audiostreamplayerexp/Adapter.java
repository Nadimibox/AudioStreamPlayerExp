package com.mrnadimi.audiostreamplayerexp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mrnadimi.audiostreamplayer.AudioManager;
import com.mrnadimi.audiostreamplayer.AudioManagerListener;
import com.mrnadimi.audiostreamplayer.RadioChannel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Developer: Mohamad Nadimi
 * Company: Saghe
 * Website: https://www.mrnadimi.com
 * Created on 05 July 2021
 * <p>
 * Description: ...
 */
public class Adapter extends RecyclerView.Adapter<Adapter.ItemViewHolder> {

    private final List<RadioChannel> items;

    private ItemViewHolder lastItem;
    AudioManager audioManager;

    public Adapter(Activity activity , List<RadioChannel> items) {
        this.items = items;
        ProgressDialog p = new ProgressDialog(activity);
        p.setCancelable(false);
        p.setCanceledOnTouchOutside(false);
        audioManager = AudioManager.getInstance(activity, new AudioManagerListener() {
            @Override
            public void onLoading(boolean progress) {
                Log.e("onLoading" , "onLoading: "+progress);
                if (progress){
                    p.show();
                }else{
                    p.dismiss();
                }
            }

            @Override
            public void onPlaying() {
               // Toast.makeText(activity, "onPlaying" , Toast.LENGTH_SHORT).show();
                Log.e("onPlaying" , "playing -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --- --");
                if (lastItem != null){
                    lastItem.playOrPause.setImageResource(R.drawable.ic_baseline_pause_24);
                    lastItem.isPlaing = true;
                }
            }

            @Override
            public void onStoping() {
               // Toast.makeText(activity, "Stoping" , Toast.LENGTH_SHORT).show();
                Log.e("onStoping" , "onStoping -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --- --");
                if (lastItem != null){
                    lastItem.playOrPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                    lastItem.isPlaing = false;
                }
            }

            @Override
            public void onBuffering() {
                //Toast.makeText(activity, "onBuffering" , Toast.LENGTH_SHORT).show();
                Log.e("onBuffering" , "onBuffering -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --- --");
            }

            @Override
            public void onError() {
                p.dismiss();
                Log.e("onError" , "onError -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --- --");
                if (lastItem != null){
                    lastItem.playOrPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                    lastItem.isPlaing = false;
                }
            }
        });
    }

    @NonNull
    @NotNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item , parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull Adapter.ItemViewHolder holder, int position) {
        RadioChannel channel = items.get(position);
        holder.textView.setText(channel.name);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.findViewById(R.id.play_pause).performClick();
            }
        });
        holder.playOrPause.setImageResource( holder.isPlaing ? R.drawable.ic_baseline_pause_24 : R.drawable.ic_baseline_play_arrow_24  );
        holder.playOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!holder.isPlaing) {
                    if (lastItem != null){
                        lastItem.playOrPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                        lastItem.isPlaing = false;
                    }
                    lastItem = holder;
                    audioManager.play( channel);
                    ((ImageView)v).setImageResource(R.drawable.ic_baseline_pause_24);
                    holder.isPlaing = true;
                }else{
                    audioManager.pausePlaying();
                    ((ImageView)v).setImageResource(R.drawable.ic_baseline_play_arrow_24);
                    holder.isPlaing = false;
                }
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull @NotNull Adapter.ItemViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder{
        
        private final TextView textView;
        private final ImageView playOrPause;
        private boolean isPlaing = false;

        public ItemViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.name);
            playOrPause = itemView.findViewById(R.id.play_pause);
        }
    }
}
