package ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators;

import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.AUTH_ITUS_ID;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Timer;
import java.util.TimerTask;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.utils.RandomProbability;

public class ItusDaemonService extends Service {
    private static final String TAG = "ItusDaemon";

    private Timer timer;
    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            Intent intent = new Intent(AUTH_ITUS_ID);
            RandomProbability rp = new RandomProbability(new int[] {0, 1},
                    new float[]{0.05f, 0.95f});
            intent.putExtra("result", (double) rp.nextInt());
            LocalBroadcastManager.getInstance(ItusDaemonService.this).sendBroadcast(intent);
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
        timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, 5000); // 5s a result
        Log.i(TAG, "I start to work!");
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        super.onDestroy();
        Log.i(TAG, "So long!");

    }
}
