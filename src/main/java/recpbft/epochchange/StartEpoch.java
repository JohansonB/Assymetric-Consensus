package recpbft.epochchange;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import trustsystem.Proc;

public class StartEpoch extends ProtoReply {
    public static final short REPLY_ID = 8;
    private Proc leader;
    private int ts;

    public StartEpoch(Proc leader, int ts) {
        super(REPLY_ID);
        this.leader = leader;
        this.ts = ts;
    }

    public Proc getLeader() {
        return leader;
    }

    public int getTs() {
        return ts;
    }
}
