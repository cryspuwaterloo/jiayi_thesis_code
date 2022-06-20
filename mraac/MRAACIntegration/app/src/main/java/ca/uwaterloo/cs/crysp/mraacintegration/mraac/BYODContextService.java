package ca.uwaterloo.cs.crysp.mraacintegration.mraac;


import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.CONTEXT_DUMMY_ID;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.CONTEXT_ONFOOT_ID;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.CONTEXT_ONSITE_ID;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleDummy.CONTEXT_DUMMY2_ID;

import ca.uwaterloo.cs.crysp.libmraacintegration.context.ContextService;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts.DummyConditionProvider;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts.DummyContextProvider;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts.OnFootProvider;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.contexts.OnsiteContextProvider;

public class BYODContextService extends ContextService {
    @Override
    public void registerContextProviders() {
        // addContextProviders(new OnsiteContextProvider(CONTEXT_ONSITE_ID, this));
        addContextProviders(new OnFootProvider(CONTEXT_ONFOOT_ID, this));
        addContextProviders(new DummyContextProvider(CONTEXT_DUMMY_ID, this));
//        addContextProviders(new DummyConditionProvider(CONTEXT_DUMMY2_ID, this));

    }
}
