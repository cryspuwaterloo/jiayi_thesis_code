package ca.uwaterloo.cs.crysp.libmraacintegration.auth;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SimpleSlidingWindow {
    public static final int INSUFFICIENT_DATA = -1;
    public static final int INSIGNIFICANT_DATA  = -2;
    int m;
    int n;
    Deque<Integer> windows;
    Map<Integer, Integer> counter;

    public SimpleSlidingWindow(int m, int n) {
        this.m = m;
        this.n = n;
        this.windows = new LinkedList<>();
        this.counter = new HashMap<>();
    }

    public int getQualifiedMajority() {
        int max = m;
        int result = INSUFFICIENT_DATA;
        for(int key: counter.keySet()) {
            if (counter.get(key) >= max) {
                max = counter.get(key);
                result = key;
            }
        }
        return result;
    }

    public void reset() {
        this.windows.clear();
        this.counter.clear();
    }


    public void add(int v) {
        windows.addLast(v);
        if(counter.containsKey(v) && counter.get(v) != null) {
            counter.put(v, counter.get(v) + 1);
        } else{
            counter.put(v, 1);
        }

        if(windows.size() > n) {
            int out = windows.pollFirst();
            counter.put(out, counter.get(out) - 1);
        }
    }

    public void resize(int m, int n) {
        if (n > this.n) {
            this.m = m;
            this.n = n;
        } else {
            while(windows.size() > n) {
                int out = windows.pollFirst();
                counter.put(out, counter.get(out) - 1);
            }
            this.n = n;
            this.m = m;
        }
    }

    public int getM() {
        return m;
    }

    public int getN() {
        return n;
    }


}
