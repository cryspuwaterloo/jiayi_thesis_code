package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.MultiStageModel.SIGNAL_TYPE_AUTH;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.MultiStageModel.SIGNAL_TYPE_RISK;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.MultiStageModel.SIGNAL_TYPE_SENS;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ACCESS_SIGNAL_HEADER;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.AccessOp;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.AuthOp;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.RiskOp;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Stage;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.exceptions.InvalidInputException;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.exceptions.StageNotFoundException;


/*
MultiStageModelBuilder

Step1: configure risktypes, maxAuthLevel, maxSensLevel
Step2: determine AuthOp, AccessOp, and RiskOp
Step3: generate all valid stages
Step4: build connections between stages
 */


public class MultiStageModelBuilder {

    private final static String TAG = "MultiStageModelBuilder";

    private List<Stage> stages;
    // INPUT SIGNALS: [SIGNAL_TYPE: [SIGNALS]]
    private Map<String, List<String>> inputSignals;
    private Stage initStage=null;

    // for building multistage model
    private List<String> riskTypes = new ArrayList<>(Collections.singletonList("normal"));
    private int maxAuthenticationLevel=3;
    private int maxSensitivityLevel=2;
    private Map<String, AuthOp> authOpMap=null;
    private Map<String, AccessOp> accessOpMap=null;
    private RiskOp riskOp=null;

    public MultiStageModelBuilder() {

    }

    public MultiStageModelBuilder setAccessOpMap(Map<String, AccessOp> accessOpMap) {
        this.accessOpMap = accessOpMap;
        return this;
    }

    public MultiStageModelBuilder setAccessOpForRisk(String risk, AccessOp op) {
        if (!riskTypes.contains(risk)) {
            throw new InvalidInputException();
        }

        accessOpMap.put(risk, op);
        return this;
    }

    public Map<String, AccessOp> genDefaultAccessOpMap() {
        Map<String, AccessOp> tmp = new HashMap<>();
        for (String risk: riskTypes) {
            tmp.put(risk, AccessOp.buildDefault(this.maxAuthenticationLevel,
                    this.maxSensitivityLevel));
        }
        return tmp;
    }


    public MultiStageModelBuilder setAuthOpMap(Map<String, AuthOp> authOpMap) {
        this.authOpMap = authOpMap;
        return this;
    }

    public MultiStageModelBuilder setAuthOpForRisk(String risk, AuthOp op) {
        if (!riskTypes.contains(risk)) {
            throw new InvalidInputException();
        }

        authOpMap.put(risk, op);
        return this;
    }


    public Map<String, AuthOp> genDefaultAuthOpMap() {
        Map<String, AuthOp> tmp = new HashMap<>();
        for (String risk: riskTypes) {
            tmp.put(risk, AuthOp.buildDefault(this.maxAuthenticationLevel));
        }
        return tmp;
    }


    public MultiStageModelBuilder setMaxAuthenticationLevel(int maxAuthenticationLevel) {
        this.maxAuthenticationLevel = maxAuthenticationLevel;
        return this;
    }

    public MultiStageModelBuilder setMaxSensitivityLevel(int maxSensitivityLevel) {
        this.maxSensitivityLevel = maxSensitivityLevel;
        return this;
    }

    public MultiStageModelBuilder setRiskOp(RiskOp riskOp) {
        this.riskOp = riskOp;
        return this;
    }

    public MultiStageModelBuilder setRiskTypes(List<String> riskTypes) {
        this.riskTypes = riskTypes;
        return this;
    }

    public MultiStageModelBuilder setStages(List<Stage> stages) {
        this.stages = stages;
        return this;
    }

    public void setInitStage(Stage initStage) {
        this.initStage = initStage;
    }

    public MultiStageModel build() {
        // inspect
        if (authOpMap == null) {
            authOpMap = genDefaultAuthOpMap();
        }

        if (accessOpMap == null) {
            accessOpMap = genDefaultAccessOpMap();

        }

        if (riskOp == null) {
            riskOp = new RiskOp();
        }

        // initialize stage
        stages = new ArrayList<>();
        stages.add(new Stage());


        // get all valid stages
        for (String risk: riskTypes) {
            for (int au = 1; au <= maxAuthenticationLevel; au++) {
                for (int se = 1; se <= maxSensitivityLevel; se++) {
                    // AuthOp tmpAuthOp = authOpMap.get(risk);
                    AccessOp tmpAccessOp = accessOpMap.get(risk);
                    if (tmpAccessOp == null) {
                        throw new NullPointerException();
                    }

                    if (!tmpAccessOp.isSupported(au)) continue;

                    if (tmpAccessOp.checkAccess(au, se)) {
                        Stage st = new Stage(risk, au, se);
                        stages.add(st);
                        Log.i(TAG, "Add Stage:" + st.getStageId());
                    }
                }
            }
        }

        // fetch all signals and construct edges
        // SIGNAL_AUTH

        initInputSignals();
        Map<String, Stage> stageMap = this.createStageMap(); // for reference
        Map<String, Map<String, Stage>> transitionMap = initTransitionMap(stages);

        if (initStage == null || initStage.getStageId().equals(Stage.LOCKED_STAGE_NAME)) {
            // note initial stage should not be the locked stage
            initStage = stageMap.get(Stage.constructStageId(riskTypes.get(0), maxAuthenticationLevel, 1));
            if (initStage == null) {
                Log.e(TAG, "You have to mannually set the initial Stage.");
                throw new StageNotFoundException();
            }
        }


        Set<String> authSignalSet = new HashSet<>();
        for(AuthOp auop: authOpMap.values()) {
            authSignalSet.addAll(auop.getSignals());
        }
        inputSignals.get(SIGNAL_TYPE_AUTH).addAll(authSignalSet);

        for(String signal: inputSignals.get(SIGNAL_TYPE_AUTH)) {
            for(Stage stage: stages) {

                Map<String, Stage> submap = transitionMap.get(stage.getStageId());

                if (stage.isLockedStage()) {
                    // special for locked stage
                    if(signal.equals(AuthSignals.EA_ACCEPT))
                        submap.put(signal, initStage);
                    if(signal.equals(AuthSignals.EA_REJECT))
                        submap.put(signal, stages.get(0));
                    continue;
                }

                AuthOp tmpAuth = authOpMap.get(stage.getRiskType());
                int res = tmpAuth.makeAuthTransition(stage.getAuthenticationLevel(), signal);

                //TODO: PROCESS UNDEFINED TRANSITIONS
                if (res == -1) {
                    // undefined benign signals

                } else if (res == -2) {
                    // undefined dangerous signals

                }


                String nextStageName = Stage.constructStageId(stage.getRiskType(),
                        res, stage.getSensitivityLevel());
                if (stageMap.containsKey(nextStageName)) {
                    submap.put(signal, stageMap.get(nextStageName));
                }
                else if (res >= 0){
                    submap.put(signal, stageMap.get(Stage.LOCKED_STAGE_NAME));
                }
            }
        }


        List<String> accessSet = inputSignals.get(SIGNAL_TYPE_SENS);
        for(int i=1; i <= maxSensitivityLevel; ++i) {
            String signalName = ACCESS_SIGNAL_HEADER + i;
            accessSet.add(signalName);
            for(Stage stage: stages) {
                if (stage.isLockedStage()) continue; // you can get nothing when locked.

                Map<String, Stage> submap = transitionMap.get(stage.getStageId());
                AccessOp tmpAcc = accessOpMap.get(stage.getRiskType());
                boolean res = tmpAcc.checkAccess(stage.getAuthenticationLevel(), i);
                if (res) {
                    String nextStageName = Stage.constructStageId(stage.getRiskType(),
                            stage.getAuthenticationLevel(), i);
                    if (!stageMap.containsKey(nextStageName))
                        throw new StageNotFoundException();
                    submap.put(signalName, stageMap.get(nextStageName));
                } else {
                    submap.put(signalName, stageMap.get(Stage.LOCKED_STAGE_NAME));
                }
            }

        }

        if (riskOp != null) {
            inputSignals.get(SIGNAL_TYPE_RISK).addAll(riskOp.getSignals());
            Log.d(TAG, riskOp.getSignals().toString());

            //construct risk op edges
            Map<String, Map<String, String>> trans = riskOp.getRiskTrans();
            for(String risk: trans.keySet()) {
                Map<String, String> subTrans = trans.get(risk);
                for(Stage stage: stages) {
                    if(stage.getRiskType().equals(risk)) {
                        for(String signal: subTrans.keySet()) {
                            String nextRisk = subTrans.get(signal);
                            String newStage = Stage.constructStageId(nextRisk,
                                    stage.getAuthenticationLevel(),
                                    stage.getSensitivityLevel());
                            if(stageMap.containsKey(newStage)) {
                                Map<String, Stage> submap = transitionMap.get(stage.getStageId());
                                submap.put(signal, stageMap.get(newStage));
                            } else {
                                Map<String, Stage> submap = transitionMap.get(stage.getStageId());
                                submap.put(signal, stageMap.get(Stage.LOCKED_STAGE_NAME));
                            }
                        }
                    }
                }
            }
        }

        // TODO: automatically add self-transition for risk signals

        // for locked stage, we will reject all different signal
        Map<String, Stage> submap = transitionMap.get(Stage.LOCKED_STAGE_NAME);
        for(String category: inputSignals.keySet()) {
            for(String signal: inputSignals.get(category)) {
                if (!signal.equals(AuthSignals.EA_ACCEPT)) {
                    submap.put(signal, stages.get(0));
                }
            }

        }

        // displayAllSignals();

        // construct all edges

        return new MultiStageModel(stages, inputSignals, transitionMap, initStage, stages.get(0));
    }

    private void initInputSignals() {
        inputSignals = new HashMap<>();
        inputSignals.put(SIGNAL_TYPE_RISK, new ArrayList<>());
        inputSignals.put(SIGNAL_TYPE_AUTH, new ArrayList<>());
        inputSignals.put(SIGNAL_TYPE_SENS, new ArrayList<>());
    }

    @NonNull
    private Map<String, Map<String, Stage>> initTransitionMap(List<Stage> stages) {
        Map<String, Map<String, Stage>> result = new HashMap<>();
        for(Stage s: stages) {
            result.put(s.getStageId(), new HashMap<>());
        }
        return result;
    }

    @NonNull
    private Map<String, Stage> createStageMap() {
        Map<String, Stage> result = new HashMap<>();
        for(Stage s: stages) {
            result.put(s.getStageId(), s);
        }
        return result;
    }


    /**
     * DEBUG FUNCTIONS
     */
    public void displayAllSignals() {
        for(String category: inputSignals.keySet()) {
            StringBuilder result = new StringBuilder();
            for(String signal: inputSignals.get(category)) {
                result.append(signal).append(" ");
            }
            Log.i(TAG, category + ": " + result);
        }
    }
}
