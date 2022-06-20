package ca.uwaterloo.cs.crysp.mraacintegration;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.DEBUG_FORCE_NEGATIVE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.DEBUG_FORCE_POSITIVE;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ca.uwaterloo.cs.crysp.libmraacintegration.SecureActivity;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.BYODAccessService;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.BYODAdaptationService;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.BYODAuthenticationService;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.BYODContextService;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts.DummyConditionService;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts.DummyContextService;

public class MainActivity extends SecureActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityResultLauncher<String[]> locationPermissionRequest =
                    registerForActivityResult(new ActivityResultContracts
                                    .RequestMultiplePermissions(), result -> {
                                Boolean fineLocationGranted = result.get(
                                        Manifest.permission.ACCESS_FINE_LOCATION);
                                Boolean coarseLocationGranted = result.get(
                                        Manifest.permission.ACCESS_COARSE_LOCATION);
                                if (fineLocationGranted != null && fineLocationGranted) {
                                    // Precise location access granted.
                                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                    // Only approximate location access granted.
                                } else {
                                    // No location access granted.
                                }
                            }
                    );

// ...

            // Before you perform the actual permission request, check whether your app
            // already has the permissions, and whether your app needs to show a permission
            // rationale dialog. For more details, see Request permissions.
            locationPermissionRequest.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

//        MultiStageModel model = ExampleBYOD.buildModel();
//        System.out.print(model.toCSV());
//        Timer timer;
//        timer = new Timer();
//        TimerTask task;
//        task = new TimerTask() {
//            @Override
//            public void run() {
//                Log.i("TAG", "I want to dosome crazy");
//                Intent ii = new Intent(MainActivity.this, BYODContextService.class);
//                ii.setAction(DEBUG_FORCE_NEGATIVE);
//                startService(ii);
//            }
//        };
//        timer.scheduleAtFixedRate(task, 1000, 1000);

    }


    @Override
    protected void onResume() {
        super.onResume();

        startService(new Intent(this, BYODAccessService.class));
        startService(new Intent(this, BYODContextService.class));
        startService(new Intent(this, BYODAuthenticationService.class));
        startService(new Intent(this, BYODAdaptationService.class));

    }

    @Override
    public void postFailedAuthentication() {
        // you need to implement something after the user fails to pass the authentication
        this.finish();
    }


}