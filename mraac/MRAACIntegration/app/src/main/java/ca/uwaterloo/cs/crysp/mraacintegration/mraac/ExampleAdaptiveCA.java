package ca.uwaterloo.cs.crysp.mraacintegration.mraac;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals.EA_ACCEPT;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals.EA_REJECT;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals.IA_ACCEPT;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals.IA_REJECT;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_START;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_STOP;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.MultiStageModel;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.MultiStageModelBuilder;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.AccessOp;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.AdaptationPolicy;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.AdaptationScheme;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.AuthOp;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.RiskOp;

public class ExampleAdaptiveCA {
    /**
     * Example YAML FILE
     */

    public static final String RISK_GENERAL = "general";
    public static final String RISK_GUEST = "guest";
    public static final String AUTH_SIGNAL_GUEST = "AUTH_SIGNAL_GUEST";
    public static final String CONTEXT_SIGNAL_SHARING = "SHARING";

    public static final int maxAuth = 2;
    public static final int maxSens = 2;
    public static final String[] riskTypes = {
            RISK_GENERAL, RISK_GUEST
    };

    public static final String[] authSignals = {
            IA_ACCEPT, IA_REJECT, EA_ACCEPT, EA_REJECT, AUTH_SIGNAL_GUEST
    };
    public static final String[] contextSignals = {
            CONTEXT_SIGNAL_SHARING
    };

    public static final int[][] defaultAuthMatrix = {
            {-1, -1, 2, 0},
            {2, 0, -1, -1},
            {2, 1, -1, -1}
    };

    public static final String[][] riskTrans = {
            {RISK_GENERAL, CONTEXT_SIGNAL_SHARING, RISK_GUEST},
            {RISK_GUEST, CONTEXT_SIGNAL_SHARING, RISK_GUEST},
            {RISK_GUEST, IA_ACCEPT, RISK_GENERAL}

    };


    public static MultiStageModel buildModel() {
        Map<String, AuthOp> authOpMap = new HashMap<>();
        authOpMap.put(riskTypes[0], buildShareAuthOp0(authSignals));
        authOpMap.put(riskTypes[1], buildShareAuthOp1(authSignals));
        Map<String, AccessOp> accessOpMap = new HashMap<>();
        accessOpMap.put(riskTypes[0], buildAccessOp(2,2));
        accessOpMap.put(riskTypes[1], buildAccessOp(1,2));
        RiskOp riskOp = buildRiskOp(riskTrans);

        MultiStageModelBuilder builder = new MultiStageModelBuilder().setMaxAuthenticationLevel(maxAuth)
                .setMaxSensitivityLevel(maxSens).setRiskTypes(Arrays.asList(riskTypes))
                .setAuthOpMap(authOpMap).setAccessOpMap(accessOpMap).setRiskOp(riskOp);

        return builder.build();
    }


    public static AuthOp buildShareAuthOp0(String[] signals) {
        Map<Integer, Map<String, Integer>> tmp = new HashMap<>();
        for (int i = 0; i <= maxAuth; ++i) {
            Map<String, Integer> tmp2 = new HashMap<>();
            for (String signal : signals) {
                tmp2.put(signal, rawAuthFunc0(i, signal));
                System.out.print("==" + i + ":" + signal + ":" + tmp2.get(signal));
            }
            tmp.put(i, tmp2);
        }
        return new AuthOp(tmp);
    }

    public static AuthOp buildShareAuthOp1(String[] signals) {
        Map<Integer, Map<String, Integer>> tmp = new HashMap<>();
        for (int i = 0; i <= 1; ++i) {
            Map<String, Integer> tmp2 = new HashMap<>();
            for (String signal : signals) {
                tmp2.put(signal, rawAuthFunc1(i, signal));
            }
            tmp.put(i, tmp2);
        }
        return new AuthOp(tmp);
    }

    public static int rawAuthFunc0(int curAuth, String signal) {
        int result = 0;

        switch (signal) {
            case IA_ACCEPT:
                if (curAuth == 0) return -1;
                result = curAuth + 1;
                if (result > maxAuth) result = maxAuth;
                break;
            case IA_REJECT:
                if (curAuth == 0) return -1;
                result = curAuth - 1;
                if (result < 0) result = 0;
                break;
            case EA_REJECT:
                if (curAuth != 0) result = -1;
                break;
            case EA_ACCEPT:
                if (curAuth != 0) result = -1;
                else result = 2;
                break;
            case AUTH_SIGNAL_GUEST:
                if (curAuth != 0) result = 1;
                else result = -1;
                break;
            default:
                result = -1;
        }
        return result;
    }

    public static int rawAuthFunc1(int curAuth, String signal) {
        int result = 1;
        if (curAuth == 2) {
            return 0;
        }
        switch (signal) {
            case IA_ACCEPT:
            case IA_REJECT:
            case AUTH_SIGNAL_GUEST:
                break;
            default:
                result = -1;
        }
        return result;
    }


    public static RiskOp buildRiskOp(String[][] riskTrans) {
        Map<String, Map<String, String>> tmp = new HashMap<>();
        for (String[] riskTran : riskTrans) {
            Map<String, String> tmp2;
            if (tmp.containsKey(riskTran[0]) && tmp.get(riskTran[0]) != null) {
                tmp2 = tmp.get(riskTran[0]);
            } else {
                tmp2 = new HashMap<>();
            }
            assert tmp2 != null;
            tmp2.put(riskTran[1], riskTran[2]);
            tmp.put(riskTran[0], tmp2);
        }
        return new RiskOp(tmp);
    }


    public static AccessOp buildAccessOp(int maxAuth, int maxSens) {
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

//    public static AdaptationScheme exampleAdaptationScheme() {
//        AdaptationScheme scheme = new AdaptationScheme();
//        Adaptation adaptation = new Adaptation(AUTH_GAIT_ID, ADAPTATION_START);
//        Adaptation adaptation1 = new Adaptation(AUTH_GAIT_ID, ADAPTATION_STOP);
//        scheme.setDefaultAuthenticators(Arrays.asList(adaptation));
//        scheme.setAuthPolicies(Arrays.asList(new AdaptationPolicy(CONTEXT_ONFOOT_EXIT, Arrays.asList(adaptation1)),
//                new AdaptationPolicy(CONTEXT_ONFOOT_ENTER, Arrays.asList(adaptation))));
//        // scheme.setAuthPolicies(Arrays.asList(new AdaptationPolicy(CONTEXT_ONFOOT_ENTER, Arrays.asList(adaptation))));
//        return scheme;
//    }
//
//    public static AdaptationScheme emptyAdaptationScheme() {
//        AdaptationScheme scheme = new AdaptationScheme();
//        return scheme;
//    }