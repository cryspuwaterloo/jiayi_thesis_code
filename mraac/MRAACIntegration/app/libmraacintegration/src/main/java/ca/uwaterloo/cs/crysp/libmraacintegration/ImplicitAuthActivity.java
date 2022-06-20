package ca.uwaterloo.cs.crysp.libmraacintegration;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.AdaptationService;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.AuthenticationService;
import ca.uwaterloo.cs.crysp.libmraacintegration.clientia.ia.TouchFeatures;
import ca.uwaterloo.cs.crysp.libmraacintegration.clientia.ia.TrainingSet;

public class ImplicitAuthActivity extends  SecureActivity{
    private static final String BASE_TAG = "Itus";
    public static int state = 0;
    final int TRAINING = 0;
    final int TESTING = 1;
    long threshold = 30;
    public final int minTrain = 50;
    boolean verbose = true;

    TouchFeatures tf;
    TrainingSet ts;

    int fvSize;
    public static boolean sharing = false;



    // utility related variable
    // auto hidden view list
    private List<View> autoHiddenViews;
    private List<View> autoDisableViews;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize utility variables
        autoDisableViews = new ArrayList<>();
        autoHiddenViews = new ArrayList<>();

    }

    @Override
    public void postFailedAuthentication() {

    }

    private void updateMode(int mode) {
        state = mode;
        if (mode == TRAINING) {
            if (verbose)
                Log.d(BASE_TAG, "Trainingset size: " + fvSize + "\n" +
                        "Min trainingset size: " + minTrain);

        }
        else {
            Log.d(BASE_TAG, "TESTING");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        // fvSize = settings.getInt("fvSize", 0);
        verbose = settings.getBoolean("verbose", true);
        threshold = settings.getLong("threshold", 30);
        File file = this.getFileStreamPath("trainingSet");


        if (file != null && file.exists()) {
            Log.i(BASE_TAG, "File exists");
            FileInputStream fis = null;
            ObjectInputStream in = null;
            try {
                fis = this.openFileInput("trainingSet");
                in = new ObjectInputStream(fis);
                ts = (TrainingSet) in.readObject();
                in.close();
                Log.d(BASE_TAG, "Load training set");
            } catch (Exception ex) {
                Log.e(BASE_TAG, ex.getMessage());
            }
            fvSize = ts.fv.size();
        }
        else {
            ts = new TrainingSet();
            fvSize = 0;
        }
        if(tf == null)
            tf = new TouchFeatures();
        if(fvSize >= minTrain)
            state = TESTING;
        else
            state = TRAINING;

        updateMode(state);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean rv = tf.procEvent(ev);
        double dist;
        if (rv) {
            if (state == TRAINING) {
                ts.fv.add(tf.fv.getAll());
                fvSize++;
                if (verbose)
                    Log.d(BASE_TAG, "Trainingset size: " + fvSize + "\n" +
                            "Min trainingset size: " + minTrain);
                updatePreferences();
                if (fvSize == minTrain) {
                    state = TESTING;
                    this.updateMode(TESTING);
                    getScaledFeatures();
                }
            }
            else {
                dist =  getDistance(tf.fv.getAll());
                if (verbose)
                    Log.d(BASE_TAG, "Threshold: "+ String.valueOf(threshold)+ "\n" +
                            "Raw score: " + String.valueOf(dist));
                int result = 0;
                if (dist < threshold) {
                    Log.d(BASE_TAG, "Success");
                    result = 1;
                } else {
                    Log.d(BASE_TAG,"Failure: " + dist + " " + threshold);
                }

                passIAResult(result);
            }

        }
        //Log.d("MainActivity", String.valueOf(rv));
        //tv.setText(String.valueOf(rv));

        return super.dispatchTouchEvent(ev);
    }


    private double getDistance(double [] f) {
        double avgDist = 0;
        double minDist = Double.MAX_VALUE;
        double dist = 0;



        for (int i = 0; i < fvSize; i++ ) {
            double [] g = ts.fv.get(i);
            dist = 0;
            for (int j = 0; j < f.length; j++) {
                dist += Math.abs(f[j] / ts.fScale[j] - g[j] / ts.fScale[j]);
            }
            dist /= f.length;

            if (dist < minDist)
                minDist = dist;
            avgDist += dist;
        }


        avgDist /= fvSize;
        return Math.floor(minDist);
    }
    private void getScaledFeatures() {
        for (int i = 0; i < ts.fScale.length; i++ )
            ts.fScale[i] = 0;
        for (int i = 0; i < fvSize; i++ )
            for (int j = 0; j < ts.fScale.length; j++)
                ts.fScale[j] += ts.fv.get(i)[j];
        for (int i = 0; i < ts.fScale.length; i++ ) {
            ts.fScale[i]/=fvSize;
            //if (fScale[i] == 0)
            ts.fScale[i] = 1;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        updatePreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void updatePreferences() {
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("threshold", threshold);
        editor.putBoolean("verbose", verbose);
        if (fvSize > 0) {
            FileOutputStream fos = null;
            ObjectOutputStream out = null;
            try {
                fos = this.openFileOutput("trainingSet", Context.MODE_PRIVATE);
                out = new ObjectOutputStream(fos);
                out.writeObject(ts);
                out.close();
                fos.close();
                Log.d(BASE_TAG, "write training set: " + ts.fv.size());
            } catch (Exception ex) {
                Log.e("FOS", ex.getMessage());
            }
        }
        // Commit the edits!
        editor.commit();
        Log.d(BASE_TAG, "Update preferences");
    }

    public void passIAResult(double score) {
        Intent ia = new Intent(AuthenticationService.CLIENT_AUTH_ID);
        ia.putExtra("result", score);
        LocalBroadcastManager.getInstance(this).sendBroadcast(ia);
    }

}
