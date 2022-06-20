package ca.uwaterloo.cs.crysp.libmraacintegration.adaptation.base;


import android.content.Context;
import android.content.Intent;

public class Signal{
    private String name;
    private String sourceId;

    public Signal(String name, String sourceId){
        // super.setAction(name);
        this.name = name;
        this.sourceId = sourceId;
    }

    public String getName() {
        return name;
    }

    public String getSourceId() {
        return sourceId;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Intent toIntent(Context context, Class<?> cls, String action) {
        Intent tmp = new Intent(context, cls);
        tmp.setAction(action);
        tmp.putExtra("sourceId", sourceId);
        tmp.putExtra("signal", name);
        return tmp;
    }

    public Intent toIntent(String action) {
        Intent tmp = new Intent();
        tmp.setAction(action);
        tmp.putExtra("sourceId", sourceId);
        tmp.putExtra("signal", name);
        return tmp;
    }

    public static Signal fromIntent(Intent intent) {
        String sourceId = intent.getStringExtra("sourceId");
        String name = intent.getStringExtra("signal");
        return new Signal(name, sourceId);
    }

}
