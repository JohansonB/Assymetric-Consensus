package communication.request;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class CloseConnectionsRequest extends ProtoRequest {
    public static short REQUEST_ID = 107;
    public CloseConnectionsRequest(){
        super(REQUEST_ID);
    }
}
