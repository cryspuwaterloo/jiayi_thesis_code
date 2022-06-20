package ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts;

import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.CONTEXT_ONFOOT_ID;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

public class OnFootReceiver extends BroadcastReceiver {
    private static final String TAG = "OnFootReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event: result.getTransitionEvents()) {
                Log.i(TAG, "capture " + event.getActivityType() + " " + event.getTransitionType() + event.getElapsedRealTimeNanos()/1e6);
                if (event.getActivityType() == DetectedActivity.ON_FOOT){
                    Intent i = new Intent(CONTEXT_ONFOOT_ID);
                    if(event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER){
                        i.putExtra("result", 1);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);

                    } else if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT){
                        i.putExtra("result", 0);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                    }
                }
            }
        }

        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity event = result.getMostProbableActivity();
            Log.i(TAG, "constant detect: " + result.getMostProbableActivity()  + result.getElapsedRealtimeMillis()/1e6);
            Intent i = new Intent(CONTEXT_ONFOOT_ID);
            if (event.getType() == DetectedActivity.ON_FOOT || event.getType() == DetectedActivity.WALKING) {
                i.putExtra("result", 1);
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            } else {
                Log.i(TAG, " send 0");
                i.putExtra("result", 0);
                long timeAnchor = SystemClock.elapsedRealtimeNanos();
                Log.i("Time Anchor", "Rececontext:" + timeAnchor);
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);

            }

        }
    }
}
