package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.exceptions.InvalidInputException;

public class RiskOp {
    /*
    R receive Signal to R
     */
    private Map<String, Map<String, String>> riskTrans;
    private Set<String> signals;

    public Map<String, Map<String, String>> getRiskTrans() {
        return riskTrans;
    }

    public RiskOp(Map<String, Map<String, String>> m) {
        riskTrans = m;
        signals = getSupportedSignals();
    }

    public RiskOp() {
        riskTrans = new HashMap<>();
        signals = new HashSet<>();
    }

    private Set<String> getSupportedSignals() {
        Set<String> result = new HashSet<>();
        for(Map<String, String> m: riskTrans.values()) {
            result.addAll(m.keySet());
        }
        return result;
    }

    public void addRiskTrans(String currentRisk, String signal, String nextRisk) {
        if (!riskTrans.containsKey(currentRisk)){
            Map<String, String> item = new HashMap<>();
            item.put(signal, nextRisk);
            riskTrans.put(currentRisk, item);
        } else {
            riskTrans.get(currentRisk).put(signal, nextRisk);
        }
        signals = getSupportedSignals();
    }

    public void setRiskTrans(Map<String, Map<String, String>> riskTrans) {
        this.riskTrans = riskTrans;
        signals = getSupportedSignals();
    }

    public String makeRiskTransition(String riskType, Signal signal) {
        if (!riskTrans.containsKey(riskType)) {
            throw new InvalidInputException();
        }
        if (!Objects.requireNonNull(riskTrans.get(riskType)).containsKey(signal.getName())) {
            return null;
        } else {
            return Objects.requireNonNull(riskTrans.get(riskType)).get(signal.getName());
        }
    }

    public Set<String> getSignals() {
        return signals;
    }
}
