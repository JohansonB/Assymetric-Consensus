public class TrustSystem {
    FaultSystem fault_assumptions;
    QuorumSystem quorums;

    public QuorumSystem get_quorums() {
        return quorums;
    }
    public FaultSystem get_fault_assumptions() {
        return fault_assumptions;
    }
}
