package recpbft.epochconsensus;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ECPropose extends ProtoRequest {
    public static final short REQUEST_ID = 10;
    String val;
    public ECPropose(String val){
        super(REQUEST_ID);
        this.val = val;
    }

    public String getVal() {
        return val;
    }
}
