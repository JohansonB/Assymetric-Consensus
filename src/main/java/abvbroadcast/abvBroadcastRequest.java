package abvbroadcast;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

import java.util.HashMap;

public class abvBroadcastRequest extends ProtoRequest {
    final static short REQUEST_ID = 2;
    HashMap<String,String> pay_load;
    boolean input;

    public abvBroadcastRequest(HashMap<String,String> pay_load, boolean input) {
        super(REQUEST_ID);
        this.pay_load = pay_load;
        this.input = input;
    }

    public HashMap<String, String> getPay_load() {
        return pay_load;
    }
    public boolean getInput(){
        return input;
    }
}
