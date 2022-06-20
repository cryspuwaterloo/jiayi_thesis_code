package ca.uwaterloo.cs.crysp.libmraacintegration.access;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ACCESS_SIGNAL_HEADER;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ACQUIRE_CURRENT_SENSITIVITY;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.SERVICE_ACCESS;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;

public abstract class AccessControlService extends Service {


    private static final String TAG = "AccessControlModule";
    private static final String SERVICE_ID = "access_control";
    public static final String REPORT_SENSITIVITY_LEVEL = "Report sensitivity level";

    private Map<String, Integer> sensitivityDictionary;
    protected String currentResource;

    protected final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() !=null ) {
                Log.i(TAG, "receive " + intent.getAction());
                if (intent.getAction().equals(ACQUIRE_CURRENT_SENSITIVITY)) {
                    // broadcast
                    // Log.i(TAG, "Request for current sensitivity level.");
                    sensitivityChange(getSensitivityLevel(currentResource));
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
        sensitivityDictionary = new HashMap<>(); // empty dictionary, need to be updated somewhere
    }


    public void setSensitivityDictionary(Map<String, Integer> sensitivityDictionary) {
        this.sensitivityDictionary = sensitivityDictionary;
    }

    public Map<String, Integer> getSensitivityDictionary() {
        return sensitivityDictionary;
    }


    public void setCurrentResource(String currentResource) {
        int prev = getSensitivityLevel(this.currentResource);
        this.currentResource = currentResource;
        int sens = getSensitivityLevel(currentResource);
        if (prev != sens) sensitivityChange(sens);
    }

    public void setCurrentResource(String currentResource, boolean force) {
        int prev = getSensitivityLevel(this.currentResource);
        this.currentResource = currentResource;
        int sens = getSensitivityLevel(currentResource);
        if (prev != sens || force) sensitivityChange(sens);
    }

    protected int getSensitivityLevel(String resourceName) {
        if (!sensitivityDictionary.containsKey(resourceName) || sensitivityDictionary.get(resourceName) == null) {
            // Log.i(TAG, "Current resource not found, default Sensitivity level = 1");
            return 1;
        } else {
            return sensitivityDictionary.get(resourceName);
        }
    }

    protected void sensitivityChange(int sens) {
        Intent resultIntent = new Signal(ACCESS_SIGNAL_HEADER + sens, SERVICE_ID).toIntent(SERVICE_ACCESS);
        resultIntent.putExtra("result", sens);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
    }

    public abstract String initResourceRequest();
}
