package palsofpaulos.soundscape;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.WearAPIManager;

public class AudioActivity extends AppCompatActivity {

    /* Recordings Data */
    private ArrayList<Recording> recs = new ArrayList<>();
    private ListView recsView;
    private ArrayAdapter<Recording> recsAdapter;
    private static final String SAVED_RECS = "ss_rec_list";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        // get recordings from saved preferences and populate listview
        getRecordings();
        recsView = (ListView) findViewById(R.id.listRecordings);
        recsAdapter = new RecordingAdapter(this, R.layout.listview_recordings, recs);
        recsView.setAdapter(recsAdapter);

        registerReceiver(audioReceiver, new IntentFilter(WearAPIManager.AUDIO_INTENT));
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        saveRecordings();
        unregisterReceiver(audioReceiver);
    }

    private BroadcastReceiver audioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String filePath = intent.getStringExtra("filePath");
            Recording newRec = new Recording(filePath);
            recs.add(0, newRec);
            updateRecsView();
        }
    };



    /* Methods to Handle Recording Data */

    private void getRecordings() {
        recs.clear();
        SharedPreferences prefs = getSharedPreferences(SAVED_RECS, MODE_PRIVATE);
        for (int ii = 0; ; ii++){
            String recPath = prefs.getString(String.valueOf(ii), "");
            if (!recPath.equals("")){
                Recording addRec = new Recording(recPath);
                recs.add(addRec);
            } else {
                Log.d("Recordings Loaded:", String.valueOf(ii));
                break; // Empty String means the default value was returned.
            }
        }
    }

    private void saveRecordings() {
        SharedPreferences prefs = getSharedPreferences(SAVED_RECS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (int ii = 0; ii < recs.size(); ii++) {
            editor.putString(String.valueOf(ii), recs.get(ii).getFilePath());
        }
        editor.commit();
    }

    private void clearRecordings() {
        SharedPreferences prefs = getSharedPreferences(SAVED_RECS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    private void updateRecsView() {
        recsView.destroyDrawingCache();
        RecordingAdapter ra = (RecordingAdapter) recsView.getAdapter();
        ra.notifyDataSetChanged();
        recsView.invalidate();
    }
}
