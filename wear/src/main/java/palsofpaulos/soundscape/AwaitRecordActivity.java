package palsofpaulos.soundscape;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AwaitRecordActivity extends WearableActivity {

    private static final String TAG = "Await Record Activity";

    /* Recording Activity Detection */
    private static final int INTERVAL = 1000;
    private static final int SECOND = 1000;
    private CountDownTimer touchTimer;
    private int timeSinceLastTouch = 0;
    private int touches = 0;

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_await_record);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);

        final Intent recordIntent = new Intent(this, RecordActivity.class);
        recordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                touches++;

                Log.d(TAG, "touch detected!");
                if (touchTimer != null) {
                    touchTimer.cancel();
                }
                touchTimer = new CountDownTimer(INTERVAL, SECOND) {
                    public void onTick(long millisUntilFinished) { }
                    public void onFinish() {
                        touches = 0;
                    }
                };

                if (touches == 2) {
                    startActivity(recordIntent);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        touches = 0;
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
        }
    }
}
