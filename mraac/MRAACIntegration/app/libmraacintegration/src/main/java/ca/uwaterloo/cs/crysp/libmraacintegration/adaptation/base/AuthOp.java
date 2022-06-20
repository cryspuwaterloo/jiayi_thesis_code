package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.exceptions.InvalidInputException;

public class AuthOp {


    private static final String TAG = "AuthOp";
    private Map<Integer, Map<String, Integer>> table;
    private Set<String> signals;

    public AuthOp(Map<Integer,Map<String, Integer>> table) {
        this.table = table;
        this.signals = getSupportedSignals();
    }

    public void setTable(Map<Integer, Map<String, Integer>> table) {
        this.table = table;
        this.signals = getSupportedSignals();
    }

    public Map<Integer, Map<String, Integer>> getTable() {
        return table;
    }

    private Set<String> getSupportedSignals() {
        Set<String> result = new HashSet<>();
        for(Map<String, Integer> item: table.values()) {
            result.addAll(item.keySet());
        }
        return result;
    }

    public Set<String> getSignals() {
        return signals;
    }

    public int makeAuthTransition(int current, Signal input) {
        return makeAuthTransition(current, input.getName());
    }

    public int makeAuthTransition(int current, String input) {
        if (!table.containsKey(current)) {
            throw new InvalidInputException();
        }

        if (!Objects.requireNonNull(table.get(current)).containsKey(input)) {
            Log.e(TAG, "Undefined Auth Transition");
            return current;
        }

        return table.get(current).get(input);
    }

    public static AuthOp buildDefault(int maxAuth) {

        Map<Integer, Map<String, Integer>> tmp = new HashMap<>();
        for (int i = 0; i <= maxAuth; ++i) {
            Map<String, Integer> tmp2 = new HashMap<>();
            for (String s: AuthSignals.defaultSignals()) {
               tmp2.put(s, AuthSignals.defaultTransitions(s, i, maxAuth));
            }
            tmp.put(i, tmp2);
        }
        return new AuthOp(tmp);
    }


}
