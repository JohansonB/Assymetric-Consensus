package communication.request;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ReInitKeyRequest extends ProtoRequest{
    public static short REQUEST_ID = 102;
    public ReInitKeyRequest(){
        super(REQUEST_ID);
    }
}

