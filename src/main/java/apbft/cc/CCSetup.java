package apbft.cc;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import trustsystem.Proc;

public class CCSetup extends ProtoRequest {
    public final static short REQUEST_ID = 7;
    private Proc leader;
    private int ts;
    private String proposal;

    public CCSetup(Proc leader, int ts, String proposal) {
        super(REQUEST_ID);
        this.leader = leader;
        this.ts = ts;
        this.proposal = proposal;
    }

    public Proc getLeader() {
        return leader;
    }

    public int getTs() {
        return ts;
    }

    public String getProposal() {
        return proposal;
    }
}
