package ca.uwaterloo.cs.crysp.libmraacintegration.auth;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.*;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;


public abstract class AuthenticationService extends Service {
    private static final String TAG = "AuthenticationService";
    public static final String SERVICE_ID = "auth_core";
    public static final String ACTION_ADAPTATION = "adaptation";
    public static final String CLIENT_AUTH_ID = "clientgeneral";

    protected Context context;
    protected Map<String, BaseAuthenticator> authenticators;
    private final BroadcastReceiver adaptationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                Adaptation s = Adaptation.fromIntent(intent);
                if (s.getTargetId() != null && s.getTargetId().equals(SERVICE_ID)) {
                    adaptAggregation(s);
                }
                else if (authenticators.containsKey(s.getTargetId())) {
                    // we have this target and want to do something
                    Log.i(TAG, "Adaptation to " + s.getTargetId() + ", command: "+ s.getAdaptation());
                    BaseAuthenticator auth = authenticators.get(s.getTargetId());
                    assert auth != null;
                    if (s.getAdaptation().equals(ADAPTATION_START)) {
                        if (auth.isRunning()) auth.adapt(s);
                        else auth.start(s);
                    }
                    if (s.getAdaptation().equals(ADAPTATION_STOP)) {
                        if (auth.isRunning())
                            auth.stop();
                    }
                    if (s.getAdaptation().equals(ADAPTATION_TUNE)) {
                        auth.adapt(s);
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
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(adaptationReceiver,
                new IntentFilter(SERVICE_AUTHENTICATION));
        authenticators = new HashMap<>();
        registerAuthenticators();
        Log.i(TAG, "register authenticators: " + authenticators.keySet().toString());
    }

    public void addAuthenticators(BaseAuthenticator... authenticators) {
        for (BaseAuthenticator auth: authenticators) {
            this.authenticators.put(auth.getId(), auth);
        }
    }

    public abstract void registerAuthenticators();

    public abstract void aggregateResult(String authName, double result);

    public void sendAuthResult(Signal signal) {
        Intent intent = signal.toIntent(SERVICE_AUTHENTICATION);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void adaptAggregation(Adaptation adaptation) {

    }
}
