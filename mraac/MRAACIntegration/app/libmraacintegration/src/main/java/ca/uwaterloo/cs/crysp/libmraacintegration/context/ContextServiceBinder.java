package ca.uwaterloo.cs.crysp.libmraacintegration.context;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public interface ContextServiceBinder {
    default void sendResult(Context context, String id, int result) {
        Intent intent = new Intent(id);
        intent.putExtra("result", result);
        long timeAnchor = SystemClock.elapsedRealtimeNanos();
        Log.i("Time Anchor", id + ":" + timeAnchor);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
