package communication.request;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import trustsystem.Proc;

public class PublicKeyRequest extends ProtoRequest {
    public static short REQUEST_ID = 10;
    Proc p;
    short source_proto;
    public PublicKeyRequest(Proc p, short source_proto){
        super(REQUEST_ID);
        this.p = p;
        this.source_proto = source_proto;
    }

    public Proc getP() {
        return p;
    }

    public short getSource_proto() {
        return source_proto;
    }
}
