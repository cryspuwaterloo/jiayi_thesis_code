package ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.DEBUG_FORCE_NEGATIVE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.DEBUG_FORCE_POSITIVE;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.AUTH_DUMMY_ID;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Timer;
import java.util.TimerTask;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.utils.RandomProbability;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.BYODContextService;

public class DummyAuthService extends Service {
    private static final String TAG = "DummyAuthService";
//    private Timer timer;
//    private TimerTask task = new TimerTask() {
//        @Override
//        public void run() {
////            RandomProbability rp = new RandomProbability(new int[] {0, 1},
////                    new float[]{0.05f, 0.95f});
//            Intent intent = new Intent(AUTH_DUMMY_ID);
//            intent.putExtra("result", 1.0);
//            long timeAnchor = SystemClock.elapsedRealtimeNanos();
//            Log.i("Time Anchor", "authissue:" + timeAnchor);
//            LocalBroadcastManager.getInstance(DummyAuthService.this).sendBroadcast(intent);
//        }
//    };

    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(AUTH_DUMMY_ID);
            intent.putExtra("result", 1.0);
            long timeAnchor = SystemClock.elapsedRealtimeNanos();
            Log.i("Time Anchor", "authissue:" + timeAnchor);
            LocalBroadcastManager.getInstance(DummyAuthService.this).sendBroadcast(intent);
            // handler.postDelayed(runnableCode, 5000);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // timer = new Timer();
        // timer.scheduleAtFixedRate(task, 2000, 5000); // 1s a result
        handler.postDelayed(runnableCode, 500);
        Log.i(TAG, "I start to work!");
    }

    @Override
    public void onDestroy() {
        long timeAnchor = SystemClock.elapsedRealtimeNanos();
        Log.i("Time Anchor", "authadapt:" + timeAnchor);
        // timer.cancel();
        super.onDestroy();
        handler.removeCallbacks(runnableCode);
        Intent intent = new Intent(this, BYODContextService.class);
        intent.setAction(DEBUG_FORCE_NEGATIVE);
        startService(intent);
        Log.i(TAG, "So long!");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(DEBUG_FORCE_POSITIVE)) {
                Intent intent1 = new Intent(AUTH_DUMMY_ID);
                intent1.putExtra("result", 1.0);
                long timeAnchor = SystemClock.elapsedRealtimeNanos();
                Log.i("Time Anchor", "authissue:" + timeAnchor);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            } else if (intent.getAction().equals(DEBUG_FORCE_NEGATIVE)) {
                Intent intent1 = new Intent(AUTH_DUMMY_ID);
                intent1.putExtra("result", 0.0);
                long timeAnchor = SystemClock.elapsedRealtimeNanos();
                Log.i("Time Anchor", "authissue:" + timeAnchor);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }




    // This something you need to add to your service code
}
