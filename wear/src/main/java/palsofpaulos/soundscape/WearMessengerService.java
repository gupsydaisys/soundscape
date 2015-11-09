package palsofpaulos.soundscape;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WearMessengerService extends WearableListenerService {

    public static final String RECORD_ACTIVITY = "/record_activity";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("Wear Listener", messageEvent.getPath());
    }

    public static void sendMessage( final GoogleApiClient mApiClient, final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                mApiClient.connect();
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
                mApiClient.disconnect();
            }
        }).start();
    }

    public static void sendData( final GoogleApiClient mApiClient, final String path, final byte[] data ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                mApiClient.connect();
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, data).await();
                }
                mApiClient.disconnect();
            }
        }).start();
    }
}
