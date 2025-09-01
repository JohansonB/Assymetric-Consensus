package randomizedconsensus.abvbroadcast;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

import java.util.HashMap;

public class MultiplexAbvDeliver extends ProtoReply {
    public static final short REPSONSE_ID = 4;
    private boolean output;
    private HashMap<String,String> payload;
    private int round;


    public MultiplexAbvDeliver(boolean output,int round, HashMap<String,String> payload) {
        super(REPSONSE_ID);
        this.output = output;
        this.payload = payload;
        this.round = round;

    }
    public boolean getOutput(){
        return output;
    }

    public HashMap<String, String> getPayload() {
        return payload;
    }

    public int getRound() {
        return round;
    }
}
