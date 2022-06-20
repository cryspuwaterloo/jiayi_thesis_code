package ca.uwaterloo.cs.crysp.libmraacintegration.auth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.reflect.Type;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;

public abstract class BaseAuthenticator {
    private final String id;
    private final Context context;
    protected BroadcastReceiver receiver;
    public AuthCallback authCallback;
    protected boolean isRunning=false;


    public BaseAuthenticator(String id, Context context, AuthCallback callback) {
        this.id = id;
        this.context = context;
        authCallback = callback;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isRunning) {
                    // only report the results when the authenticator is "running"
                    callback.onAuthResultReceived(intent.getDoubleExtra("result", -1));
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, new IntentFilter(id));
    }
    public void start(Adaptation adaptation) {
        this.isRunning = true;
    } // start to work
    public void stop() {
        this.isRunning = false;
    }; // stop working
    public abstract void adapt(Adaptation adaptation); // change parameters or behaviors

    public Context getContext() {
        return context;
    }

    public String getId() {
        return id;
    }

    public boolean isRunning() {
        return isRunning;
    }

}
