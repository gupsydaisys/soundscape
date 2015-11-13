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
import android.view.MotionEvent;
import android.view.View;
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

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.WearAPIManager;

public class MobileMessengerService extends WearableListenerService {

    private static final String TAG = "Mobile Listener";

    public Recording recording;

    private GoogleApiClient mApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service created!");

        initializeGoogleApiClient();
        mApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Service destroyed!");

        if (mApiClient.isConnected()) {
            mApiClient.disconnect();
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, messageEvent.getPath());
    }

    @Override
    public void onChannelOpened(final Channel channel) {
        if (channel.getPath().equals(WearAPIManager.RECORD_CHANNEL)) {
            getRecordingFromChannel(channel);
        }
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        if (channel.getPath().equals(WearAPIManager.RECORD_CHANNEL)) {
            if (recording == null) {
                Log.e(TAG, "Recording finished but the recording object was not created");
            }
            else {
                Log.d(TAG, "recording channel closed, attempting to play audio");
                startAudioActivity();
            }
        }
    }


    private void initializeGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)  // used for data layer API
                .build();
    }

    private void startAudioActivity() {
        Intent audioIntent = new Intent(WearAPIManager.AUDIO_INTENT);
        audioIntent.putExtra("filePath", recording.getFilePath());
        sendBroadcast(audioIntent);
    }

    private void getRecordingFromChannel(final Channel channel) {
        final String rootPath = this.getFilesDir().getPath();
        Thread recordThread = new Thread(new Runnable() {
            public void run() {
                if (!mApiClient.isConnected()) {
                    Log.e(TAG, "Channel opened before api client connected!");
                    mApiClient.blockingConnect();
                }
                Channel.GetInputStreamResult getInputStreamResult = channel.getInputStream(mApiClient).await();
                InputStream inputStream = getInputStreamResult.getInputStream();
                recording = new Recording(inputStream, rootPath);
            }
        }, "RecordFile Thread");
        recordThread.start();
    }
}
