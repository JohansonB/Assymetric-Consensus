package communication.request;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import trustsystem.Proc;

import java.util.HashMap;

public class SetRunId extends ProtoRequest {
    public static final short REQUEST_ID = 100;
    private final int run_id;
    private short origin_proto;
    public SetRunId(int run_id, short origin_proto) {
        super(SetRunId.REQUEST_ID);
        this.run_id = run_id;
        this.origin_proto = origin_proto;
    }

    public int getRunId() {
        return run_id;
    }

    public short getOriginProto(){
        return origin_proto;
    }
}
