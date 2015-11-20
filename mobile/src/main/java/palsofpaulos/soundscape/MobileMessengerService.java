package palsofpaulos.soundscape;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

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

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.WearAPIManager;

public class MobileMessengerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final String TAG = "Mobile Listener";

    private GoogleApiClient mApiClient;
    private String placeName = "";

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
                        if (status.getStatus().isSuccess()) {
                            Log.d(TAG, "Successfully requested location");
                        } else {
                            Log.e(TAG, status.getStatusMessage());
                        }
                    }
                });
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location updated!" + location.toString());
        WearAPIManager.currentLocation = location;
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connResult) {}


    private void initializeGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(Wearable.API)  // used for data layer API
                .addConnectionCallbacks(this)
                .build();
    }

    private void sendRecordingToAudioActivity(Recording recording) {

        Intent audioIntent = new Intent(WearAPIManager.AUDIO_INTENT);
        audioIntent.putExtra(WearAPIManager.REC_FILEPATH, recording.getFilePath());
        audioIntent.putExtra(WearAPIManager.REC_LAT, WearAPIManager.currentLocation.getLatitude());
        audioIntent.putExtra(WearAPIManager.REC_LNG, WearAPIManager.currentLocation.getLongitude());
        audioIntent.putExtra(WearAPIManager.REC_PLACE, recording.getName());
        sendBroadcast(audioIntent);
    }

    private void getRecordingFromChannel(final Channel channel) {
        final String rootPath = this.getFilesDir().getPath();

        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(mApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                String mostLikelyPlace = "";
                double mostLikelyValue = 0;
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    if (placeLikelihood.getLikelihood() > mostLikelyValue) {
                        mostLikelyPlace = (String) placeLikelihood.getPlace().getName();
                        mostLikelyValue = placeLikelihood.getLikelihood();
                    }
                }
                likelyPlaces.release();
                placeName = mostLikelyPlace;
            }
        });

        Thread recordThread = new Thread(new Runnable() {
            public void run() {
                if (!mApiClient.isConnected()) {
                    Log.e(TAG, "Channel opened before api client connected!");
                    mApiClient.blockingConnect();
                }
                Channel.GetInputStreamResult getInputStreamResult = channel.getInputStream(mApiClient).await();
                InputStream inputStream = getInputStreamResult.getInputStream();
                Recording recording = new Recording(inputStream, rootPath);
                recording.setName(placeName);
                sendRecordingToAudioActivity(recording);
            }
        }, "RecordFile Thread");
        recordThread.start();
    }
}
