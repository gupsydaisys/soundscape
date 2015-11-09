package palsofpaulos.soundscape;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MobileMessengerService extends WearableListenerService {

    private static final String TAG = "Mobile Listener";

    public static final String RECORD_ACTIVITY = "/record_activity";
    public static final String RECORD_CHANNEL = "/record_channel";

    public static final String AUDIO_INTENT = "/audio_intent";

    public String audioPath;

    private GoogleApiClient mApiClient;

    /* Recording Parameters */
    private static final int RECORDER_SAMPLERATE = 20000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;


    @Override
    public void onCreate() {
        super.onCreate();

        audioPath = this.getFilesDir().getPath() + "/8k16bitMono.pcm";

        initializeGoogleApiClient();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mApiClient.disconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, messageEvent.getPath());
    }

    @Override
    public void onChannelOpened(Channel channel) {
        if (channel.getPath().equals(RECORD_CHANNEL)) {
            if (!mApiClient.isConnected()) {
                Log.e(TAG, "Channel opened before api client connected!");
                return;
            }
            Channel.GetInputStreamResult getInputStreamResult = channel.getInputStream(mApiClient).await();
            InputStream inputStream = getInputStreamResult.getInputStream();
            writePCMToFile(inputStream);
        }
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        if (channel.getPath().equals(RECORD_CHANNEL)) {
            Log.d(TAG, "recording channel closed, attempting to play audio");
            try {
                playAudio(audioPath);
            }
            catch (IOException e) {
                Log.e(TAG, "something wrong with file path");
            }
        }
    }


    private void initializeGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)  // used for data layer API
                .build();
        mApiClient.connect();
    }

    private void playAudio(String filePath) throws IOException {
        Log.d("Audio Activity", "Attempting to play audio at path " + filePath);
        // We keep temporarily filePath globally as we have only two sample sounds now..
        if (filePath == null)
            return;

        // Reading the file..
        File file = new File(filePath);
        file.deleteOnExit();
        byte[] byteData = new byte[(int) file.length()];
        Log.d("MOBILE LISTENER", (int) file.length() + "");

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(byteData);
            in.close();
        } catch (FileNotFoundException e) {
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



    public void writePCMToFile(InputStream inputStream) {
        OutputStream outputStream = null;
        File audioFile = new File(audioPath);
        try {
            outputStream = new FileOutputStream(audioFile);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            Log.d(TAG, "Done writing PCM to file!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
