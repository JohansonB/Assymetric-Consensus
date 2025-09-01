package trustsystem;

import java.util.*;

public class FaultSystem extends ProcSystem {

    public FaultSystem( Collection<ProcSet> p_sets) {
        super(p_sets);
    }

    public QuorumSystem canonical_quorum_system(HashSet<Proc> members){
        HashSet<Proc> temp;
        ArrayList<ProcSet> q_s = new ArrayList<>();
        for(ProcSet p_set : p_sets){
            temp = new HashSet<>(members);
            temp.removeAll(p_set.get_p_set());
            q_s.add(new ProcSet(temp));
        }
        return new QuorumSystem(q_s);
    }

    public CoreSetSystem get_core_set_system(HashSet<Proc> members){
        ArrayList<ProcSet> core_candidates = new ArrayList<>();
        HashSet<Proc> intersect;
        HashSet<Proc> temp;
        ArrayList<ProcSet> new_candidates;
        HashSet<Proc> comp;
        for(ProcSet f : p_sets){
            comp = new HashSet<>(members);
            comp.removeAll(f.get_p_set());
            //if the kernel_candidates are empty initialize them with a Proc in q each
            if( core_candidates.isEmpty()){
                for(Proc p : comp){
                    core_candidates.add(new ProcSet(Collections.singleton(p)));
                }
                continue;
            }
            //for every candidate check if it intersects comp. If not the candidates are extended by the candidate union with
            //each of the Proces in comp
            Iterator<ProcSet> iterator = core_candidates.iterator();
            new_candidates = new ArrayList<>();
            while (iterator.hasNext()) {
                ProcSet c = iterator.next();
                intersect = new HashSet<>(c.get_p_set());
                intersect.retainAll(comp);
                if (intersect.isEmpty()) {
                    iterator.remove();
                    for(Proc p : comp){
                        temp = new HashSet<>(c.get_p_set());
                        temp.add(p);
                        new_candidates.add(new ProcSet(temp));

                    }
                }
            }
            core_candidates.addAll(new_candidates);
        }
        removeSupersets(core_candidates);
        return new CoreSetSystem(core_candidates);
    }

}
