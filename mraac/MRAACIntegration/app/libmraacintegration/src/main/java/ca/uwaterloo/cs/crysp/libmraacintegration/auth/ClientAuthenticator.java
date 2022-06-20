package ca.uwaterloo.cs.crysp.libmraacintegration.auth;

import android.content.Context;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.AuthCallback;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.BaseAuthenticator;

public class ClientAuthenticator extends BaseAuthenticator {
    public ClientAuthenticator(String id, Context context, AuthCallback callback) {
        super(id, context, callback);
    }

    @Override
    public void adapt(Adaptation adaptation) {

    }
}
