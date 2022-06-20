package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ACQUIRE_CURRENT_SENSITIVITY;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_GET_CURRENT_STAGE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_START;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_STOP;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.MRAAC_STAGE_CHANGE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.SERVICE_ACCESS;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.SERVICE_AUTHENTICATION;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.SERVICE_CONTEXT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ca.uwaterloo.cs.crysp.libmraacintegration.IMRAACClientInterface;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.AdaptationScheme;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Stage;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.AuthenticationService;


public abstract class AdaptationService extends Service {

    private static final String TAG = "MRAACService";

    private MultiStageModel model;
    private Stage currentStage;
    private Stage prevStage;

    private IMRAACClientInterface clientInterface = null;

    // debug helper
    int debugCounter = 0;

    private final BroadcastReceiver authReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equals(SERVICE_AUTHENTICATION)) {
                    Signal s = Signal.fromIntent(intent);
                    if (s.getName() == null) return;
                    Log.d(TAG, "Receive from Authentication Service: " + s.getName());
                    // long timeAnchor = SystemClock.elapsedRealtimeNanos();
                    // Log.i("Time Anchor", "authreceive:" + timeAnchor);
                    // Debug.startMethodTracing("processSignal" + debugCounter);
                    // Log.d("MRAAC_EXP", "auth signal: " + s.getName());
                    // debugCounter ++;
                    processSignal(s);
                    // Debug.stopMethodTracing();
                }
            }
        }
    };

    private final BroadcastReceiver accessReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                Signal s = Signal.fromIntent(intent);
                Log.d(TAG, "Receive from Access Control Service: " + s.getName());
                // Debug.startMethodTracing("processSignal" + debugCounter);
                // debugCounter ++;
                // Log.d("MRAAC_EXP", "access signal: " + s.getName());
                processSignal(s);
                // Debug.stopMethodTracing();
            }
        }
    };

    private final BroadcastReceiver contextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equals(SERVICE_CONTEXT)) {
                    Signal s = Signal.fromIntent(intent);
                    Log.d(TAG, "Receive from Context Service: " + s.getName());
                    // long timeAnchor = SystemClock.elapsedRealtimeNanos();
                    // Log.i("Time Anchor", s.getSourceId() + "receive:" + timeAnchor);
//                    if (debugCounter == 10) {
//                        Log.e("MRAAC_", "END");
//                    }
                    // Debug.startMethodTracing("adapt-pixel-" + debugCounter);
                    // debugCounter ++;
                    processSignal(s);
                    // Log.d("MRAAC_EXP", "context signal: " + s.getName());
                    // Debug.stopMethodTracing();
                }
            }

        }
    };

    @Override
    public void onCreate() {

        super.onCreate();
        initModel();
        currentStage = model.getInitStage();
        prevStage = model.getLockedStage();
        LocalBroadcastManager.getInstance(this).registerReceiver(authReceiver, new IntentFilter(SERVICE_AUTHENTICATION));
        LocalBroadcastManager.getInstance(this).registerReceiver(accessReceiver, new IntentFilter(SERVICE_ACCESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(contextReceiver, new IntentFilter(SERVICE_CONTEXT));
        // build connections with context/access control/authentication services


        Log.i(TAG, "broadcast initial adaptation");
        stageChangeProcession();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return new IMRAACClientInterface.Stub() {
            @Override
            public String getCurrentRiskType() throws RemoteException {
                // long timeAnchor = SystemClock.elapsedRealtimeNanos();
                // Log.i("Time Anchor", "currentRisk:" + timeAnchor);
                return currentStage.getRiskType();
            }

            @Override
            public int getCurrentAuthenticationLevel() throws RemoteException {
                return currentStage.getAuthenticationLevel();
            }

            @Override
            public int getCurrentSensitivityLevel() throws RemoteException {
                return currentStage.getSensitivityLevel();
            }

            @Override
            public int sendIAResult(int result, double score) throws RemoteException {
                // TODO: forward IA result to Client IA provider
                Intent ia = new Intent(AuthenticationService.CLIENT_AUTH_ID);
                ia.putExtra("result", score);
                // long timeAnchor = SystemClock.elapsedRealtimeNanos();
                // Log.i("Time Anchor", "serveria:" + timeAnchor);
                LocalBroadcastManager.getInstance(AdaptationService.this).sendBroadcast(ia);
                return 0;
            }
        };
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(accessReceiver);
        unregisterReceiver(authReceiver);
        unregisterReceiver(contextReceiver);
        super.onDestroy();
    }


    private void processSignal(Signal signal) {
        // first go through internal adaptation

        AdaptationScheme scheme = currentStage.getScheme();
        if (scheme == null) return;

        List<Adaptation> adaptations = scheme.acquireAdaptationsBySignal(signal);

        if (!adaptations.isEmpty()) {
            for(Adaptation ad: adaptations) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(ad.toIntent(SERVICE_AUTHENTICATION));
            }
        }

        if (model.isSupportedSignals(signal.getName())) {
            Stage nextStage = model.makeTransition(currentStage, signal);
            if (nextStage != currentStage) {
                // Stage change part
                prevStage = currentStage;
                currentStage = nextStage;
                Log.d(TAG, "Stage Change: " + prevStage.getStageId() + " to " + currentStage.getStageId());
                stageChangeProcession();
            }
        }
    }


    public void stageChangeProcession() {
        // we make all communications with local broadcasting

        // Intent intent = new Intent(ACQUIRE_CURRENT_SENSITIVITY);
        List<String> stopList = new ArrayList<>();
        Set<String> currentAuth = currentStage.getScheme().getAllRelatedAuthenticators();
        for(Adaptation ad: prevStage.getScheme().getDefaultAuthenticators()) {
            if (ad.getAdaptation().equals(ADAPTATION_START)) {
                if (!currentAuth.contains(ad.getTargetId())) {
                    stopList.add(ad.getTargetId());
                }
            }
        }
        for (String target: stopList) {
            // Log.i(TAG, "Stage change: send stopping signal to " + target);
            Intent i = new Adaptation(target, ADAPTATION_STOP).toIntent(SERVICE_AUTHENTICATION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        }

        List<Adaptation> adaptations = currentStage.getScheme().getDefaultAuthenticators();
        if (!adaptations.isEmpty()) {
            for(Adaptation ad: adaptations) {
                Log.i(TAG, "broadcast " + ad.getTargetId() + " " + ad.getAdaptation());
                LocalBroadcastManager.getInstance(this).sendBroadcast(ad.toIntent(SERVICE_AUTHENTICATION));
            }
        }

        // whenever a stage change happens, the system proactively acquires the current signal for adaptation
        List<String> acquireSignals = currentStage.getScheme().acquireSignals();
        // TODO:REQUEST SIGNALS
        // Specifically, we need to acquire the current sensitivity
        // also we need the current sensitivity level and force a context signal?
        // e.g. Context sensor needs to record the last context information


        Intent stageIntent = new Intent(MRAAC_STAGE_CHANGE);
        stageIntent.putExtra("stage", currentStage.getStageId());
        LocalBroadcastManager.getInstance(this).sendBroadcast(stageIntent);


        broadcastStageToClients(currentStage);
    }

    public abstract void initModel();


    public void setModel(MultiStageModel model) {
        this.model = model;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    public void broadcastStageToClients(Stage stage) {
        Intent intent = new Intent();
        intent.setAction(getPackageName());
        intent.putExtra("riskType", stage.getRiskType());
        intent.putExtra("authenticationLevel", stage.getAuthenticationLevel());
        intent.putExtra("sensitivityLevel", stage.getSensitivityLevel());
        // long timeAnchor = SystemClock.elapsedRealtimeNanos();
        // Log.i("Time Anchor", "stagesend:" + timeAnchor);
        sendBroadcast(intent);
        // Log.d(TAG, "Broadcast stage information to clients" + getPackageName() + "," + stage.getStageId());
    }


}
