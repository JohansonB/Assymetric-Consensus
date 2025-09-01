package pbft.epochconsensus;

import pbft.EpochState;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class ECAborted extends ProtoReply {
    public static final short REPLY_ID = 11;
    int ts;
    EpochState state;
    public ECAborted(EpochState state, int ts){
        super(REPLY_ID);
        this.state = state;
        this.ts = ts;
    }

    public EpochState getState() {
        return state;
    }

    public int getTs() {
        return ts;
    }
}
