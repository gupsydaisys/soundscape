package palsofpaulos.soundscape;

import palsofpaulos.soundscape.common.CommManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.ArrayList;

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.RecordingManager;

public class NotifyService extends Service {

    private static final String TAG = "Notify Service";

    private static ArrayList<Recording> notifyRecs = new ArrayList<>();
    private static Recording notifyRecording = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Notifications enabled!");

        Intent locationIntent = new Intent(this, LocationService.class);
        startService(locationIntent);

        if (RecordingManager.dbRecs.size() == 0) {
            RecordingManager.getDBRecordings(this);
        }
        notifyRecs = new ArrayList<>(RecordingManager.dbRecs);

        CommManager.setLocationChangedListener(new CommManager.LocationChangedListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (!CommManager.notificationActive) {
                    lookForNearRecording(location);
                }
            }
        });
        lookForNearRecording(CommManager.currentLocation);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");

        CommManager.setLocationChangedListener(new CommManager.LocationChangedListener() {
            @Override
            public void onLocationChanged(Location location) {
                // do nothing
            }
        });
    }




    private void lookForNearRecording(Location userLoc) {

        Log.d(TAG, "Checking for nearby recordings!");

        double minDist = CommManager.MAX_NOTIFY_DISTANCE;
        int minRec = -1;

        for (int ii = 0; ii < notifyRecs.size(); ii++) {
            Recording rec = notifyRecs.get(ii);

            double dist = rec.getLocation().distanceTo(userLoc);
            if (minDist > dist) {
                minDist = dist;
                minRec = ii;
            }
        }

        if (minDist < CommManager.MAX_NOTIFY_DISTANCE) {
            Log.d(TAG, "Found nearby recording, notifying");


            notifyRecording(notifyRecs.get(minRec));
            notifyRecs.remove(minRec);
        }
    }

    public void notifyRecording(Recording rec) {

        CommManager.notificationActive = true;
        notifyRecording = rec;

        // Build intent for recording playback
        Intent recIntent = new Intent(CommManager.AUDIO_INTENT);
        recIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        recIntent.putExtra(CommManager.REC_FILEPATH, CommManager.PLAY_PATH + rec.getFilePath());
        recIntent.putExtra(CommManager.REC_NAME, rec.getName());
        recIntent.putExtra(CommManager.REC_DATE, rec.getDateStorageString());
        recIntent.putExtra(CommManager.REC_LAT, rec.getLocation().getLatitude());
        recIntent.putExtra(CommManager.REC_LNG, rec.getLocation().getLongitude());

        //PendingIntent.getBroadcast()
        PendingIntent recPendingIntent = PendingIntent.getBroadcast(this, 0, recIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // action to start the intent
        NotificationCompat.Action listenAction = new NotificationCompat.Action(R.drawable.blank, "Listen", recPendingIntent);

        NotificationCompat.WearableExtender extender =
                new NotificationCompat.WearableExtender()
                        .addAction(listenAction)
                        .setContentAction(0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.soundscape_ic)
                        .setContentTitle(rec.getName())
                        .setContentText(rec.getDateString())
                        .setContentIntent(recPendingIntent)
                        .setVibrate(new long[]{500, 500})
                        .extend(extender);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        CommManager.notificationId++;
        notificationManager.cancel(CommManager.notificationId - 1);
        notificationManager.notify(CommManager.notificationId, notificationBuilder.build());
    }

}