package ca.uwaterloo.cs.crysp.mraacintegration.mraac;

import android.content.Intent;

import ca.uwaterloo.cs.crysp.libmraacintegration.access.AccessControlService;

public class BYODAccessService extends AccessControlService {
    @Override
    public String initResourceRequest() {
        return null;
    }
    /*
    How to get the current resource name ?
    An observer of activity, once the current resource has change, observer.notify(currentResource)
     */

}
