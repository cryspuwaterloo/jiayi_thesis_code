package ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators;

import android.content.Context;
import android.content.Intent;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.AuthCallback;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.BaseAuthenticator;

public class GaitAuthenticator extends BaseAuthenticator {
    public GaitAuthenticator(String id, Context context, AuthCallback callback) {
        super(id, context, callback);
    }

    @Override
    public void start(Adaptation adaptation) {
        super.start(adaptation);
        Intent i=new Intent(getContext(), GaitAuthService.class);
        getContext().startService(i);
        adapt(adaptation);
    }

    @Override
    public void stop() {
        super.stop();
        Intent i=new Intent(getContext(), GaitAuthService.class);
        getContext().stopService(i);
    }

    @Override
    public void adapt(Adaptation adaptation) {

    }

}
