package palsofpaulos.soundscape;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ToggleButton notifyToggle = (ToggleButton) findViewById(R.id.toggleButton);

        final Intent notifyServiceIntent = new Intent(this, NotifyService.class);

        notifyToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService(notifyServiceIntent);
                }
                else {
                    stopService(notifyServiceIntent);
                }
            }
        });
    }

    public void showRecordings(View view) {
        startActivity(new Intent(this, AudioActivity.class));
    }
}
