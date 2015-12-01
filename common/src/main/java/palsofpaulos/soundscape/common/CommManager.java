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

    public static final String SPEECH_RECOGNITION_RESULT = "/speech_recog_res";
    public static final String RECORD_CHANNEL = "/record_channel";

    // broadcast intents
    public static final String AUDIO_INTENT = "/audio_intent";
    public static final String AUDIO_RESPONSE_INTENT = "/response_intent";
    public static final String NAME_INTENT = "/name_intent";

    // recording intent extras keys
    public static final String SPEECH_FOR_NAME_EXTRA = "sfn_intent";

    public static final String REC_FILEPATH = "filepath";
    public static final String REC_LAT = "lat";
    public static final String REC_LNG = "lng";
    public static final String REC_NAME = "name";
    public static final String REC_PLACE = "place";
    public static final String REC_DATE = "date";

    // recording intent extras values
    public static final String NULL_REC_PATH = "null_path";
    public static final String RENAME_PATH = "rename_path";

    public static final int LOCATION_UPDATE_INTERVAL = 300000;
    public static final int LOCATION_UPDATE_FASTEST = 60000;

    public static Location currentLocation = new Location("");

    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
