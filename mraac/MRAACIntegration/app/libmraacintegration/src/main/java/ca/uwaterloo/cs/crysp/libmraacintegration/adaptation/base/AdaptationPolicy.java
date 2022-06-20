package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base;

import java.util.ArrayList;
import java.util.List;

public class AdaptationPolicy {
    private String triggerSignal;
    private List<Adaptation> adaptations;

    public AdaptationPolicy(String trigger, List<Adaptation> adaptations) {
        this.triggerSignal = trigger;
        this.adaptations = adaptations;
    }

    public AdaptationPolicy(String trigger, Adaptation adaptation) {
        this.triggerSignal = trigger;
        this.adaptations = new ArrayList<>();
        this.adaptations.add(adaptation);
    }

    public void setAdaptations(List<Adaptation> adaptations) {
        this.adaptations = adaptations;
    }

    public void setTriggerSignal(String triggerSignal) {
        this.triggerSignal = triggerSignal;
    }

    public List<Adaptation> getAdaptations() {
        return adaptations;
    }

    public String getTriggerSignal() {
        return triggerSignal;
    }

    public void addAdaptation(Adaptation adaptation) {
        this.adaptations.add(adaptation);
    }
}
