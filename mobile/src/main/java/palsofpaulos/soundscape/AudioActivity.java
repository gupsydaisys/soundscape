package palsofpaulos.soundscape;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import palsofpaulos.soundscape.common.CommManager;
import palsofpaulos.soundscape.common.LayoutAnimations.HeightAnimation;
import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.RecordingException;
import palsofpaulos.soundscape.common.RecordingManager;

public class AudioActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "Audio Activity";
    private static final int MUSIC_BAR_ANIMATION_DURATION = 500; // ms
    private static final int PLAY_LAYOUT_ANIMATION_DURATION = 700; // ms
    private static final int MAP_ANIMATION_DURATION = 200;

    private GoogleMap map;
    private ArrayList<Marker> mapMarkers = new ArrayList<>();
    private HashMap<Marker, Recording> markerRecHashmap;

    private View listLayout;
    private View recsLayout;
    private View recsListLayout;
    private ListView recsView;
    private View mapLayout;
    private View barLayout;
    private View playLayout;

    private int listLayoutInitHeight;
    private int recsLayoutInitHeight;
    private int recsListLayoutInitHeight;
    private int barLayoutInitHeight;

    private boolean playBarExpanded = false;
    private boolean preventPlayBarClose = true;
    private boolean blockSeekUpdate = false;

    private ImageButton mapsButton;
    private ImageButton listButton;
    private ImageView playButtonBar;
    private ImageView playButtonBig;
    private ProgressBar progressBar;
    private SeekBar seekBar;
    private TextView playText;
    private TextView playLength;
    private TextView seekCurrentTime;
    private TextView seekTotalTime;
    private TextView playTextBig;
    private EditText playTextEdit;
    private EditText playRating;
    private Recording.PlayListener playListener;

    /* Recordings Data */
    private ArrayList<Recording> recs = new ArrayList<>();

    private ArrayAdapter<Recording> recsAdapter;
    private Recording playingRec; // references the currently playing recording, null otherwise
    private Intent responseIntent = new Intent(CommManager.AUDIO_RESPONSE_INTENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        Intent messengerIntent = new Intent(this, MobileMessengerService.class);
        startService(messengerIntent);
        sendBroadcast(responseIntent);

        // get recordings from saved preferences and populate listview
        getRecordings();
        recsView = (ListView) findViewById(R.id.listRecordings);
        recsAdapter = new RecordingAdapter(this, R.layout.listview_recordings, recs);
        recsView.setAdapter(recsAdapter);

        listLayout = findViewById(R.id.list_layout);
        recsLayout = findViewById(R.id.recordings_layout);
        recsListLayout = findViewById(R.id.recordings_list);
        mapLayout = findViewById(R.id.map_layout);
        barLayout = findViewById(R.id.play_bar);
        playLayout = findViewById(R.id.play_layout);
        playLayout.setVisibility(View.GONE);
        seekCurrentTime = (TextView) findViewById(R.id.seek_current_time);
        seekTotalTime = (TextView) findViewById(R.id.seek_total_time);
        playTextBig = (TextView) findViewById(R.id.play_text_big);

        initializeButtons();

        registerReceiver(audioReceiver, new IntentFilter(CommManager.AUDIO_INTENT));

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.recordings_map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopPlayback();
        saveRecordings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(audioReceiver);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        markerRecHashmap = new HashMap<>();
        for (int ii = 0; ii < recs.size(); ii++) {
            addMarkerForRec(ii);
        }
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                try {
                    Recording markerRec = markerRecHashmap.get(marker);
                    marker.setTitle(markerRec.getName());
                    playRec(markerRec);
                }
                catch (RecordingException e) {
                    marker.remove();
                    Toast.makeText(getApplicationContext(), "This recording no longer exists",
                            Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });
        LatLng cameraLoc = new LatLng(CommManager.currentLocation.getLatitude(), CommManager.currentLocation.getLongitude());

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cameraLoc, 10);
        map.moveCamera(cameraUpdate);
    }

    private void addMarkerForRec(int position) {
        Recording rec = recs.get(position);
        LatLng latLng = new LatLng(rec.getLocation().getLatitude(), rec.getLocation().getLongitude());
        Marker recMarker = map.addMarker(new MarkerOptions()
                .position(latLng)
                .title(rec.getName())
                .snippet(rec.getDateString()));
        mapMarkers.add(position, recMarker);
        markerRecHashmap.put(recMarker, rec);
    }

    private BroadcastReceiver audioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            sendBroadcast(responseIntent);

            final String filePath = intent.getStringExtra(CommManager.REC_FILEPATH);
            // a null recording request was sent, indicating it was just looking for a response
            if (filePath.equals(CommManager.NULL_REC_PATH)) {
                return;
            }
            else if (filePath.equals(CommManager.RENAME_PATH)) {
                recs.get(0).setName(intent.getStringExtra(CommManager.REC_NAME));
                recsAdapter.notifyDataSetChanged();
                return;
            }

            final Location recLoc = new Location("");
            double latitude = intent.getDoubleExtra(CommManager.REC_LAT, 0);
            double longitude = intent.getDoubleExtra(CommManager.REC_LNG, 0);
            recLoc.setLatitude(latitude);
            recLoc.setLongitude(longitude);
            final Date recDate = RecordingManager.recDateFromString(intent.getStringExtra(CommManager.REC_DATE));

            Recording newRec = new Recording(filePath, recLoc, recDate);
            newRec.setName(intent.getStringExtra(CommManager.REC_NAME));


            recs.add(0, newRec);
            addMarkerForRec(0);

            updateRecsView();
        }
    };

    public void watchNotification(View view) {
        int notificationId = 001;
        // Build intent for notification content
        Intent viewIntent = new Intent(this, AudioActivity.class);
        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(this, 0, viewIntent, 0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.soundscape_ic)
                        .setContentTitle("A recording is nearby!")
                        .setContentText("Recording Title")
                        .setContentIntent(viewPendingIntent);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notificationBuilder.build());
    }


    private void initializeButtons() {
        mapsButton = (ImageButton) findViewById(R.id.maps_button);
        listButton = (ImageButton) findViewById(R.id.list_button);
        playButtonBar = (ImageView) findViewById(R.id.play_pause_bar);
        playButtonBar.setImageResource(R.drawable.play_button);
        playButtonBig = (ImageView) findViewById(R.id.play_pause_big);
        playButtonBig.setImageResource(R.drawable.play_button);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        playText = (TextView) findViewById(R.id.play_text_bar);
        playLength = (TextView) findViewById(R.id.play_length);
        playTextEdit = (EditText) findViewById(R.id.play_text_edit);
        playRating = (EditText) findViewById(R.id.rating);

        // Rating change
        playRating.setOnFocusChangeListener(new OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    playingRec.setRating(Integer.parseInt(String.valueOf(playRating.getText())));
                }

            }
        });

        mapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommManager.isNetworkAvailable(AudioActivity.this)) {
                    expandMapLayout();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Network connection required to use maps view",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeMapLayout();
            }
        });

        playTextBig.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                playTextBig.setVisibility(View.GONE);
                playTextEdit.setVisibility(View.VISIBLE);
                playTextEdit.requestFocus();
                playTextEdit.selectAll();
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showSoftInput(playTextEdit, 0);
                return false;
            }
        });

        playTextEdit.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE || event != null &&
                                event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (!(event != null && event.isShiftPressed())) {
                                // the user is done typing.
                                playingRec.setName(playTextEdit.getText().toString());
                                setPlayText(playingRec.getName());
                                playTextEdit.setVisibility(View.GONE);
                                playTextBig.setVisibility(View.VISIBLE);
                                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                mgr.hideSoftInputFromWindow(playTextEdit.getWindowToken(), 0);

                                recsAdapter.notifyDataSetChanged();
                                return true; // consume.
                            }
                        }
                        return false; // pass on to other listeners.
                    }
                });

        // playListener defines what actions to take when a song progresses and when it ends
        playListener = new Recording.PlayListener() {
            @Override
            public void onUpdate(final int progress) {
                if (!blockSeekUpdate) {
                    setProgressBars(progress);
                    seekCurrentTime.setText(currentPlayTime());
                }
            }
            @Override
            public void onFinished() {
                setPlayButtons();
                setProgressBars(progressBar.getMax());
                if (playLayout.getVisibility() != View.VISIBLE && (playingRec == null || playingRec.isDeleted())) {
                    closePlayBar();
                }
            }
        };

        recsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    playRecAtPosition(position);
                }
                catch (RecordingException e) {
                    Log.e(TAG, "Tried to play deleted recording!");
                    Toast.makeText(getApplicationContext(), "This recording no longer exists",
                            Toast.LENGTH_LONG).show();
                }
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
                seekCurrentTime.setText(virtualPlayTime(progress));
                if (fromUser) {
                    playingRec.setPlayHead(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                blockSeekUpdate = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                blockSeekUpdate = false;
            }
        });
    }

    /* Methods to handle playback, ensuring only one recording plays at a time */

    public void playRecAtPosition(int position) throws RecordingException {
        playRec(recs.get(position));
    }

    public void playRec(Recording rec) throws RecordingException {
        if (playingRec != null) {
            preventPlayBarClose = true;
            playingRec.stop();
            preventPlayBarClose = false;
        }

        playingRec = rec;

        if (rec == null || rec.isDeleted()) {
            throw new RecordingException("Tried to play recording" + rec.getId() + " which has been deleted!");
        }

        rec.play(playListener);

        // Setup and display audio play bar
        setPauseButtons();
        setPlayText(rec.getName());
        playTextEdit.setText(rec.getName());
        playLength.setText(rec.lengthString());
        playRating.setText(Integer.toString(rec.getRating()));
        setProgressBarsMax(playingRec.frameLength());
        setProgressBars(0);
        seekCurrentTime.setText("--:--");
        seekTotalTime.setText(rec.lengthString());
        expandPlayBar();
    }

    public void stopPlayback() {
        if (playingRec != null) {
            playingRec.stop();
        }
    }

    public String currentPlayTime() {
        int playHead = playingRec.currentPlayTime();
        return String.format("%d:%02d", playHead / 60, playHead % 60);
    }

    public String virtualPlayTime(int progress) {
        int playHead = (progress / RecordingManager.SAMPLERATE);
        return String.format("%d:%02d", playHead / 60, playHead % 60);
    }

    /* Methods to Handle Recording Data */

    private void getRecordings() {
        recs.clear();
        SharedPreferences prefs = getSharedPreferences(RecordingManager.SAVED_RECS, MODE_PRIVATE);
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
                recs.add(addRec);
            } else {
                Log.d("Recordings Loaded:", String.valueOf(ii));
                break; // Empty String means the default value was returned.
            }
        }
        if (recs.size() > 0) {
            Recording.setLastId(recs.get(0).getId()+1);
        }
    }

    private void saveRecordings() {
        SharedPreferences prefs = getSharedPreferences(RecordingManager.SAVED_RECS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (int ii = 0; ii < recs.size(); ii++) {
            Recording rec = recs.get(ii);
            editor.putString(ii + "file", rec.getFilePath());
            editor.putLong(ii + "lat", Double.doubleToRawLongBits(rec.getLocation().getLatitude()));
            editor.putLong(ii + "lng", Double.doubleToLongBits(rec.getLocation().getLongitude()));
            editor.putString(ii + "place", rec.getName());
            editor.putString(ii + "date", rec.getDateStorageString());
        }
        editor.commit();
    }

    private void clearRecordings() {
        SharedPreferences prefs = getSharedPreferences(RecordingManager.SAVED_RECS, MODE_PRIVATE);
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

    public void setPlayText(String name) {
        if (name.equals("")) {
            playText.setText("(Untitled)");
            playTextBig.setText("(Untitled)");
        }
        else {
            playText.setText(name);
            playTextBig.setText(name);
        }
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

    public void expandMapLayout() {

        LatLng cameraLoc;
        if (playingRec != null) {
            cameraLoc = new LatLng(playingRec.getLocation().getLatitude(), playingRec.getLocation().getLongitude());
        }
        else {
            cameraLoc = new LatLng(CommManager.currentLocation.getLatitude(), CommManager.currentLocation.getLongitude());
        }
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cameraLoc, 15);
        map.moveCamera(cameraUpdate);

        mapLayout.setVisibility(View.VISIBLE);
        recsListLayout.setVisibility(View.GONE);

        /*
        recsListLayoutInitHeight = recsListLayout.getHeight();
        HeightAnimation closeAnim = new HeightAnimation(recsListLayout, recsListLayoutInitHeight, 0, MAP_ANIMATION_DURATION);
        closeAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                recsListLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        recsListLayout.startAnimation(closeAnim);
        */
    }

    public void closeMapLayout() {
        recsListLayout.setVisibility(View.VISIBLE);
        mapLayout.setVisibility(View.GONE);

        /*
        final HeightAnimation openRecsListAnim = new HeightAnimation(recsListLayout, 0, recsListLayoutInitHeight, MAP_ANIMATION_DURATION);
        openRecsListAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mapLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        recsListLayout.startAnimation(openRecsListAnim);
        */
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
