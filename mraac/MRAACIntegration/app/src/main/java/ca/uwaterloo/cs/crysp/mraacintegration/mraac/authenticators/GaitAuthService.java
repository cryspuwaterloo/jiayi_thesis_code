package ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators;

import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.AUTH_GAIT_ID;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.psambit9791.jdsp.signal.peaks.FindPeak;
import com.github.psambit9791.jdsp.signal.peaks.Peak;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ca.uwaterloo.cs.crysp.mraacintegration.tool.FakeSensorDataProvider;

public class GaitAuthService extends Service implements SensorEventListener {
    // TODO: DOWNLOAD MODEL AND EXPERIMENT DATA REMOTELY

    private static final String TAG = "GaitAuthService";
    private static final boolean USE_FAKE_DATA = true;

    private static final int SAMPLING_RATE = 50;
    private static final int S_TO_US = 1000000;
    private static final int BUFFER_SIZE = 2000;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private GaitData buffer;

    // for test and evaluation only
    private FakeSensorDataProvider fakeAccProvider;
    private FakeSensorDataProvider fakeGyroProvider;

    private Interpreter interpreter;


    public GaitAuthService() {
        buffer = new GaitData(BUFFER_SIZE);

    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            File f = new File(getFilesDir(), "model.tflite");
            if (f.exists()) {
                interpreter = new Interpreter(f);
            } else {
                InputStream ist = getAssets().open("model561993.tflite");
                interpreter = new Interpreter(Objects.requireNonNull(createFileFromInputStream(ist)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (USE_FAKE_DATA) {
            try {
                InputStream is1 = getAssets().open("acc2.csv");
                fakeAccProvider = FakeSensorDataProvider.fromInputStream(is1);
                InputStream is2 = getAssets().open("gyro2.csv");
                fakeGyroProvider = FakeSensorDataProvider.fromInputStream(is2);
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        int interval = S_TO_US / SAMPLING_RATE;
        mSensorManager.registerListener(this, mAccelerometer, interval);
        mSensorManager.registerListener(this, mGyroscope, interval);

        Log.i(TAG, "I start to work!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        long timeAnchor = SystemClock.elapsedRealtimeNanos();
        Log.i("Time Anchor", "contextonfootadapt:" + timeAnchor);
        mSensorManager.unregisterListener(this); // , mAccelerometer);
        interpreter.close();
        Log.i(TAG, "So long!");

        // mSensorManager.unregisterListener(this, mGyroscope);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch(sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                if (USE_FAKE_DATA) {
                    float[] tmp = fakeAccProvider.getNext();
                    buffer.addAcc(tmp[0], tmp[1], tmp[2]);
                } else {
                    buffer.addAcc(sensorEvent.values[0],
                            sensorEvent.values[1],
                            sensorEvent.values[2]);
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (USE_FAKE_DATA) {
                    float[] tmp = fakeGyroProvider.getNext();
                    buffer.addGyro(tmp[0], tmp[1], tmp[2]);
                } else {
                    buffer.addGyro(sensorEvent.values[0],
                            sensorEvent.values[1],
                            sensorEvent.values[2]);
                }
                break;
        }

        if (buffer.isReady()) {
            double[][] data = buffer.popAll();
            double[] mag = new double[BUFFER_SIZE];
            Log.i(TAG, data.length + "," + data[0].length);

            // get mag
            for(int i = 0; i < BUFFER_SIZE; ++i) {
                mag[i] = Math.sqrt(data[0][i] * data[0][i]
                        + data[1][i] * data[1][i]
                        + data[2][i] * data[2][i]);
            }

            // find peaks (checked)
            FindPeak fp = new FindPeak(mag);
            Peak out = fp.detectPeaks();
            int[] result = out.filterByPeakDistance(25);
            List<Integer> resultIndex = new ArrayList<>();
            if (result.length > 0) {
                double[] heights = out.findPeakHeights(result);
                for (int i = 0; i < heights.length; ++i) {
                    if (heights[i] > 10) {
                        resultIndex.add(result[i]);
                    }
                }
            }

            // intepolation (checked)
            List<double[][]> finalResult = new ArrayList<>();
            if (resultIndex.size() > 3) {
                Log.i(TAG, resultIndex.toString());
                for (int i = 0; i < resultIndex.size() - 3; ++i) {
                    int startId = resultIndex.get(i);
                    int endId = resultIndex.get(i + 2);
                    double[] xs = new double[endId - startId];
                    for(int j = 0; j < xs.length; ++j) {
                        xs[j] = 128.0 / xs.length * j;
                    }
                    xs[xs.length - 1] = 127.0;

                    LinearInterpolator li = new LinearInterpolator();
                    double[][] tmp = new double[6][128];
                    for(int j = 0; j < 6; ++j){
                        PolynomialSplineFunction func = li.interpolate(xs, Arrays.copyOfRange(data[j], startId, endId));
                        for(int k = 0; k < 128; ++k) {
                            tmp[j][k] = func.value(k);
                        }

                    }
                    finalResult.add(tmp);
                }
                // Log.i(TAG,  Arrays.deepToString(finalResult.get(0)));
            } else {
                return;
            }


            // classification
            // Creates inputs for reference.
//            ByteBuffer buffer = ByteBuffer.allocate(128 * 6 * 4);
//            buffer.order(ByteOrder.nativeOrder());
//            // buffer.asFloatBuffer();
//
//            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 128, 6}, DataType.FLOAT32);
            List<Float> scores = new ArrayList<>();
            for (int i = 0; i < finalResult.size(); ++i) {
                double[][] currData = finalResult.get(i);
                float[][] conv = new float[currData[0].length][currData.length];
                for (int j = 0; j < currData[0].length; ++j) {
                    for (int k = 0; k < currData.length; ++k) {
                        conv[j][k] = (float) currData[k][j];
                    }
                }
                float[][] answer = new float[][]{new float[2]};
                interpreter.run(new float[][][]{conv}, answer);
                scores.add(answer[0][1]);

//                buffer.rewind();
//                double[][] currData = finalResult.get(i);
//                for (int j = 0; j < currData[0].length; ++j) {
//                    for (int k = 0; k < currData.length; ++k) {
//                        buffer.putFloat((float) currData[k][j]);
//                    }
//                }
//                buffer.rewind();
//                inputFeature0.loadBuffer(buffer);
//                Model1.Outputs outputs = model.process(inputFeature0);
//                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

//                float[] tmp = outputFeature0.getFloatArray();
//                scores.add(tmp[1]);
            }
            // Log.i(TAG, "original scores: " + scores.toString());

            float sum = 0;
            for(float s: scores) {
                sum += s;
            }
            sendResult(sum / scores.size());
        }

    }

    public void sendResult(double result) {
        Log.i(TAG, AUTH_GAIT_ID + " authentcation score: " + result);
        Intent intent = new Intent(AUTH_GAIT_ID);
        intent.putExtra("result", result);
        long timeAnchor = SystemClock.elapsedRealtimeNanos();
        Log.i("Time Anchor", "authissue:" + timeAnchor);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private File createFileFromInputStream(InputStream inputStream) {

        try{
            File f = new File(getFilesDir(), "model.tflite");
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        }catch (IOException e) {
            //Logging exception
        }

        return null;
    }
}
