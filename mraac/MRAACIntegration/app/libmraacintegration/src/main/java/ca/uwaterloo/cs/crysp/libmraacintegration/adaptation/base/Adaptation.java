package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base;

import android.content.Context;
import android.content.Intent;

public class Adaptation  {
    private String targetId;
    private String adaptation;
    private String argument;


    public Adaptation(String targetId, String adaptation) {
        this.targetId = targetId;
        this.adaptation = adaptation;
        this.argument = "default";
    }

    public Adaptation(String targetId, String adaptation, String argument) {
        this.targetId = targetId;
        this.adaptation = adaptation;
        this.argument = argument;
    }


    public String getTargetId() {
        return targetId;
    }

    public String getAdaptation() {
        return adaptation;
    }

    public String getArgument() {
        return argument;
    }

    public void setAdaptation(String adaptation) {
        this.adaptation = adaptation;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public Intent toIntent(Context context, Class<?> cls, String action) {
        Intent tmp = new Intent(context, cls);
        tmp.setAction(action);
        tmp.putExtra("targetId", targetId);
        tmp.putExtra("Adaptation", adaptation);
        tmp.putExtra("argument", argument);
        return tmp;
    }

    public Intent toIntent(String action) {
        Intent tmp = new Intent();
        tmp.setAction(action);
        tmp.putExtra("targetId", targetId);
        tmp.putExtra("Adaptation", adaptation);
        tmp.putExtra("argument", argument);
        return tmp;
    }

    public static Adaptation fromIntent(Intent intent) {
        String targetId = intent.getStringExtra("targetId");
        String adaptation = intent.getStringExtra("Adaptation");
        String argument = intent.getStringExtra("argument");
        return new Adaptation(targetId, adaptation, argument);
    }

}
