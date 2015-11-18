package palsofpaulos.soundscape;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioTimestamp;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;


import java.util.ArrayList;

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.WearAPIManager;

public class AudioActivity extends AppCompatActivity {

    private static final String TAG = "Audio Activity";
    private static final int MUSIC_BAR_ANIMATION_DURATION = 500; // ms

    private View recsLayout;
    private int recsLayoutInitHeight;
    private View barLayout;
    private boolean playBarExpanded = false;
    private boolean keepPlayBarOpen = true;

    /* Recordings Data */
    private ArrayList<Recording> recs = new ArrayList<>();
    private ListView recsView;
    private ArrayAdapter<Recording> recsAdapter;
    private static final String SAVED_RECS = "ss_rec_list";
    private Recording playingRec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        // get recordings from saved preferences and populate listview
        getRecordings();
        recsView = (ListView) findViewById(R.id.listRecordings);
        recsAdapter = new RecordingAdapter(this, R.layout.listview_recordings, recs);
        recsView.setAdapter(recsAdapter);

        recsLayout = findViewById(R.id.recordings_layout);
        barLayout = findViewById(R.id.music_bar);
        final ImageView playButton = (ImageView) findViewById(R.id.play_pause);
        playButton.setImageResource(R.drawable.play3);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        final TextView playText = (TextView) findViewById(R.id.play_text);
        final TextView playLength = (TextView) findViewById(R.id.play_length);
        final Recording.PlayListener playListener = new Recording.PlayListener() {
            @Override
            public void onUpdate(final int progress) {
                progressBar.setProgress(progress);
            }
            @Override
            public void onFinished() {
                playButton.setImageResource(R.drawable.play3);
                playButton.invalidate();
                closePlayBar();
            }
        };
        recsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (playingRec != null) {
                    keepPlayBarOpen = true;
                    playingRec.stop();
                    keepPlayBarOpen = false;
                }

                Recording rec = recs.get(position);
                playingRec = rec;
                rec.play(playListener);

                // Setup and display audio play bar
                playButton.setImageResource(R.drawable.pause3);
                playButton.invalidate();
                playText.setText(String.valueOf(rec.getId()));
                playLength.setText(rec.lengthString());
                progressBar.setMax(playingRec.frameLength());
                progressBar.setProgress(0);
                expandPlayBar();

            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playingRec == null) {
                    return;
                }
                if (!playingRec.isPlaying()) {
                    playingRec.play(playListener);
                    playButton.setImageResource(R.drawable.pause3);
                    playButton.invalidate();
                } else {
                    playButton.setImageResource(R.drawable.play3);
                    playButton.invalidate();
                    playingRec.pause();
                }
            }
        });
        barLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        registerReceiver(audioReceiver, new IntentFilter(WearAPIManager.AUDIO_INTENT));
    }


    @Override
    protected void onPause() {
        super.onPause();

        saveRecordings();
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
        Recording.setLastId(recs.get(0).getId()+1);
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


    public void expandPlayBar() {
        if (playBarExpanded) {
            return;
        }
        playBarExpanded = true;
        recsLayoutInitHeight = recsLayout.getHeight();
        recsLayout.startAnimation(new HeightAnimation(recsLayout, recsLayoutInitHeight, recsLayoutInitHeight - barLayout.getHeight(), MUSIC_BAR_ANIMATION_DURATION));
    }

    public void closePlayBar() {
        if (!playBarExpanded || keepPlayBarOpen) {
            return;
        }
        HeightAnimation closeAnim = new HeightAnimation(recsLayout, recsLayoutInitHeight - barLayout.getHeight(), recsLayoutInitHeight, MUSIC_BAR_ANIMATION_DURATION);
        closeAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                playBarExpanded = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        recsLayout.startAnimation(closeAnim);
    }

    public class HeightAnimation extends Animation {
        private View mView;
        private float mToHeight;
        private float mFromHeight;

        public HeightAnimation(View v, float fromHeight, float toHeight, int duration) {
            mToHeight = toHeight;
            mFromHeight = fromHeight;
            mView = v;
            setDuration(duration);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height =
                    (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            LayoutParams p = mView.getLayoutParams();
            p.height = (int) height;
            mView.requestLayout();
        }
    }
}
