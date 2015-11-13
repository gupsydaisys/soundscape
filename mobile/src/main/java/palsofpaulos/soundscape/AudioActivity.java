package palsofpaulos.soundscape;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.RecordingManager;
import palsofpaulos.soundscape.common.WearAPIManager;
import palsofpaulos.soundscape.common.Recording.PostPlayListener;
import palsofpaulos.soundscape.common.Recording.PlayAudioTask;

public class AudioActivity extends AppCompatActivity {

    /* Wear Data API */
    private GoogleApiClient mApiClient;
    private Channel mApiChannel;
    private InputStream inputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        registerReceiver(audioReceiver, new IntentFilter(WearAPIManager.AUDIO_INTENT));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(audioReceiver);
    }


    private BroadcastReceiver audioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final TextView audioStatusText = (TextView) findViewById(R.id.audio_status_text);
            audioStatusText.setText("Playing audio!");

            final String filePath = intent.getStringExtra("filePath");
            Recording recording = new Recording(filePath);
            final PlayAudioTask audioTask = recording.getAudioTask(new PostPlayListener() {
                @Override
                public void onFinished() {
                    audioStatusText.setText("Waiting for audio...");
                }
            });
            audioTask.execute();

            View view = findViewById(R.id.audio_view);
            view.setOnClickListener(new View.OnClickListener() {
                private boolean isPaused = false;

                @Override
                public void onClick(View v) {
                    if (isPaused) {
                        isPaused = false;
                        audioTask.play();
                    }
                    else {
                        isPaused = true;
                        audioTask.pause();
                    }
                }
            });
        }
    };
}
