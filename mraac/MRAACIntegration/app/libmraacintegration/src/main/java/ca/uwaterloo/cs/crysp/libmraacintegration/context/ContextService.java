package ca.uwaterloo.cs.crysp.libmraacintegration.context;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_START;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_STOP;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_TUNE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.DEBUG_FORCE_NEGATIVE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.SERVICE_CONTEXT;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;

public abstract class ContextService extends Service {
    private static final String TAG = "ContextService";
    private static final String SERVICE_ID = "context_core";

    protected Map<String, BaseContextProvider> contextProviders;

    protected final BroadcastReceiver adaptationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                Log.i(TAG, "receive " + intent.getAction());
                Adaptation s = Adaptation.fromIntent(intent);
                if (contextProviders.containsKey(s.getTargetId())) {
                    // we have this target and want to do something
                    Log.i(TAG, "Adaptation to " + s.getTargetId() + ", command: "+ s.getAdaptation());
                    BaseContextProvider provider = contextProviders.get(s.getTargetId());
                    assert provider!= null;
                    if (s.getAdaptation().equals(ADAPTATION_START)) {
                        if (provider.isRunning()) provider.adapt(s);
                        else provider.start(s);
                    }
                    if (s.getAdaptation().equals(ADAPTATION_STOP)) {
                        if (provider.isRunning())
                            provider.stop();
                    }
                    if (s.getAdaptation().equals(ADAPTATION_TUNE)) {
                        provider.adapt(s);
                    }
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.i(TAG, "receive " + intent.getAction());
            Adaptation s = Adaptation.fromIntent(intent);

            if (intent.getAction() != null && intent.getAction().equals(DEBUG_FORCE_NEGATIVE)) {
                Log.i(TAG, "restart everything");
                deactivateAll();
                activateAll();
            }

        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        contextProviders = new HashMap<>();
        registerContextProviders();
        activateAll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deactivateAll();
    }

    public abstract void registerContextProviders();

    public void addContextProviders(@NonNull BaseContextProvider... providers) {
        for(BaseContextProvider provider: providers) {
            contextProviders.put(provider.getId(), provider);
        }
    }

    private void activateAll() {
        for (String name: contextProviders.keySet()) {
            BaseContextProvider provider = contextProviders.get(name);
            if (provider != null) {
                provider.start(null);
                Log.i(TAG, "Activate " + provider.getId());
            }
        }
    }

    private  void deactivateAll(){
        for (String name: contextProviders.keySet()) {
            BaseContextProvider provider = contextProviders.get(name);
            if (provider != null) {
                provider.stop();
            }
        }
    }

    public void sendContextResult(@NonNull Signal contextSignal) {
        Intent intent = contextSignal.toIntent(SERVICE_CONTEXT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
