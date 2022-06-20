package ca.uwaterloo.cs.crysp.libmraacintegration.auth;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class SWParser {
    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = { "-reset", "-r" }, description = "Reset sliding window")
    private boolean reset = false;

    @Parameter(names = "-m", description = "m")
    private Integer m;

    @Parameter(names = "-n", description = "n")
    private Integer n;

    public Integer getM() {
        return m;
    }

    public Integer getN() {
        return n;
    }

    public boolean isReset() {
        return reset;
    }
}
