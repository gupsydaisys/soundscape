package palsofpaulos.soundscape;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import palsofpaulos.soundscape.common.CommManager;

public class AwaitRecordActivity extends WearableActivity {

    private static final String TAG = "Await Record Activity";

    /* Recording Activity Detection */
    private static final int REC_TOUCHES = 3;
    private static final int INTERVAL = 1000;
    private static final int SECOND = 1000;
    private boolean useSpeechForName = false;
    private CountDownTimer touchTimer;
    private int touches = 0;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mSpeechForNameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_await_record);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mSpeechForNameText = (TextView) findViewById(R.id.speech_for_name_text);
        updateSpeechForNameText();

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
                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        touches = 0;
                    }
                };

                if (touches == REC_TOUCHES) {
                    recordIntent.putExtra(CommManager.SPEECH_FOR_NAME_EXTRA, useSpeechForName);
                    startActivity(recordIntent);
                }
            }
        });

        mContainerView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (useSpeechForName) {
                    useSpeechForName = false;
                }
                else {
                    useSpeechForName = true;
                }
                updateSpeechForNameText();

                return true;
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
        //updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        //updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        //updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackground(getResources().getDrawable(android.R.color.black));
        } else {
            mContainerView.setBackground(getResources().getDrawable(R.drawable.blue_black_texture));
        }
    }

    private void updateSpeechForNameText() {
        if (mSpeechForNameText == null) {
            return;
        }
        if (useSpeechForName) {
            mSpeechForNameText.setText(Html.fromHtml("Speech to Text Naming <font color='#9CCB46'>Enabled</font>"));
        }
        else {
            mSpeechForNameText.setText(Html.fromHtml("Speech to Text Naming <font color='#D94B4F'>Disabled</font>"));
        }
    }
}
