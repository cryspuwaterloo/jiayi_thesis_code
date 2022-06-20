package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.AdaptationScheme;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Stage;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.exceptions.IncompleteTransitionTableException;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.exceptions.StageNotFoundException;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.utils.Utilities;

public class MultiStageModel {

    private static final String TAG = "MultiStageModel";

    public final static String SIGNAL_TYPE_RISK = "SIGNAL_TYPE_RISK";
    public final static String SIGNAL_TYPE_AUTH = "SIGNAL_TYPE_AUTH";
    public final static String SIGNAL_TYPE_SENS = "SIGNAL_TYPE_SENS";



    private List<Stage> stages;
    private Set<String> inputSignals;
    private Map<String, Map<String, Stage>> transitionTable;
    private Stage initStage;
    private Stage lockedStage;



    public MultiStageModel(List<Stage> stages, Map<String, List<String>> inputSignals, Map<String, Map<String, Stage>> transitionTable, Stage initStage, Stage lockedStage) {
        this.stages = stages;
        this.inputSignals = new HashSet<>();
        for(List<String> input: inputSignals.values()) this.inputSignals.addAll(input);
        this.transitionTable = transitionTable;
        this.initStage = initStage;
        this.lockedStage = lockedStage;
    }


    /**
     * Find the stage with providing stageId
     * @param stageId a string identifier for a stage
     * @return the stage object
     */

    public Stage findStage(String stageId) {
        for (Stage s: stages) {
            if (s.getStageId().equals(stageId)) return s;
        }
        return null;
    }

    public Stage findStage(String riskType, int auth, int sens) {
        String stageId = Stage.constructStageId(riskType, auth, sens);
        return findStage(stageId);
    }


    /**
     * Mannually add stage transitions
     * @param current the current stage id
     * @param signal the received signal name
     * @param next the next stage id
     */

    public void addTransition(String current, String signal, String next) {
        if (!transitionTable.containsKey(current)) throw new StageNotFoundException();
        Map<String, Stage> map = transitionTable.get(current);
        Stage nextStage = findStage(next);
        if (nextStage == null) throw new StageNotFoundException();
        if (map == null) map = new HashMap<>();
        map.put(signal, nextStage);
    }

    public Stage makeTransition(Stage current, Signal signal) {
        if (!stages.contains(current)) {
            throw new NoSuchElementException();
        }
        if (!transitionTable.containsKey(current.getStageId())) {
            throw new IncompleteTransitionTableException();
        }

        Map<String, Stage> relatedTransitions = transitionTable.get(current.getStageId());
        assert relatedTransitions != null;
        if (!relatedTransitions.containsKey(signal.getName())) {
            Log.i(TAG, "Not defined transition");
            // reaction to undefined transititon
            // stay in the current stage or move to locked stage
            Log.d("MRAAC_EXP", "undefined transition");
            return this.lockedStage;
        } else {
            return relatedTransitions.get(signal.getName());
        }
    }

    public Stage getAdaptationStage(String stageName) {
        for (Stage s: stages){
            if (s.getStageId().equals(stageName)) return s;
        }
        throw new StageNotFoundException();
    }

    public Stage getAdaptationStage(String riskType, int authentication, int sensitivity) {
        for (Stage s: stages) {
            if (s.getRiskType().equals(riskType) && s.getAuthenticationLevel() == authentication && s.getSensitivityLevel() == sensitivity) {
                return s;
            }
        }
        throw new StageNotFoundException();
    }


    public void setAdaptationScheme(@NonNull Stage stage, AdaptationScheme scheme) {
        stage.setScheme(scheme);
    }

    public void setAdaptationScheme(String stageId, AdaptationScheme scheme) {
        Stage selectStage = this.getAdaptationStage(stageId);
        this.setAdaptationScheme(selectStage, scheme);
    }

    public Set<String> getInputSignals() {
        return inputSignals;
    }

    public boolean isSupportedSignals(String name) {
        return inputSignals.contains(name);
    }

    public Stage getInitStage() {
        return initStage;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public Stage getLockedStage() {
        return lockedStage;
    }



    /***********************
     *  Utility functions  *
     ***********************/


    // gen TransitionTable
    public String toCSV() {
        StringBuilder result = new StringBuilder();
        String[][] st = new String[stages.size()][stages.size()];
        String[] names = new String[stages.size()];
        for(int i=0; i < stages.size(); ++i)
            for(int j=0; j < stages.size();++j)
                st[i][j] = "";


        Map<String, Integer> stageMap = new HashMap<>();
        for(int i=0; i < stages.size(); ++i) {
            stageMap.put(stages.get(i).getStageId(), i);
            names[i] = stages.get(i).getStageId();
        }


        for(Stage stage: stages) {
            Map<String, Stage> subMap = transitionTable.get(stage.getStageId());
            for(String signalName: subMap.keySet()) {
                Stage toStage = subMap.get(signalName);
                int i = stageMap.get(stage.getStageId());
                int j = stageMap.get(toStage.getStageId());
                if(st[i][j].length() == 0) st[i][j] = signalName;
                else st[i][j] = Utilities.join("|", st[i][j], signalName);
            }
        }

        result.append(",").append(Utilities.join(",", names)).append("\n");
        for(int i=0; i < stages.size(); ++i) {
            result.append(names[i]).append(",").append(Utilities.join(",", st[i]))
                    .append("\n");
        }
        return result.toString();
    }



}
