package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.exceptions.InvalidInputException;

public class AccessOp {
    /*
    Access control operator
    given authentication level and sensitivity level
    to determine whether to reject a user
     */
    private static final String TAG = "AccessOp";
    private Map<Integer, Map<Integer, Boolean>> table;


    public Map<Integer, Map<Integer, Boolean>> getTable() {
        return table;
    }

    public void setTable(Map<Integer, Map<Integer, Boolean>> table) {
        this.table = table;
    }

    public boolean checkAccess(int auth, int sens) {
        if (!table.containsKey(auth)) {
            throw new InvalidInputException();
        }
        if (!Objects.requireNonNull(table.get(auth)).containsKey(sens)) {
            Log.e(TAG, "not defined comparison, return false");
            return false;
        } else {
            return Objects.requireNonNull(table.get(auth)).get(sens);
        }
    }

    public boolean isSupported(int auth) {
        return table.containsKey(auth);
    }


    public AccessOp(Map<Integer, Map<Integer, Boolean>> t) {
        this.table = t;
    }


    public static AccessOp buildDefault(int maxAuth, int maxSens) {
        Map<Integer, Map<Integer, Boolean>> tmp = new HashMap<>();
        for (int i = 0; i <= maxAuth; ++i) {
            Map<Integer, Boolean> tmp2 = new HashMap<>();
            for (int j = 1; j <= maxSens; ++j) {
                if (i >= j) {
                    tmp2.put(j, true);
                } else {
                    tmp2.put(j, false);
                }
            }
            tmp.put(i, tmp2);
        }
        return new AccessOp(tmp);
    }
}
