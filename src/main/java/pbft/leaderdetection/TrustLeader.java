package pbft.leaderdetection;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import trustsystem.Proc;

public class TrustLeader extends ProtoReply {
    public static final short REPLY_ID = 7;
    private Proc leader;

    public TrustLeader(Proc leader) {
        super(REPLY_ID);
        this.leader = leader;
    }

    public Proc getLeader() {
        return leader;
    }
}
