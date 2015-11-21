package palsofpaulos.soundscape;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import palsofpaulos.soundscape.common.WearAPIManager;

public class WearMessengerService extends WearableListenerService {

    public static final String RECORD_ACTIVITY = "/record_activity";
    public static final String TAG = "Wear Listener";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, messageEvent.getPath());
    }

    public static void sendMessage(final GoogleApiClient mApiClient, final String path, final String text) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

}
