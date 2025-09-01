package pbft.epochconsensus;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ECAbort  extends ProtoRequest {
    public static final short REQUEST_ID = 11;
    public ECAbort(){
        super(REQUEST_ID);
    }
}
