package palsofpaulos.soundscape;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


import java.util.ArrayList;

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.WearAPIManager;
import palsofpaulos.soundscape.common.LayoutAnimations.*;

public class AudioActivity extends AppCompatActivity {

    private static final String TAG = "Audio Activity";
    private static final int MUSIC_BAR_ANIMATION_DURATION = 500; // ms
    private static final int PLAY_LAYOUT_ANIMATION_DURATION = 700; // ms

    private View listLayout;
    private int listLayoutInitHeight;
    private View recsLayout;
    private int recsLayoutInitHeight;
    private View barLayout;
    private int barLayoutInitHeight;
    private boolean playBarExpanded = false;
    private boolean preventPlayBarClose = true;
    private View playLayout;

    private ImageView playButtonBar;
    private ImageView playButtonBig;
    private ProgressBar progressBar;
    private SeekBar seekBar;
    private TextView playText;
    private TextView playLength;
    private Recording.PlayListener playListener;

    /* Recordings Data */
    private ArrayList<Recording> recs = new ArrayList<>();
    private ListView recsView;
    private ArrayAdapter<Recording> recsAdapter;
    private static final String SAVED_RECS = "ss_rec_list";
    private Recording playingRec; // references the currently playing recording, null otherwise
    private int oldProgress; // playhead progress is reset to 0 on pause, this stores the progress before reset

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        // get recordings from saved preferences and populate listview
        getRecordings();
        recsView = (ListView) findViewById(R.id.listRecordings);
        recsAdapter = new RecordingAdapter(this, R.layout.listview_recordings, recs);
        recsView.setAdapter(recsAdapter);

        listLayout = findViewById(R.id.list_layout);
        recsLayout = findViewById(R.id.recordings_layout);
        barLayout = findViewById(R.id.play_bar);
        playLayout = findViewById(R.id.play_layout);
        playLayout.setVisibility(View.GONE);
        oldProgress = 0;

        initializeButtons();

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


    private void initializeButtons() {
        playButtonBar = (ImageView) findViewById(R.id.play_pause_bar);
        playButtonBar.setImageResource(R.drawable.play_button);
        playButtonBig = (ImageView) findViewById(R.id.play_pause_big);
        playButtonBig.setImageResource(R.drawable.play_button);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        playText = (TextView) findViewById(R.id.play_text);
        playLength = (TextView) findViewById(R.id.play_length);

        // playListener defines what actions to take when a song progresses
        // and when it ends
        playListener = new Recording.PlayListener() {
            @Override
            public void onUpdate(final int progress) {
                setProgressBars(oldProgress + progress);
            }
            @Override
            public void onFinished() {
               setPlayButtons();
                oldProgress = 0;
                if (playLayout.getVisibility() != View.VISIBLE) {
                    closePlayBar();
                }
            }
        };

        recsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playRecAtPosition(position);
            }
        });

        View.OnClickListener playPauseListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playingRec == null) {
                    return;
                }
                if (!playingRec.isPlaying()) {
                    playingRec.play(playListener);
                    setPauseButtons();
                } else {
                    setPlayButtons();
                    playingRec.pause();
                }
            }
        };
        // play/pause button action
        playButtonBar.setOnClickListener(playPauseListener);
        playButtonBig.setOnClickListener(playPauseListener);

        // clicking the play bar expands the full play layout
        barLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandPlayLayout();
            }
        });

        final RelativeLayout backButton = (RelativeLayout) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePlayLayout();
            }
        });

        // adjusting the seekbar changes the song position
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                oldProgress = seekBar.getProgress();
                playingRec.setPlayHead(oldProgress);
            }
        });
    }

    public void playRecAtPosition(int position) {
        if (playingRec != null) {
            preventPlayBarClose = true;
            playingRec.stop();
            preventPlayBarClose = false;
        }
        oldProgress = 0;

        Recording rec = recs.get(position);
        playingRec = rec;
        rec.play(playListener);

        // Setup and display audio play bar
        setPauseButtons();
        playText.setText(String.valueOf(rec.getId()));
        playLength.setText(rec.lengthString());
        setProgressBarsMax(playingRec.frameLength());
        setProgressBars(0);
        expandPlayBar();
    }



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

    // call after changes recordings data to update the listview
    private void updateRecsView() {
        recsView.destroyDrawingCache();
        RecordingAdapter ra = (RecordingAdapter) recsView.getAdapter();
        ra.notifyDataSetChanged();
        recsView.invalidate();
    }






    /* Animation Methods */

    public void setPlayButtons() {
        playButtonBar.setImageResource(R.drawable.play_button);
        playButtonBig.setImageResource(R.drawable.play_button);
    }

    public void setPauseButtons() {
        playButtonBar.setImageResource(R.drawable.pause_button);
        playButtonBig.setImageResource(R.drawable.pause_button);
    }

    public void setProgressBars(int progress) {
        progressBar.setProgress(progress);
        seekBar.setProgress(progress);
    }

    public void setProgressBarsMax(int max) {
        progressBar.setMax(max);
        seekBar.setMax(max);
    }

    public void expandPlayBar() {
        if (playBarExpanded) {
            return;
        }
        playBarExpanded = true;
        recsLayoutInitHeight = recsLayout.getHeight();
        barLayoutInitHeight = barLayout.getHeight();
        recsLayout.startAnimation(new HeightAnimation(recsLayout, recsLayoutInitHeight, recsLayoutInitHeight - barLayoutInitHeight, MUSIC_BAR_ANIMATION_DURATION));
    }

    public void closePlayBar() {
        if (!playBarExpanded || preventPlayBarClose) {
            return;
        }
        HeightAnimation closeAnim = new HeightAnimation(recsLayout, recsLayoutInitHeight - barLayoutInitHeight, recsLayoutInitHeight, MUSIC_BAR_ANIMATION_DURATION);
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

    public void expandPlayLayout() {
        preventPlayBarClose = true;
        playLayout.setVisibility(View.VISIBLE);

        listLayoutInitHeight = listLayout.getHeight();
        final HeightAnimation closeListAnim = new HeightAnimation(listLayout, listLayoutInitHeight, 0, PLAY_LAYOUT_ANIMATION_DURATION);
        closeListAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                listLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        listLayout.startAnimation(closeListAnim);
    }

    public void closePlayLayout() {
        listLayout.setVisibility(View.VISIBLE);
        final HeightAnimation openListAnim = new HeightAnimation(listLayout, 0, listLayoutInitHeight, PLAY_LAYOUT_ANIMATION_DURATION);
        openListAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                playLayout.setVisibility(View.GONE);
                preventPlayBarClose = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        listLayout.startAnimation(openListAnim);
    }


}
