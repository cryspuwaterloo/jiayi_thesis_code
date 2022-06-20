package ca.uwaterloo.cs.crysp.mraacintegration.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FakeSensorDataProvider {

    float [][] data;
    private int ptr;

    public FakeSensorDataProvider(float[][] data) {
        this.data = data;
        ptr = 0;
    }

    public float[] getNext() {
        float[] result = data[ptr];
        ptr ++;
        if (ptr >= data.length) ptr = 0;
        return result;
    }

    public int getPtr() {
        return ptr;
    }

    public static FakeSensorDataProvider fromInputStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<float[]> result = new ArrayList<>();
        if (is != null) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] items = line.split(",");
                float[] tmp = new float[items.length];
                for (int i = 0; i < items.length; ++i) {
                    tmp[i] = Float.parseFloat(items[i]);
                }
                result.add(tmp);
            }
        }
        float[][] res = result.toArray(new float[result.size()][]);
        return new FakeSensorDataProvider(res);
    }
}
