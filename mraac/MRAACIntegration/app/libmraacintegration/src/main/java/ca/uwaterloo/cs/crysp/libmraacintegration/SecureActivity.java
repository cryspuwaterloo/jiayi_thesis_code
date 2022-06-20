package ca.uwaterloo.cs.crysp.libmraacintegration;



import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.MRAAC_STAGE_CHANGE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.SERVICE_AUTHENTICATION;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.concurrent.Executor;

import ca.uwaterloo.cs.crysp.libmraacintegration.access.AccessControlService;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Stage;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals;

public abstract class SecureActivity extends AppCompatActivity {

    // extends SecureActivity to trigger Keyguard at the locked stage
    private static final String TAG = "SecureActivity";

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private KeyguardManager km;
    private final BroadcastReceiver stageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Log.i(TAG, intent.getAction());
            if (intent.getStringExtra("stage").equals(Stage.LOCKED_STAGE_NAME)) {
                biometricPrompt.authenticate(promptInfo);
                // Log.d("MRAAC_EXP", "EA");
                // assume the user unlocked successfully
                // Signal s = new Signal(AuthSignals.EA_ACCEPT, TAG);
                // LocalBroadcastManager.getInstance(SecureActivity.this).sendBroadcast(s.toIntent(SERVICE_AUTHENTICATION));
            }
        }
    };
    private boolean registered=false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        executor = ContextCompat.getMainExecutor(this);
        km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        biometricPrompt = new BiometricPrompt(SecureActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
                postFailedAuthentication();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                Signal s = new Signal(AuthSignals.EA_ACCEPT, TAG);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(s.toIntent(SERVICE_AUTHENTICATION));
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();
                postFailedAuthentication();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authentication for proceeding")
                .setDeviceCredentialAllowed(true)
                .build();

    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(stageReceiver, new IntentFilter(MRAAC_STAGE_CHANGE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(stageReceiver);

        }catch (Exception e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    public abstract void postFailedAuthentication();

    public void reportSensitivity(int sensitivity, Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.setAction(AccessControlService.REPORT_SENSITIVITY_LEVEL);
        intent.putExtra("level", sensitivity);
        startService(intent);
    }
}
