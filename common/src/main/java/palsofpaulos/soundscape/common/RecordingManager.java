package palsofpaulos.soundscape.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RecordingManager {
    public static final String TAG = "Recording Manager";

    public static final int SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int SAMPLERATE = 20000;
    public static final int CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int MAX_LENGTH = 20 * (60 * SAMPLERATE); // max 20 minutes

    public static final Location DEFAULT_LOCATION = new Location("");
    public static final SimpleDateFormat STORE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
    public static final SimpleDateFormat PRINT_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.US);

    public static final String SAVED_RECS = "ss_rec_list";
    public static final String DB_RECS = "db_rec_list";

    public static int lastId = 0;

    public static ArrayList<Recording> dbRecs = new ArrayList<>();

    // returns date if string is properly formatted, otherwise the current date
    public static Date recDateFromString(String dateString) {
        Date recDate;
        if (dateString.equals("")) {
            Log.e(TAG, "Tried to initialize date from empty string");
        }
        try {
            recDate = RecordingManager.STORE_DATE_FORMAT.parse(dateString);
        }
        catch (ParseException e) {
            Log.e(TAG, "Tried to initialize date from bad string");
            recDate = Calendar.getInstance().getTime();
        }

        return recDate;
    }

    public static void getLastRecId(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(RecordingManager.SAVED_RECS, ctx.MODE_PRIVATE);
        int maxId = 0;
        for (int ii = 0; ; ii++){
            String recPath = prefs.getString(ii + "file", "");

            if (!recPath.equals("")){
                int recId = prefs.getInt(ii + "id", -1);
                if (maxId < recId) {
                    maxId = recId;
                }
            } else {
                break; // Empty String means the default value was returned.
            }
        }
        lastId = maxId + 1;
    }
}
