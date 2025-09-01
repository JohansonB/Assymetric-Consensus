package pbft;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class PBFTPropose extends ProtoRequest {
    public static final short PROTO_REQUEST = 12;
    String value;
    public PBFTPropose(String value){
        super(PROTO_REQUEST);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
