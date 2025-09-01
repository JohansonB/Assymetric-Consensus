package pbft.epochconsensus;

import pbft.EpochState;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import trustsystem.Proc;

public class ECSetup extends ProtoRequest {
    public final static short REQUEST_ID = 9;
    EpochState state;
    int ets;
    Proc leader;
    public ECSetup(EpochState state,int ets, Proc leader){
        super(REQUEST_ID);
        this.state = state;
        this.ets = ets;
        this.leader = leader;
    }

    public EpochState getState() {
        return state;
    }

    public Proc getLeader() {
        return leader;
    }

    public int getEts() {
        return ets;
    }
}
