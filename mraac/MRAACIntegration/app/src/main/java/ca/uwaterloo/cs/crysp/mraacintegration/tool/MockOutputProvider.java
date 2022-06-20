package ca.uwaterloo.cs.crysp.mraacintegration.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockOutputProvider<T> {
    List<T> outputs;
    int ptr;

    public MockOutputProvider(T... data) {
        outputs = new ArrayList<>();
        outputs.addAll(Arrays.asList(data));
        ptr = 0;
    }

    public MockOutputProvider() {
        outputs = new ArrayList<>();
        ptr = 0;
    }

    public void add(T value) {
        outputs.add(value);
    }

    public void addN(T value, int N) {
        for (int i = 0; i < N; ++i) {
            add(value);
        }
    }

    public T next() {
        T result = outputs.get(ptr);
        ptr += 1;
        if (ptr >= outputs.size()) {
            ptr = 0;
        }
        return result;
    }
}
