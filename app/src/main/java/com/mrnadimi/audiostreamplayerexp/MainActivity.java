package com.mrnadimi.audiostreamplayerexp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mrnadimi.audiostreamplayer.AudioManager;
import com.mrnadimi.audiostreamplayer.RadioChannel;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class MainActivity extends FragmentActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //List sakhte mishavad
        RecyclerView recyclerView = findViewById(R.id.recycle);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(llm);
        //Adapter belist ersal mishavad
        Adapter adapter = new Adapter(this , getShoutcasts(this));
        recyclerView.setAdapter(adapter);
    }

    /*
     * Daryaft list az folder raw ke be surate json ast
     */
    public static List<RadioChannel> getShoutcasts(Context context){

        Reader reader = new InputStreamReader(context.getResources().openRawResource(R.raw.shoutcasts));

        return (new Gson()).fromJson(reader, new TypeToken<List<RadioChannel>>() {}.getType());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //AudioManager.getInstance(this , null).releasePlayer(this);
        AudioManager.getInstance(this , null).unbind(this);
    }
}