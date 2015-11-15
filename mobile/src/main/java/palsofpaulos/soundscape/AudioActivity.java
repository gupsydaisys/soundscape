package palsofpaulos.soundscape;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import java.io.InputStream;
import java.util.ArrayList;

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.WearAPIManager;
import palsofpaulos.soundscape.common.Recording.PostPlayListener;
import palsofpaulos.soundscape.common.Recording.PlayAudioTask;

public class AudioActivity extends AppCompatActivity {

    /* Wear Data API */
    private GoogleApiClient mApiClient;
    private Channel mApiChannel;
    private InputStream inputStream;

    /* Recordings Data */
    private ArrayList<Recording> recs = new ArrayList<>();
    private ListView recsView;
    private ArrayAdapter<Recording> recsAdapter;
    private static final String SAVED_RECS = "ss_rec_list";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        // Get recordings from saved preferences and
        // populate the listview with them
        getRecordings();
        recsView = (ListView) findViewById(R.id.listRecordings);
        recsAdapter = new RecordingAdapter(this, R.layout.listview_recordings, recs);
        recsView.setAdapter(recsAdapter);

//        recsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
//                ImageView playButton = (ImageView) findViewById(R.id.play_pause);
//                playButton.setImageResource(R.drawable.pause);
//
//                Recording rec = recs.get(position);
//                if (view.getId() == R.id.delete_button) {
//                    if (rec.isPlaying()) {
//                        rec.stop();
//                    }
//                    rec.getFile().delete();
//                    recs.remove(position);
//                    updateRecsView();
//                }
//            }
//        });

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
