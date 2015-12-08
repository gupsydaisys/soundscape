package palsofpaulos.soundscape;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.wearable.Wearable;

import palsofpaulos.soundscape.common.CommManager;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final String TAG = "Location Service";

    private GoogleApiClient mApiClient;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "service started");

        initializeGoogleApiClient();
    }

    @Override
    public void onDestroy() {
        if (mApiClient.isConnected()) {
            mApiClient.disconnect();
        }
    }

    private void initializeGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)       // location API
                .addConnectionCallbacks(this)
                .build();
        mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(CommManager.LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(CommManager.LOCATION_UPDATE_FASTEST);

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
        CommManager.currentLocation = location;
        CommManager.locationChangedListener.onLocationChanged(location);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connResult) {}
}
