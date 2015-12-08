package palsofpaulos.soundscape;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import palsofpaulos.soundscape.common.CommManager;
import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.RecordingManager;

public class MobileMessengerService extends WearableListenerService {

    private static final String TAG = "Mobile Listener";

    private GoogleApiClient mApiClient;
    private String placeName = "";

    private static Intent checkResponseIntent; // send to check if audio activity is ready to receive recordings
    private static Queue<Intent> pendingRecordings = new LinkedList<>(); // holds recordings and sends them when audio activity responds

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service created!");

        Intent locationIntent = new Intent(this, LocationService.class);
        startService(locationIntent);

        checkResponseIntent = new Intent(CommManager.AUDIO_INTENT);
        checkResponseIntent.putExtra(CommManager.REC_FILEPATH, CommManager.RESPONSE_PATH);

        registerReceiver(responseReceiver, new IntentFilter(CommManager.AUDIO_RESPONSE_INTENT));
        sendPendingRecordings();

        initializeGoogleApiClient();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Service destroyed!");

        if (pendingRecordings.size() > 0) {
            Log.e(TAG, "Some recordings not sent!");
        }

        if (mApiClient.isConnected()) {
            mApiClient.disconnect();
        }

        unregisterReceiver(responseReceiver);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, messageEvent.getPath());
        if (messageEvent.getPath().equals(CommManager.SPEECH_RECOGNITION_RESULT)) {
            Intent nameIntent = new Intent(CommManager.AUDIO_INTENT);
            nameIntent.putExtra(CommManager.REC_FILEPATH, CommManager.RENAME_PATH);
            nameIntent.putExtra(CommManager.REC_NAME, capitalizeWords(new String(messageEvent.getData())));
            pendingRecordings.add(nameIntent);
            sendPendingRecordings();
        }
    }

    @Override
    public void onChannelOpened(final Channel channel) {
        if (channel.getPath().equals(CommManager.RECORD_CHANNEL)) {
            getRecordingFromChannel(channel);
        }
    }




    private void initializeGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)        // places API
                .addApi(Places.PLACE_DETECTION_API) // places API
                .addApi(Wearable.API)               // data layer API
                .build();
        mApiClient.connect();
    }

    /* Recording Handling Methods */

    private void getRecordingFromChannel(final Channel channel) {
        Log.d(TAG, "Getting new recording!");

        if (RecordingManager.lastId == 0) {
            RecordingManager.getLastRecId(this);
        }

        final String rootPath = this.getFilesDir().getPath();
        final Date recDate = Calendar.getInstance().getTime();

        Thread recordThread = new Thread(new Runnable() {
            public void run() {
                if (!mApiClient.isConnected()) {
                    Log.e(TAG, "Channel opened before api client connected!");
                    mApiClient.blockingConnect();
                }
                Channel.GetInputStreamResult getInputStreamResult = channel.getInputStream(mApiClient).await();
                InputStream inputStream = getInputStreamResult.getInputStream();
                Recording newRec = new Recording(inputStream, rootPath, CommManager.currentLocation, recDate);
                Log.d(TAG, "Recording " + newRec.getFilePath() + " created!");
                addRecordingToQueue(newRec);
                sendPendingRecordings();
            }
        }, "RecordFile Thread");
        recordThread.start();

        //getCurrentPlaceName();
    }

    private void addRecordingToQueue(Recording recording) {
        if (recording == null) {
            Log.e(TAG, "Attempted to add null recording!");
            return;
        }
        if (placeName != null && !placeName.equals("")) {
            recording.setName(placeName);
        }
        else {
            recording.setName(recording.getDateString());
        }

        Log.d(TAG, "Adding recording " + recording.getFilePath() + " to queue");

        Intent audioIntent = new Intent(CommManager.AUDIO_INTENT);
        audioIntent.putExtra(CommManager.REC_FILEPATH, recording.getFilePath());
        audioIntent.putExtra(CommManager.REC_LAT, recording.getLocation().getLatitude());
        audioIntent.putExtra(CommManager.REC_LNG, recording.getLocation().getLongitude());
        audioIntent.putExtra(CommManager.REC_NAME, recording.getName());
        //audioIntent.putExtra(CommManager.REC_PLACE, lastRecordingName);
        audioIntent.putExtra(CommManager.REC_DATE, recording.getDateStorageString());

        pendingRecordings.add(audioIntent);

        placeName = "";
    }

    // response receiver gets indications that recordings are ready to be received
    private BroadcastReceiver responseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (pendingRecordings.size() > 0) {
                Intent sendIntent = pendingRecordings.remove();
                Log.d(TAG, "Sending recording " + sendIntent.getExtras().getString(CommManager.REC_FILEPATH));
                sendBroadcast(sendIntent);
                sendBroadcast(checkResponseIntent);
            }
        }
    };

    private void sendPendingRecordings() {
        if (pendingRecordings.size() > 0) {
            sendBroadcast(checkResponseIntent);
        }
    }


    private void getCurrentPlaceName() {
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(mApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                String mostLikelyPlace = "";
                double mostLikelyValue = 0;
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    //Log.d(TAG, "Place: " + placeLikelihood.getPlace().getName() + " | Likelihood: " + placeLikelihood.getLikelihood());
                    if (placeLikelihood.getLikelihood() > mostLikelyValue) {
                        mostLikelyPlace = (String) placeLikelihood.getPlace().getName();
                        mostLikelyValue = placeLikelihood.getLikelihood();
                    }
                }
                likelyPlaces.release();
                placeName = mostLikelyPlace;
            }
        });
    }

    // from stack overflow, capitalizes first letter of each word
    public static String capitalizeWords(String str) {
        if (str.equals("")) {
            return str;
        }
        String[] strArr = str.split(" ");
        StringBuffer strBuff = new StringBuffer();

        for (int ii = 0; ii < strArr.length; ii++) {
            strBuff.append(Character.toUpperCase(strArr[ii].charAt(0)))
                    .append(strArr[ii].substring(1)).append(" ");
        }
        return strBuff.toString().trim();
    }

    public static void sendMessage(final GoogleApiClient mApiClient, final String path, final String text) {
        if (!mApiClient.isConnected()) {
            mApiClient.connect();
        }
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }
}
