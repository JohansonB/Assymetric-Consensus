package trustsystem;


import java.util.*;

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

    public TrustSystem permute(HashMap<Proc,Proc> permutation){
        ArrayList<ProcSet> temp = new ArrayList<>();
        ProcSet temptemp;
        FaultSystem f_s;
        QuorumSystem q_s;
        for(ProcSet p_set : fault_assumptions){
            temptemp = new ProcSet(new HashSet<>());
            for(Proc p : p_set){
                temptemp.p_set.add(permutation.get(p));
            }
            temp.add(temptemp);
        }
        f_s = new FaultSystem(temp);

        temp = new ArrayList<>();

        for(ProcSet p_set : quorums){
            temptemp = new ProcSet(new HashSet<>());
            for(Proc p : p_set){
                temptemp.p_set.add(permutation.get(p));
            }
            temp.add(temptemp);
        }
        q_s = new QuorumSystem(temp);
        return new TrustSystem(f_s,q_s);
    }

    public QuorumSystem get_quorums() {
        return quorums;
    }
    public FaultSystem get_fault_assumptions() {
        return fault_assumptions;
    }

    public static TrustSystem threshold_system(Collection<Proc> procs,int threshold){
        return new TrustSystem(new FaultSystem(new HashSet<>()),QuorumSystem.thresholdQuorums(procs,threshold));
    }
    public static TrustSystem threshold_system(Collection<Proc> procs) {
        return new TrustSystem(new FaultSystem(new HashSet<>()),QuorumSystem.majorityQuorums(procs));

    }
}
