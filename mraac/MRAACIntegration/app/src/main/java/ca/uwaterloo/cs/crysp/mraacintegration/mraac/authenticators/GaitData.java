package ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators;

public class GaitData {
    public double[] ax, ay, az;
    public double[] gx, gy, gz;

    public int a_ptr;
    public int g_ptr;
    private final int size;

    public GaitData(int bufferSize) {
        size = bufferSize;
        ax = new double[bufferSize];
        ay = new double[bufferSize];
        az = new double[bufferSize];
        gx = new double[bufferSize];
        gy = new double[bufferSize];
        gz = new double[bufferSize];
        a_ptr = 0;
        g_ptr = 0;
    }


    public void addAcc(float x, float y, float z) {
        if (a_ptr < size) {
            ax[a_ptr] = x;
            ay[a_ptr] = y;
            az[a_ptr] = z;
            a_ptr++;
        }
    }

    public void addGyro(float x, float y, float z) {
        if (g_ptr < size) {
            gx[g_ptr] = x;
            gy[g_ptr] = y;
            gz[g_ptr] = z;
            g_ptr++;
        }
    }

    public boolean isReady() {
        return a_ptr >= size && g_ptr >= size;
    }

    public double[][] popAll() {
        double[][] result = new double[6][size];
        result[0] = ax;
        result[1] = ay;
        result[2] = az;
        result[3] = gx;
        result[4] = gy;
        result[5] = gz;

        a_ptr = 0;
        g_ptr = 0;
        return result;
    }

}
