// IMRAACClientInterface.aidl
package ca.uwaterloo.cs.crysp.libmraacintegration;

// Declare any non-default types here with import statements

interface IMRAACClientInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    String getCurrentRiskType();

    int getCurrentAuthenticationLevel();

    int getCurrentSensitivityLevel();

    int sendIAResult(int result, double score);
}
