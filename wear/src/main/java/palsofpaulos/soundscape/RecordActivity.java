package palsofpaulos.soundscape;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import palsofpaulos.soundscape.common.RecordingManager;
import palsofpaulos.soundscape.common.WearAPIManager;

public class RecordActivity extends Activity  {

    private static final String TAG = "Record Activity";

    /* UI Elements */
    Button recButton;

    /* Wear Data API */
    private Thread mApiThread;
    private GoogleApiClient mApiClient;
    private Channel mApiChannel;
    private String mApiNodeId;
    private String nodeId;

    /* Recording Parameters */
    private int bufferSize = AudioRecord.getMinBufferSize(RecordingManager.SAMPLERATE, RecordingManager.CHANNELS_IN, RecordingManager.ENCODING);

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private String audioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        audioPath = this.getFilesDir().getPath() + "/8k16bitMono.pcm";

        initializeGoogleApiClient();
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();

        if (mApiClient.isConnected()) {
            mApiClient.disconnect();
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

    private void hideConnectingText() {
        TextView connectingText = (TextView) findViewById(R.id.wear_connecting_text);
        connectingText.setVisibility(View.GONE);
    }

    private void initializeGoogleApiClient() {
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
                        hideConnectingText();
                        initializeButtons();
                        toggleRecording();
                    }
                });

            }
        }, "Google API Thread");
        mApiThread.start();

        mApiClient.disconnect();
    }



   /* Recording Methods
    * ------------------------------------------- */

    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
            recButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.record_on2));
        } else {
            stopRecording();
            recButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.record_off2));
            finish();
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
        ChannelApi.OpenChannelResult result = Wearable.ChannelApi.openChannel(mApiClient, mApiNodeId, WearAPIManager.RECORD_CHANNEL).await();
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
            mApiClient.disconnect();
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



}
