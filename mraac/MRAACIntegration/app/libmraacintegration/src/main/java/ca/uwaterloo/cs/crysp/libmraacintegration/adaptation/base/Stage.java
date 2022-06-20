package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base;

public class Stage {
    /*
    Stage Class: represent a risk type and level and maintain a list of adaptation policy
     */
    public static final String LOCKED_STAGE_NAME = "locked";
    private static final String TAG = "Stage";
    private String riskType;
    private int authenticationLevel;
    private int sensitivityLevel;
    private AdaptationScheme scheme;
    private final boolean isLockedStage;


    public Stage(String riskType, int authenticationLevel, int sensitivityLevel) {
        this.riskType = riskType;
        this.authenticationLevel = authenticationLevel;
        this.sensitivityLevel = sensitivityLevel;
        this.scheme = new AdaptationScheme();
        this.isLockedStage = false;
    }

    public Stage() {
        this.riskType = LOCKED_STAGE_NAME;
        this.authenticationLevel = 0;
        this.sensitivityLevel = 0;
        this.scheme = new AdaptationScheme();
        this.isLockedStage = true;
    }

    public boolean isLockedStage() {
        return isLockedStage;
    }

    public void setAuthenticationLevel(int authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }

    public void setRiskType(String riskType) {
        this.riskType = riskType;
    }

    public void setSensitivityLevel(int sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public void setScheme(AdaptationScheme scheme) {
        this.scheme = scheme;
    }

    public int getAuthenticationLevel() {
        return authenticationLevel;
    }

    public int getSensitivityLevel() {
        return sensitivityLevel;
    }

    public String getRiskType() {
        return riskType;
    }

    public AdaptationScheme getScheme() {
        return scheme;
    }

    public void addAdaptationItem(String signal, Adaptation adaptation) {
        this.scheme.appendPolicy(signal, adaptation);
    }

    public String getStageId() {
        if (isLockedStage){
            return LOCKED_STAGE_NAME;
        } else {
            return riskType + "-" + authenticationLevel + "-" + sensitivityLevel;
        }
    }

    public static String constructStageId(String risk, int auth, int sens) {
        return risk + "-" + auth + "-" + sens;
    }
}
