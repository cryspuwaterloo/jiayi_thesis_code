package ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts;

import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleAdaptiveCA.AUTH_SIGNAL_GUEST;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleDummy.CONTEXT_DUMMY_CONDITION;

import android.content.Context;
import android.content.Intent;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;
import ca.uwaterloo.cs.crysp.libmraacintegration.context.BaseContextProvider;

public class DummyConditionProvider extends BaseContextProvider {
    public DummyConditionProvider(String id, Context context) {
        super(id, context);
    }

    @Override
    public void start(Adaptation adaptation) {
        super.start(adaptation);
        Intent i = new Intent(getContext(), DummyConditionService.class);
        getContext().startService(i);
    }

    @Override
    public void stop() {
        super.stop();
        Intent i = new Intent(getContext(), DummyConditionService.class);
        getContext().stopService(i);
    }

    @Override
    public void adapt(Adaptation adaptation) {
    }

    @Override
    public void processResult(int result) {
        if (result == 1) {

            sendContextSignal(new Signal(CONTEXT_DUMMY_CONDITION, getId()));
        }

    }
}
