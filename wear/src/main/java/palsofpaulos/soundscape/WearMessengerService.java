package palsofpaulos.soundscape;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WearMessengerService extends WearableListenerService {

    public static final String RECORD_ACTIVITY = "/record_activity";
    public static final String TAG = "Wear Listener";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, messageEvent.getPath());
    }

}
