package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;

public class AuthSignals {

    public static final String EA_ACCEPT = "EA_ACCEPT";
    public static final String EA_REJECT = "EA_REJECT";
    public static final String IA_ACCEPT = "IA_ACCEPT";
    public static final String IA_REJECT = "IA_REJECT";
    public static final String INACTIVITY = "INACTIVITY";
    public static final String SHARE_AUTH = "SHARE_AUTH";

    public static List<String> defaultSignals() {
        return new ArrayList<>(Arrays.asList( //EA_ACCEPT, EA_REJECT,
                IA_ACCEPT, IA_REJECT, INACTIVITY));
    }

    public static int defaultTransitions(String s, int cur, int max) {
        switch (s) {
            case "EA_ACCEPT":
                return max;
            case "EA_REJECT":
                return 0;
            case "IA_ACCEPT":
                if (cur == 0) return cur;
                if (cur >= max) return max;
                else return cur + 1;
            case "IA_REJECT":
                if (cur <= 0) return 0;
                else return cur - 1;
            case "INACTIVITY":
                if (cur == 1) return 1;
                else if (cur > 1) return cur - 1;
                else return 0;
            default:
                return cur;
        }
    }

    public static int exampleTransitions(String s, int cur, int max) {
        switch (s) {
            case "EA_ACCEPT":
                return max;
            case "EA_REJECT":
                return 0;
            case "IA_ACCEPT":
                if (cur == 0) return cur;
                if (cur >= max) return max;
                else return cur + 1;
            case "IA_REJECT":
                if (cur <= 0) return 0;
                else return 1;
            case "INACTIVITY":
                if (cur == 1) return 1;
                else if (cur > 1) return cur - 1;
                else return 0;
            default:
                return cur;
        }
    }

}
