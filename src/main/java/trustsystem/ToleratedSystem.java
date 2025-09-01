package trustsystem;

import java.util.Collection;

public class ToleratedSystem extends TrustSystem {
    ToleratedSystem(FaultSystem fault_assumptions,
                    QuorumSystem quorums) {
        super(fault_assumptions,quorums);
    }
    public ToleratedSystem(TrustSystem ts){
        super(ts.fault_assumptions,ts.quorums);
    }
}
