package ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.DEBUG_FORCE_NEGATIVE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.DEBUG_FORCE_POSITIVE;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.AUTH_DUMMY_ID;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.CONTEXT_DUMMY_ID;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleDummy.CONTEXT_DUMMY2_ID;

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

import ca.uwaterloo.cs.crysp.libmraacintegration.context.ContextServiceBinder;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators.DummyAuthService;

public class DummyConditionService extends Service implements ContextServiceBinder {
    private static final String TAG = "DummyContextService";
    private Timer timer;
    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
//            Intent intent = new Intent(DummyContextProvider.CONTEXT_NAME);
//            RandomProbability rp = new RandomProbability(new int[] {0, 1},
//                    new float[]{0.7f, 0.3f});
//            intent.putExtra("result", rp.nextInt());
//            LocalBroadcastManager.getInstance(DummyContextService.this).sendBroadcast(intent);
            sendResult(DummyConditionService.this, CONTEXT_DUMMY2_ID, 1);
        }
    };

    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            // Repeat this the same runnable code block again another 2 seconds
            sendResult(DummyConditionService.this, CONTEXT_DUMMY2_ID, 1);
            handler.postDelayed(runnableCode, 2000);
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
//        timer = new Timer();
//        timer.scheduleAtFixedRate(task, 0, 5000);
        handler.postDelayed(runnableCode, 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // timer.cancel();
        handler.removeCallbacks(runnableCode);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(DEBUG_FORCE_POSITIVE)) {
                sendResult(this, CONTEXT_DUMMY2_ID, 1);
            } else if (intent.getAction().equals(DEBUG_FORCE_NEGATIVE)) {
                sendResult(this, CONTEXT_DUMMY2_ID, 0);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
