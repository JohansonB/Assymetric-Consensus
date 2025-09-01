package randomizedconsensus.abvbroadcast;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

import java.util.HashMap;

public class MultiplexAbvBroadcastRequest extends ProtoRequest {
    final static short REQUEST_ID = 3;
    HashMap<String,String> pay_load;
    boolean input;
    int round;

    public MultiplexAbvBroadcastRequest(HashMap<String,String> pay_load, boolean input, int round) {
        super(REQUEST_ID);
        this.pay_load = pay_load;
        this.input = input;
        this.round = round;
    }

    public HashMap<String, String> getPay_load() {
        return pay_load;
    }
    public boolean getInput(){
        return input;
    }

    public int getRound() {
        return round;
    }
}
