package palsofpaulos.soundscape;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telecom.ConnectionRequest;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

import palsofpaulos.soundscape.common.CommManager;
import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.RecordingManager;

public class NotifyService extends Service {

    private static final String TAG = "Notify Service";

    private static boolean notificationActive = false;
    private static Recording notifyRecording = null;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        Log.d(TAG, "Notifications enabled!");

        getDBRecordings();
        CommManager.setLocationChangedListener(new CommManager.LocationChangedListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (!notificationActive) {
                    lookForNearRecording(location);
                }
            }
        });
        lookForNearRecording(CommManager.currentLocation);
    }

    @Override
    public void onDestroy() {
        CommManager.setLocationChangedListener(new CommManager.LocationChangedListener() {
            @Override
            public void onLocationChanged(Location location) {
                // do nothing
            }
        });
    }

    private void getDBRecordings() {
        RecordingManager.dbRecs.clear();
        SharedPreferences prefs = getSharedPreferences(RecordingManager.DB_RECS, MODE_PRIVATE);
        for (int ii = 0; ; ii++){
            String recPath = prefs.getString(ii + "file", "");

            // recPath is an empty string when we've run out of recordings to get
            if (!recPath.equals("")){
                Location recLoc = new Location("");
                Date recDate = RecordingManager.recDateFromString(prefs.getString(ii + "date", ""));
                recLoc.setLatitude(Double.longBitsToDouble(prefs.getLong(ii + "lat", 0)));
                recLoc.setLongitude(Double.longBitsToDouble(prefs.getLong(ii + "lng", 0)));

                Recording addRec = new Recording(recPath, recLoc, recDate);
                addRec.setName(prefs.getString(ii + "place", ""));
                RecordingManager.dbRecs.add(addRec);
            } else {
                Log.d("Recordings Loaded:", String.valueOf(ii));
                break; // Empty String means the default value was returned.
            }
        }
    }


    private void lookForNearRecording(Location userLoc) {

        Log.d(TAG, "Checking for nearby recordings!");

        double minDist = CommManager.MAX_NOTIFY_DISTANCE;
        Recording minRec = null;

        for (Recording rec : RecordingManager.dbRecs) {
            double dist = rec.getLocation().distanceTo(userLoc);
            if (minDist > dist) {
                minDist = dist;
                minRec = rec;
            }
        }

        if (minDist <= CommManager.MAX_NOTIFY_DISTANCE && minRec != null) {
            Log.d(TAG, "Found nearby recording, notifying");

            watchNotification(minRec);
        }
    }



    public void watchNotification(Recording recording) {

        //notificationActive = true;
        notifyRecording = recording;

        // Build intent for recording playback
        Intent recIntent = new Intent(CommManager.AUDIO_INTENT);
        recIntent.putExtra(CommManager.REC_FILEPATH, CommManager.PLAY_PATH + recording.getFilePath());
        recIntent.putExtra(CommManager.REC_NAME, recording.getName());
        recIntent.putExtra(CommManager.REC_DATE, recording.getDateStorageString());
        recIntent.putExtra(CommManager.REC_LAT, recording.getLocation().getLatitude());
        recIntent.putExtra(CommManager.REC_LNG, recording.getLocation().getLongitude());

        //PendingIntent.getBroadcast()
        PendingIntent recPendingIntent = PendingIntent.getBroadcast(this, 0, recIntent, 0);

        // action to start the intent
        NotificationCompat.Action listenAction = new NotificationCompat.Action(R.drawable.blank, "Listen", recPendingIntent);

        NotificationCompat.WearableExtender extender =
                new NotificationCompat.WearableExtender()
                        .addAction(listenAction)
                        .setContentAction(0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.soundscape_ic)
                        .setContentTitle(recording.getName())
                        .setContentText(recording.getDateString())
                        .setVibrate(new long[]{1000, 1000})
                        .setContentIntent(recPendingIntent)
                        .setAutoCancel(true)
                        .extend(extender);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        CommManager.notificationId++;
        notificationManager.cancel(CommManager.notificationId - 1);
        notificationManager.notify(CommManager.notificationId, notificationBuilder.build());
    }
}
