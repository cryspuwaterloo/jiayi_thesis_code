package ca.uwaterloo.cs.crysp.mraacintegration.mraac;

import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals.IA_ACCEPT;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.AuthSignals.IA_REJECT;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_START;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_STOP;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.ADAPTATION_TUNE;
import static ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.constants.ServiceSignals.SERVICE_AUTHENTICATION;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.AUTH_DUMMY_ID;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.AUTH_GAIT_ID;
import static ca.uwaterloo.cs.crysp.mraacintegration.mraac.ExampleBYOD.AUTH_ITUS_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Adaptation;
import ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base.Signal;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.AuthCallback;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.AuthenticationService;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.BaseAuthenticator;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.ClientAuthenticator;
import ca.uwaterloo.cs.crysp.libmraacintegration.auth.SimpleSlidingWindow;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators.DummyAuthenticator;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators.GaitAuthenticator;
import ca.uwaterloo.cs.crysp.mraacintegration.mraac.authenticators.ItusAuthenticator;


/**
 * The collected authentication results are integers
 */
public class BYODAuthenticationService extends AuthenticationService {
    private static final String TAG = "BYODAUTH";
    private SimpleSlidingWindow ssw;

    public BYODAuthenticationService() {
        ssw = new SimpleSlidingWindow(1,1);
    }

    @Override
    public void registerAuthenticators() {
        /*
         * To create an authenticator instance, you need to specify its id (which is the identifier
         * in the adaptation policy), the service context, and a callback function regarding how
         * you process the result.
         *
         * By default, it should be
         * this, result -> aggregateResult(id, result)
         */
        // addAuthenticators(new ItusAuthenticator(AUTH_ITUS_ID,
        //        this, result -> aggregateResult(AUTH_ITUS_ID, result)));
        addAuthenticators(new GaitAuthenticator(AUTH_GAIT_ID,
                this, result -> aggregateResult(AUTH_GAIT_ID, result)));
//        addAuthenticators(new DummyAuthenticator(AUTH_DUMMY_ID,
//                this, result -> aggregateResult(AUTH_DUMMY_ID, result)));
        addAuthenticators(new ClientAuthenticator(CLIENT_AUTH_ID,
                this, result -> {

            aggregateResult(CLIENT_AUTH_ID, result);
        }));
    }

    @Override
    public void aggregateResult(String authName, double result) {
        /*
         * Implement you score fusion scheme here.
         */
        if (result >= 0.0) {
            ssw.add(1);
        } else{
            ssw.add(0);
        }

        int significant = ssw.getQualifiedMajority();

        if (significant == 1) {
            // need to pack the method
            sendAuthResult(new Signal(IA_ACCEPT, authName));
        } else if (significant == 0) {
            sendAuthResult(new Signal(IA_REJECT, authName));
        }
    }
}
