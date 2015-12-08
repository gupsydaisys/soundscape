package palsofpaulos.soundscape.common;

/* CommManager holds common string constants for
 * coomunicating through intents and across the wear API layer
 */

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class CommManager {

    public static final String RECORD_ACTIVITY = "/record_activity";

    // message paths
    public static final String SPEECH_RECOGNITION_RESULT = "/speech_recog_res";
    public static final String NOTIFICATION_REC_NAME = "/notify_name";
    public static final String NOTIFICATION_REC_DATE = "/notify_date";
    public static final String NOTIFICATION_REC_FILE = "/notify_filename";

    // channels
    public static final String RECORD_CHANNEL = "/record_channel";

    // broadcast intents
    public static final String AUDIO_INTENT = "/audio_intent";
    public static final String AUDIO_RESPONSE_INTENT = "/response_intent";

    // recording intent extras keys
    public static final String SPEECH_FOR_NAME_EXTRA = "sfn_intent";

    public static final String REC_FILEPATH = "filepath";
    public static final String REC_LAT = "lat";
    public static final String REC_LNG = "lng";
    public static final String REC_NAME = "name";
    public static final String REC_PLACE = "place";
    public static final String REC_DATE = "date";

    // recording intent extras values
    public static final String RESPONSE_PATH = "response_path";
    public static final String RENAME_PATH = "rename_path";
    public static final String PLAY_PATH = "play_path";

    public static final int LOCATION_UPDATE_INTERVAL = 300000;
    public static final int LOCATION_UPDATE_FASTEST = 60000;

    public static final double MAX_NOTIFY_DISTANCE = 500; // meters
    public static int notificationId = 0;
    public static boolean notificationActive = false;

    public static Location currentLocation = new Location("");
    public static LocationChangedListener locationChangedListener = new LocationChangedListener() {
        @Override
        public void onLocationChanged(Location location) {

        }
    };

    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void setLocationChangedListener(LocationChangedListener listener) {
        locationChangedListener = listener;
    }

    public interface LocationChangedListener {
        void onLocationChanged(Location location);
    }
}
