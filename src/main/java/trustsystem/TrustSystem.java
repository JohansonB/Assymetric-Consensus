package trustsystem;


import java.util.ArrayList;

public class TrustSystem {
    FaultSystem fault_assumptions;
    QuorumSystem quorums;

    public TrustSystem(FaultSystem fault_assumptions,
                       QuorumSystem quorums) {
        this.fault_assumptions = fault_assumptions;
        this.quorums = quorums;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(fault_assumptions.toString()).append(" ");
        sb.append(quorums.toString());
        sb.append('}');
        return sb.toString();
    }

    public static TrustSystem parse(String code){
        code = code.trim();
        code = code.substring(1, code.length() - 1);
        ArrayList<String> temp = TrustSystemSerializer.flatten_brackets(code);
        FaultSystem f_s = new FaultSystem(FaultSystem.parse(temp.get(0)).get_p_sets());
        QuorumSystem q_s = new QuorumSystem(QuorumSystem.parse(temp.get(1)).get_p_sets());
        return new TrustSystem(f_s,q_s);
    }

    public QuorumSystem get_quorums() {
        return quorums;
    }
    public FaultSystem get_fault_assumptions() {
        return fault_assumptions;
    }
}
