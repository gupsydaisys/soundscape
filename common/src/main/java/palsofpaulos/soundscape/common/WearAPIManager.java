package palsofpaulos.soundscape.common;

/* WearAPIManager holds common string constants for using the
 * Google Wear API Layer.
 */

import android.location.Location;

public class WearAPIManager {

    public static final String RECORD_ACTIVITY = "/record_activity";

    public static final String RECORD_CHANNEL = "/record_channel";

    public static final String AUDIO_INTENT = "/audio_intent";
    public static final String REC_FILEPATH = "filepath";
    public static final String REC_LAT = "lat";
    public static final String REC_LNG = "lng";
    public static final String REC_PLACE = "place";

    public static final int LOCATION_UPDATE_INTERVAL = 300000;
    public static final int LOCATION_UPDATE_FASTEST = 60000;

    public static Location currentLocation = new Location("");

    private WearAPIManager() {

    }
}
