package palsofpaulos.soundscape;

import android.content.Context;
import android.content.Intent;
import android.media.AudioRecord;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import palsofpaulos.soundscape.common.RecordingManager;
import palsofpaulos.soundscape.common.CommManager;

public class RecordActivity extends WearableActivity {

    private static final String TAG = "Record Activity";
    private static final int SPEECH_REQUEST_CODE = 7;

    /* UI Elements */
    Button recButton;

    /* Wear Data API */
    private Thread mApiThread;
    private GoogleApiClient mApiClient;
    private Channel mApiChannel;
    private String mApiNodeId;
    private SpeechRecognizer speechRecognizer;
    private boolean useSpeechForName = true;

    /* Recording Parameters */
    private int bufferSize = AudioRecord.getMinBufferSize(RecordingManager.SAMPLERATE, RecordingManager.CHANNELS_IN, RecordingManager.ENCODING);

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        setAmbientEnabled();

        useSpeechForName = getIntent().getExtras().getBoolean(CommManager.SPEECH_FOR_NAME_EXTRA);

        // recording is started once the wear api client connects
        initGoogleApiClient();

        if (useSpeechForName) {
            initSpeechRecgonition();
        }
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();

        stopRecording();
        if (mApiClient.isConnected()) {
            mApiClient.disconnect();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

    }


   /* Activity Setup Methods
    * ------------------------------------------- */

    private void initializeButtons() {
        recButton = (Button) findViewById(R.id.record_button);
        recButton.setVisibility(View.VISIBLE);
        recButton.setBackground(ContextCompat.getDrawable(this, R.drawable.record_off));

        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecording();
            }
        });
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)  // used for data layer API
                .build();

        mApiThread = new Thread(new Runnable() {
            public void run() {
                mApiClient.blockingConnect(5, TimeUnit.SECONDS);

                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mApiClient).await();

                if (result.getNodes().size() == 0) {
                    Log.e(TAG, "No Wear API nodes found!");
                }
                mApiNodeId = result.getNodes().get(0).getId();

                RecordActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        initializeButtons();
                        toggleRecording();
                    }
                });

            }
        }, "Google API Thread");
        mApiThread.start();
    }



   /* Recording Methods
    * ------------------------------------------- */

    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
            recButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.record_on_3));
        } else {
            recButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.record_off_3));
            stopRecording();
            if (useSpeechForName && isNetworkAvailable()) {
                startSpeechRecognition();
            }
            else {
                finish();
            }
        }
    }

    private void startRecording() {

        // Initialize Audio Recorder.
        recorder = new AudioRecord(
                RecordingManager.SOURCE,
                RecordingManager.SAMPLERATE,
                RecordingManager.CHANNELS_IN,
                RecordingManager.ENCODING,
                bufferSize);
        recorder.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            public void run() {
                sendRecordingData();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void sendRecordingData() {
        mApiClient.blockingConnect();

        // Setup Wear API data channel
        ChannelApi.OpenChannelResult result = Wearable.ChannelApi.openChannel(mApiClient, mApiNodeId, CommManager.RECORD_CHANNEL).await();
        mApiChannel = result.getChannel();

        // Get the channel output stream
        Channel.GetOutputStreamResult outputStreamResult = mApiChannel.getOutputStream(mApiClient).await();
        OutputStream outputStream = outputStreamResult.getOutputStream();

        // Buffer to hold audio data
        byte[] bData = new byte[bufferSize];

        // Send recording data over channel while recording
        while (isRecording) {
            recorder.read(bData, 0, bufferSize);
            try {
                outputStream.write(bData, 0, bufferSize);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Close the channel output stream
        try {
            outputStream.close();
            Log.d("Record Activity", "Finished recording and closed stream");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Close the channel and disconnect from the API client
        if (mApiClient.isConnected()) {
            mApiChannel.close(mApiClient);
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }

    }

    private void initSpeechRecgonition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
    }

    private void startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available!");
            finish();
        } else {
            Log.d(TAG, "Speech recognition available!!");
        }

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

        startActivityForResult(recognizerIntent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                String spokenName = "";
                for (int ii = 0; ii < results.size(); ii++) {
                    spokenName += results.get(ii);
                }

                WearMessengerService.sendMessage(mApiClient, CommManager.SPEECH_RECOGNITION_RESULT, spokenName);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
