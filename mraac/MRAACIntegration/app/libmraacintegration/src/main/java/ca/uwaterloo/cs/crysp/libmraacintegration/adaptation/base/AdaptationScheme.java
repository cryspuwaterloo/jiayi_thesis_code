package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_START;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_TUNE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdaptationScheme {
    /*
    General authentication schemes
     */
    private List<Adaptation> defaultAuthenticators; // imply the authenticators that should be automatically triggered when

    private List<AdaptationPolicy> authPolicies;

    /*
    Note: the adaptation part for access control is left for the access control module
     */
    private String accessScheme;
    // we only keep the scheme name in the adaptation scheme
    // context aware access control is implemented in
    // define some resources that can be only accessed when context satisfied
    // Access control policies are only triggered when the current app changes
    // Enforcement sequence: current app changes --> high-level control --> acquire context information --> determine availability

    public AdaptationScheme(List<Adaptation> defaultAuthenticators, List<AdaptationPolicy> authPolicies, String accessScheme) {
        this.defaultAuthenticators = defaultAuthenticators;
        this.authPolicies = authPolicies;
        this.accessScheme = accessScheme;
    }

    public AdaptationScheme() {
        this.defaultAuthenticators = new ArrayList<>();
        this.authPolicies = new ArrayList<>();
        this.accessScheme = null;
    }

    public void appendPolicy(String signal, Adaptation adaptation) {
        for (AdaptationPolicy ap: authPolicies) {
            if(ap.getTriggerSignal().equals(signal)) {
                ap.addAdaptation(adaptation);
                return;
            }
        }
        authPolicies.add(new AdaptationPolicy(signal, adaptation));
    }

    public List<Adaptation> acquireAdaptationsBySignal(Signal signal) {
        List<Adaptation> adaptations = new ArrayList<>();
        for (AdaptationPolicy ap: authPolicies) {
            if (ap.getTriggerSignal().equals(signal.getName())) {
                adaptations.addAll(ap.getAdaptations());
            }
        }
        return adaptations;
    }

    public List<String> acquireSignals() {
        Set<String> signals = new HashSet<>();
        for (AdaptationPolicy ap: authPolicies) {
            signals.add(ap.getTriggerSignal());
        }
        return new ArrayList<>(signals);
    }


    public List<AdaptationPolicy> getAuthPolicies() {
        return authPolicies;
    }

    public List<Adaptation> getDefaultAuthenticators() {
        return defaultAuthenticators;
    }

    public String getAccessScheme() {
        return accessScheme;
    }

    public void setAccessScheme(String accessScheme) {
        this.accessScheme = accessScheme;
    }

    public void setAuthPolicies(List<AdaptationPolicy> authPolicies) {
        this.authPolicies = authPolicies;
    }

    public void setDefaultAuthenticators(List<Adaptation> defaultAuthenticators) {
        this.defaultAuthenticators = defaultAuthenticators;
    }


    public Set<String> getAllRelatedAuthenticators() {
        Set<String> result = new HashSet<>();
        for (Adaptation ad : defaultAuthenticators) {
            if(ad.getAdaptation().equals(ADAPTATION_START) || ad.getAdaptation().equals(ADAPTATION_TUNE)) {
                result.add(ad.getTargetId());
            }
        }
        return result;
    }

}
