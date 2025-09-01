package communication.request;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class PrivateKeyRequest extends ProtoRequest {
    public static short REQUEST_ID = 9;
    public PrivateKeyRequest(){
        super(REQUEST_ID);
    }
}
