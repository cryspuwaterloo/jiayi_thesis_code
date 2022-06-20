package ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts;

import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.CONTEXT_ONSITE_ID;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.location.LocationRequest;

import java.util.List;

import ca.uwaterloo.cs.crysp.libmraacintegration.context.ContextServiceBinder;
import ca.uwaterloo.cs.crysp.mraacintegration.tool.MockOutputProvider;

public class OnsiteContextService extends Service implements ContextServiceBinder {

    /**
     * It consists of location sensors and network provider
     * Output: 0 not in the company, 1 in the company
     */

    private static final String TAG = "OnsiteContextService";
    private static final boolean USE_MOCK_RESULT =true;
    private static final int DETECTION_INTERVAL = 15000;

    private FusedLocationProviderClient fusedLocationClient;
    private boolean requestingLocationUpdates = true;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private double[][] companyLocation= new double[][]{{43.472, -80.544}, {43.475,-80.54}};

    MockOutputProvider<Integer> mockOutputProvider;


    @Override
    public void onCreate() {
        super.onCreate();

        // for experiments only
        mockOutputProvider = new MockOutputProvider<>();
        mockOutputProvider.addN(1, 1);
        mockOutputProvider.addN(0, 1);

        Log.i(TAG, "service created");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                    // Update UI with location data
                // Log.i("Location", "Now we are at " + location.getLatitude() + "," + location.getLongitude());

                if (USE_MOCK_RESULT) {
                    sendResult(OnsiteContextService.this, CONTEXT_ONSITE_ID, mockOutputProvider.next());
                } else {
                    if (location.getLatitude() < companyLocation[1][0] && location.getLatitude() > companyLocation[0][0]
                            && location.getLongitude() < companyLocation[1][1] && location.getLongitude() > companyLocation[0][1]) {
                        sendResult(OnsiteContextService.this, CONTEXT_ONSITE_ID, 1);
                    } else {
                        sendResult(OnsiteContextService.this, CONTEXT_ONSITE_ID, 0);
                    }
                }

            }
        };
        locationRequest = LocationRequest.create().
                setFastestInterval(DETECTION_INTERVAL).setInterval(DETECTION_INTERVAL).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (requestingLocationUpdates) {
            Log.i(TAG, "start sensing the location");
            startLocationUpdates();
        }

    }


    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}