package communication.request;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class AllKeysUp extends ProtoRequest {
    public static short REQUEST_ID = 104;
    public AllKeysUp(){
        super(REQUEST_ID);
    }
}
