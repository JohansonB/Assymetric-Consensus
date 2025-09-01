package pbft.leaderdetection;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import trustsystem.Proc;

public class Complain extends ProtoRequest {
    public final static short REQUEST_ID = 6;
    private Proc leader;

    public Complain(Proc leader) {
        super(REQUEST_ID);
        this.leader = leader;
    }

    public Proc getLeader() {
        return leader;
    }

}
