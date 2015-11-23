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
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.WearAPIManager;

public class MobileMessengerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final String TAG = "Mobile Listener";

    private GoogleApiClient mApiClient;
    private String placeName = "";

    private static Intent checkResponseIntent; // send to check if audio activity is ready to receive recordings
    private static Queue<Intent> pendingRecordings = new LinkedList<>(); // holds recordings and sends them when audio activity responds

    private Recording lastRecording;
    private String lastRecordingName;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service created!");

        checkResponseIntent = new Intent(WearAPIManager.AUDIO_INTENT);
        checkResponseIntent.putExtra(WearAPIManager.REC_FILEPATH, WearAPIManager.NULL_REC_PATH);

        registerReceiver(responseReceiver, new IntentFilter(WearAPIManager.AUDIO_RESPONSE_INTENT));
        sendPendingRecordings();

        initializeGoogleApiClient();
        mApiClient.connect();
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
        if (messageEvent.getPath().equals(WearAPIManager.SPEECH_RECOGNITION_RESULT)) {
            if (lastRecording == null) {
                return;
            }
            lastRecordingName = new String(messageEvent.getData());
            sendRecordingToAudioActivity(lastRecording);
        }
    }

    @Override
    public void onChannelOpened(final Channel channel) {
        if (channel.getPath().equals(WearAPIManager.RECORD_CHANNEL)) {
            getRecordingFromChannel(channel);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(WearAPIManager.LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(WearAPIManager.LOCATION_UPDATE_FASTEST);

        LocationServices.FusedLocationApi
                .requestLocationUpdates(mApiClient, locationRequest, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (!status.getStatus().isSuccess()) {
                            Log.d(TAG, "Location request failed!");
                        }
                    }
                });
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, location.toString());
        WearAPIManager.currentLocation = location;
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connResult) {}


    private void initializeGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)       // location API
                .addApi(Places.GEO_DATA_API)        // places API
                .addApi(Places.PLACE_DETECTION_API) // places API
                .addApi(Wearable.API)               // data layer API
                .addConnectionCallbacks(this)
                .build();
    }

    /* Recording Handling Methods */

    private void getRecordingFromChannel(final Channel channel) {
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
                lastRecording = new Recording(inputStream, rootPath, null, recDate);
            }
        }, "RecordFile Thread");
        recordThread.start();

        //getCurrentPlaceName();
    }

    private void sendRecordingToAudioActivity(Recording recording) {
        if (recording == null) {
            Log.e(TAG, "Attempted to send null recording!");
            return;
        }
        if (lastRecordingName != null && !lastRecordingName.equals("")) {
            recording.setName(capitalizeWords(lastRecordingName));
        }
        else if (placeName != null && !placeName.equals("")) {
            recording.setName(placeName);
        }
        else {
            recording.setName(recording.getDateString());
        }

        Intent audioIntent = new Intent(WearAPIManager.AUDIO_INTENT);
        audioIntent.putExtra(WearAPIManager.REC_FILEPATH, recording.getFilePath());
        audioIntent.putExtra(WearAPIManager.REC_LAT, WearAPIManager.currentLocation.getLatitude());
        audioIntent.putExtra(WearAPIManager.REC_LNG, WearAPIManager.currentLocation.getLongitude());
        audioIntent.putExtra(WearAPIManager.REC_NAME, recording.getName());
        //audioIntent.putExtra(WearAPIManager.REC_PLACE, lastRecordingName);
        audioIntent.putExtra(WearAPIManager.REC_DATE, recording.getDateStorageString());
        pendingRecordings.add(audioIntent);
        sendPendingRecordings();

        lastRecordingName = "";
        placeName = "";
    }

    // response receiver gets indications that recordings are ready to be received
    private BroadcastReceiver responseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Response from audio activity received, sending recording");
            if (pendingRecordings.size() > 0) {
                sendBroadcast(pendingRecordings.remove());
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
}
