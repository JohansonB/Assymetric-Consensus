package recpbft.conditionalcollect;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import trustsystem.Proc;

import java.util.HashMap;

public class CollectedOutput extends ProtoReply {
    public static final short PROTO_REPLY = 9;
    HashMap<Proc,String> M;
    int ts;
    boolean proposed;
    public CollectedOutput(HashMap<Proc,String> M, int ts, boolean proposed){
        super(PROTO_REPLY);
        this.M = M;
        this.ts = ts;
        this.proposed = proposed;
    }

    public HashMap<Proc, String> getM() {
        return M;
    }

    public boolean getProposed(){
        return proposed;
    }
    public int getTs() {
        return ts;
    }
}
