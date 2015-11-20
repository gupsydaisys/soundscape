package palsofpaulos.soundscape;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.RecordingManager;
import palsofpaulos.soundscape.common.WearAPIManager;
import palsofpaulos.soundscape.common.LayoutAnimations.*;

public class AudioActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "Audio Activity";
    private static final int MUSIC_BAR_ANIMATION_DURATION = 500; // ms
    private static final int PLAY_LAYOUT_ANIMATION_DURATION = 700; // ms
    private static final int MAP_ANIMATION_DURATION = 200;

    private GoogleMap map;
    private HashMap<Marker, Integer> markerIdHashMap;

    Intent mobileMessengerIntent;

    private View listLayout;
    private View recsLayout;
    private View recsListLayout;
    private View mapLayout;
    private View barLayout;
    private View playLayout;

    private int listLayoutInitHeight;
    private int recsLayoutInitHeight;
    private int recsListLayoutInitHeight;
    private int barLayoutInitHeight;

    private boolean playBarExpanded = false;
    private boolean preventPlayBarClose = true;

    private ImageButton mapsButton;
    private ImageButton listButton;
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
    private Recording playingRec; // references the currently playing recording, null otherwise
    private int oldProgress; // playhead progress is reset to 0 on pause, this stores the progress before reset

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        // start MobileMessengerService to update location
        mobileMessengerIntent = new Intent(this, MobileMessengerService.class);
        startService(mobileMessengerIntent);

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
        oldProgress = 0;

        initializeButtons();

        registerReceiver(audioReceiver, new IntentFilter(WearAPIManager.AUDIO_INTENT));

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.recordings_map);
        mapFragment.getMapAsync(this);
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

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        markerIdHashMap = new HashMap<>();
        for (int ii = 0; ii < recs.size(); ii++) {
            Recording rec = recs.get(ii);
            LatLng latLng = new LatLng(rec.getLocation().getLatitude(), rec.getLocation().getLongitude());
            Marker recMarker = map.addMarker(new MarkerOptions().position(latLng).title(rec.getName()));
            markerIdHashMap.put(recMarker, ii);
        }
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                playRecAtPosition(markerIdHashMap.get(marker));
                return false;
            }
        });
        LatLng cameraLoc = new LatLng(WearAPIManager.currentLocation.getLatitude(), WearAPIManager.currentLocation.getLongitude());

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cameraLoc, 10);
        map.moveCamera(cameraUpdate);

        stopService(mobileMessengerIntent);
    }


    private BroadcastReceiver audioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String filePath = intent.getStringExtra(WearAPIManager.REC_FILEPATH);
            final Location recLoc = new Location("");
            double latitude = intent.getDoubleExtra(WearAPIManager.REC_LAT, 0);
            double longitude = intent.getDoubleExtra(WearAPIManager.REC_LNG, 0);
            recLoc.setLatitude(latitude);
            recLoc.setLongitude(longitude);

            Recording newRec = new Recording(filePath, recLoc);
            newRec.setName(intent.getStringExtra(WearAPIManager.REC_PLACE));


            recs.add(0, newRec);
            updateRecsView();
        }
    };


    private void initializeButtons() {
        mapsButton = (ImageButton) findViewById(R.id.maps_button);
        listButton = (ImageButton) findViewById(R.id.list_button);
        playButtonBar = (ImageView) findViewById(R.id.play_pause_bar);
        playButtonBar.setImageResource(R.drawable.play_button);
        playButtonBig = (ImageView) findViewById(R.id.play_pause_big);
        playButtonBig.setImageResource(R.drawable.play_button);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        playText = (TextView) findViewById(R.id.play_text);
        playLength = (TextView) findViewById(R.id.play_length);

        mapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
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
                    //closePlayBar();
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
        SharedPreferences prefs = getSharedPreferences(RecordingManager.SAVED_RECS, MODE_PRIVATE);
        for (int ii = 0; ; ii++){
            String recPath = prefs.getString(String.valueOf(ii) + "file", "");
            Location recLoc = new Location("");
            recLoc.setLatitude(Double.longBitsToDouble(prefs.getLong(String.valueOf(ii) + "lat", 0)));
            recLoc.setLongitude(Double.longBitsToDouble(prefs.getLong(String.valueOf(ii) + "lng", 0)));
            if (!recPath.equals("")){
                Recording addRec = new Recording(recPath, recLoc);
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
            editor.putString(String.valueOf(ii) + "file", rec.getFilePath());
            editor.putLong(String.valueOf(ii) + "lat", Double.doubleToRawLongBits(rec.getLocation().getLatitude()));
            editor.putLong(String.valueOf(ii) + "lng", Double.doubleToLongBits(rec.getLocation().getLongitude()));
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
        mapLayout.setVisibility(View.VISIBLE);
        recsListLayoutInitHeight = recsListLayout.getHeight();

        LatLng cameraLoc = new LatLng(WearAPIManager.currentLocation.getLatitude(), WearAPIManager.currentLocation.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cameraLoc, 10);
        map.moveCamera(cameraUpdate);

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
    }

    public void closeMapLayout() {
        recsListLayout.setVisibility(View.VISIBLE);

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


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
