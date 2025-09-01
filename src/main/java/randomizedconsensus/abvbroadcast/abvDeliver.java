package randomizedconsensus.abvbroadcast;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

import java.util.HashMap;

public class abvDeliver extends ProtoReply {
    public static final short REPSONSE_ID = 3;
    private boolean output;
    private HashMap<String,String> payload;


    public abvDeliver(boolean output, HashMap<String,String> payload) {
        super(REPSONSE_ID);
        this.output = output;
        this.payload = payload;

    }
    public boolean getOutput(){
        return output;
    }

    public HashMap<String, String> getPayload() {
        return payload;
    }
}
