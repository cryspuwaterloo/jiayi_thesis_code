package ca.uwaterloo.cs.crysp.libmraacintegration.context;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.SERVICE_CONTEXT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Map;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;


public abstract class BaseContextProvider {
    private final String id;
    private final Context context;
    protected BroadcastReceiver receiver;
    private boolean isRunning=false;

    /*
    Note: we do not provide a callback for context provider, it can directly broadcast
    service_context signal to the main adaptation service
    However, we need a result Map to map the raw Context service result (int) to MRAAC signals.
    It is possible for the context service directly sends the context signal to the main adaptation
    service. However, we need to note that it
     */

    public BaseContextProvider(String id, Context context) {
        this.id = id;
        this.context = context;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int result = intent.getIntExtra("result", -1);
                processResult(result);
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, new IntentFilter(id));
    }


    public void start(Adaptation adaptation)  {
        // note: every context provider should have a default mode.
        this.isRunning = true;
    }

    public void stop() {
        this.isRunning = false;
    }

    public abstract void adapt(Adaptation adaptation);

    public String getId() {
        return id;
    }

    public Context getContext() {
        return context;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public abstract void processResult(int result);

    public void sendContextSignal(@NonNull Signal signal) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(signal.toIntent(SERVICE_CONTEXT));
    }
}
