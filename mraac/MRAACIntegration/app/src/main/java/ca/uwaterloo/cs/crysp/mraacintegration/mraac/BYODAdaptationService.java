package ca.uwaterloo.cs.crysp.mraacintegration.mraac;

import android.os.Debug;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.AdaptationService;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.MultiStageModel;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Stage;

public class BYODAdaptationService extends AdaptationService {
    private static final String TAG = "BYODMAIN";

    @Override
    public void initModel() {
        // BYOD Logic
//        MultiStageModel model = ExampleBYOD.buildModel();
//        //  System.out.print(model.toCSV());
//        for (Stage stage: model.getStages()) {
//            if (!stage.isLockedStage()) stage.setScheme(ExampleBYOD.exampleAdaptationScheme());
//            else stage.setScheme((ExampleDummy.emptyAdaptationScheme()));
//        }
//        setModel(model);
//        MultiStageModel model = ExampleDummy.buildModel();
//        System.out.print(model.toCSV());
//        for (Stage stage: model.getStages()) {
//            if (!stage.isLockedStage()) stage.setScheme((ExampleDummy.exampleAdaptationScheme()));
//            else stage.setScheme((ExampleDummy.emptyAdaptationScheme()));
//        }
//        setModel(model);

        MultiStageModel model = ExampleAdaptiveCA.buildModel();
        System.out.print(model.toCSV());
        for (Stage stage: model.getStages()) {
            if (!stage.isLockedStage()) stage.setScheme(ExampleBYOD.exampleAdaptationScheme());
            else stage.setScheme((ExampleDummy.emptyAdaptationScheme()));
        }
        setModel(model);
    }
}
