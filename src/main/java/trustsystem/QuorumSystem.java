package trustsystem;


import java.util.*;

public class QuorumSystem extends ProcSystem {

    QuorumSystem( Collection<ProcSet> p_sets) {
        super(p_sets);
    }

    public KernelSystem get_kernel_system(){
        ArrayList<ProcSet> kernel_candidates = new ArrayList<>();
        HashSet<Proc> intersect;
        HashSet<Proc> temp;
        ArrayList<ProcSet> new_candidates;
        for(ProcSet q : p_sets){
            //if the kernel_candidates are empty initialize them with a Proc in q each
            if( kernel_candidates.isEmpty()){
                for(Proc p : q.p_set){
                    kernel_candidates.add(new ProcSet(Collections.singleton(p)));
                }
            }
            //for every candidate check if it intersects q. If not the candidates are extended by the candidate union with
            //each of the Proces in q
            Iterator<ProcSet> iterator = kernel_candidates.iterator();
            new_candidates = new ArrayList<>();
            while (iterator.hasNext()) {
                ProcSet c = iterator.next();
                intersect = new HashSet<>(c.get_p_set());
                intersect.retainAll(q.get_p_set());
                if (intersect.isEmpty()) {
                    iterator.remove();
                    for(Proc p : q.get_p_set()){
                        temp = new HashSet<>(c.get_p_set());
                        temp.add(p);
                        new_candidates.add(new ProcSet(temp));

                    }
                }
            }
            kernel_candidates.addAll(new_candidates);
        }
        removeSubsets(kernel_candidates);
        return new KernelSystem(kernel_candidates);
    }

    private void removeSubsets(ArrayList<ProcSet> kernel_candidates) {
        ArrayList<ProcSet> toRemove = new ArrayList<>();

        for (int i = 0; i < kernel_candidates.size(); i++) {
            ProcSet set1 = kernel_candidates.get(i);

            for (int j = i+1; j < kernel_candidates.size(); j++) {
                ProcSet set2 = kernel_candidates.get(j);

                if (set2.get_p_set().containsAll(set1.get_p_set())) {
                    toRemove.add(set1);
                    break;  // No need to check further once we've found a subset
                }
            }
        }
        kernel_candidates.removeAll(toRemove);
    }
}
