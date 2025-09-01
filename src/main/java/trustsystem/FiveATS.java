package trustsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FiveATS extends AsymmetricTrustSystem {
    public static final int start_port = 5000;
    public static final String address = "127.0.0.1";
    public FiveATS(AsymmetricTrustSystem  ats){
        super(ats.asym_trust_system);
    }
    public FiveATS() {
        super(new HashMap<>());
        ArrayList<Proc> members = new ArrayList<>();
        HashMap<Proc,TrustSystem> ats = new HashMap<>();
        for(int i = 0; i<5;i++){
            members.add(new Proc(i,address,start_port+i));
        }
        TrustSystem t_s;
        HashSet<Proc> p_s = new HashSet<>();
        ArrayList<ProcSet> p_sets = new ArrayList<>();

        p_s.add(members.get(0));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(1));
        p_s.add(members.get(3));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        t_s = new TrustSystem(new FaultSystem(new HashSet<>()),new QuorumSystem(p_sets));
        ats.put(members.get(0),t_s);
        p_sets = new ArrayList<>();

        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(1));
        p_s.add(members.get(3));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        t_s = new TrustSystem(new FaultSystem(new HashSet<>()),new QuorumSystem(p_sets));
        ats.put(members.get(1),t_s);
        p_sets = new ArrayList<>();

        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(2));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        t_s = new TrustSystem(new FaultSystem(new HashSet<>()),new QuorumSystem(p_sets));
        ats.put(members.get(2),t_s);
        p_sets = new ArrayList<>();

        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(1));
        p_s.add(members.get(3));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        p_s.add(members.get(0));
        p_s.add(members.get(1));
        p_s.add(members.get(2));
        p_s.add(members.get(3));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        t_s = new TrustSystem(new FaultSystem(new HashSet<>()),new QuorumSystem(p_sets));
        ats.put(members.get(3),t_s);
        p_sets = new ArrayList<>();

        p_s.add(members.get(0));
        p_s.add(members.get(2));
        p_s.add(members.get(4));
        p_sets.add(new ProcSet(p_s));
        p_s = new HashSet<>();

        t_s = new TrustSystem(new FaultSystem(new HashSet<>()),new QuorumSystem(p_sets));
        ats.put(members.get(4),t_s);
        p_sets = new ArrayList<>();


        asym_trust_system = ats;
        this.members = members;
    }
    @Override
    public FiveATS permute(HashMap<Proc,Proc> per){
        return new FiveATS(super.permute(per));
    }
    @Override
    public FiveATS permute(){
        return new FiveATS(super.permute());
    }

    @Override
    public String name(){
        return "5pATS";
    }
}
