package palsofpaulos.soundscape;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
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

public class AudioActivity extends AppCompatActivity {

    /* Wear Data API */
    private GoogleApiClient mApiClient;
    private Channel mApiChannel;
    private InputStream inputStream;

    /* Recording Parameters */
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        registerReceiver(audioReceiver, new IntentFilter(MobileMessengerService.RECORD_ACTIVITY));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(audioReceiver);
    }





    private void playAudio(String path) {
        Log.d("Audio Activity", "Attempting to play audio at path " + path);
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e("Audio Activity", "Path error! " + path);
            e.printStackTrace();
        }
    }

    private void PlayShortAudioFileViaAudioTrack(String filePath) throws IOException {
        Log.d("Audio Activity", "Attempting to play audio at path " + filePath);
        // We keep temporarily filePath globally as we have only two sample sounds now..
        if (filePath == null)
            return;

        //Reading the file..
        File file = new File(filePath); // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"
        byte[] byteData = new byte[(int) file.length()];
        Log.d("MOBILE LISTENER", (int) file.length() + "");

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(byteData);
            in.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Set and push to audio track..
        int intSize = android.media.AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, intSize, AudioTrack.MODE_STREAM);
        if (at != null) {
            at.play();
            // Write the byte array to the track
            at.write(byteData, 0, byteData.length);
            at.stop();
            at.release();
        }

    }




    private BroadcastReceiver audioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView audioStatusText = (TextView) findViewById(R.id.audio_status_text);
            audioStatusText.setText("Playing audio!");

            String path = intent.getStringExtra("path");
            try {
                PlayShortAudioFileViaAudioTrack(path);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
