package ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts;

import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.CONTEXT_ONFOOT_ID;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import java.util.ArrayList;
import java.util.List;

import ca.uwaterloo.cs.crysp.libmraacintegration.context.ContextServiceBinder;
import ca.uwaterloo.cs.crysp.mraacintegration.tool.MockOutputProvider;

public class OnFootService extends Service implements ContextServiceBinder{
    private static final String TAG = "OnFootService";
    private static final boolean USE_MOCK_RESULT = true;

    List<ActivityTransition> transitions;
    MockOutputProvider<Integer> mockOutputProvider;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // for experiments only
        mockOutputProvider = new MockOutputProvider<>();
        mockOutputProvider.addN(1, 1);
        mockOutputProvider.addN(0, 1);

        transitions = new ArrayList<>();
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        );
        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        // passive mode
//        Task<Void> task = ActivityRecognition.getClient(this)
//                .requestActivityTransitionUpdates(request, getPendingIntent());
        // consistent mode
        Task<Void> task = ActivityRecognition.getClient(this)
                .requestActivityUpdates(100, getPendingIntent());

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "Have onfoot task ready");
                    }
                }
        );
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "NO! Fail to have on foot");
                    }
                }
        );

    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, OnFootService.class);
        intent.setAction(CONTEXT_ONFOOT_ID);
        return PendingIntent.getService(
                this,
                1233,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() !=null && intent.getAction().equals(CONTEXT_ONFOOT_ID)) {
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event: result.getTransitionEvents()) {
                    Log.i(TAG, "capture " + event.getActivityType() + " " + event.getTransitionType() + event.getElapsedRealTimeNanos()/1e6);
                    if (event.getActivityType() == DetectedActivity.ON_FOOT){
                        if(event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER){
                            sendResult(this, CONTEXT_ONFOOT_ID, 1);

                        } else if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT){
                            sendResult(this, CONTEXT_ONFOOT_ID, 0);
                        }
                    }
                }
            }

            if (ActivityRecognitionResult.hasResult(intent)) {
                if (USE_MOCK_RESULT) {
                    sendResult(this, CONTEXT_ONFOOT_ID, mockOutputProvider.next());
                }
                else {
                    ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                    DetectedActivity event = result.getMostProbableActivity();
                    Log.i(TAG, "constant detect: " + result.getMostProbableActivity());
                    Intent i = new Intent(CONTEXT_ONFOOT_ID);
                    if (event.getType() == DetectedActivity.ON_FOOT || event.getType() == DetectedActivity.WALKING) {
                        sendResult(this, CONTEXT_ONFOOT_ID, 1);
                    } else {
                        long timeAnchor = SystemClock.elapsedRealtimeNanos();
                        Log.i("Time Anchor", "Rececontext:" + timeAnchor);
                        sendResult(this, CONTEXT_ONFOOT_ID, 0);

                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
