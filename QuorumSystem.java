import java.util.*;

public class QuorumSystem extends ProcessSystem {

    QuorumSystem(Collection<ProcessSet> p_sets) {
        super(p_sets);
    }

    public KernelSystem get_kernel_system(){
        ArrayList<ProcessSet> kernel_candidates = new ArrayList<>();
        HashSet<Process> intersect;
        HashSet<Process> temp;
        ArrayList<ProcessSet> new_candidates;
        for(ProcessSet q : p_sets){
            //if the kernel_candidates are empty initialize them with a process in q each
            if( kernel_candidates.isEmpty()){
                for(Process p : q.p_set){
                    kernel_candidates.add(new ProcessSet(Collections.singleton(p)));
                }
            }
            //for every candidate check if it intersects q. If not the candidates are extended by the candidate union with
            //each of the Processes in q
            Iterator<ProcessSet> iterator = kernel_candidates.iterator();
            new_candidates = new ArrayList<>();
            while (iterator.hasNext()) {
                ProcessSet c = iterator.next();
                intersect = new HashSet<>(c.get_p_set());
                intersect.retainAll(q.get_p_set());
                if (intersect.isEmpty()) {
                    iterator.remove();
                    for(Process p : q.get_p_set()){
                        temp = new HashSet<>(c.get_p_set());
                        temp.add(p);
                        new_candidates.add(new ProcessSet(temp));

                    }
                }
            }
            kernel_candidates.addAll(new_candidates);
        }
        removeSubsets(kernel_candidates);
        return new KernelSystem(kernel_candidates);
    }

    private void removeSubsets(ArrayList<ProcessSet> kernel_candidates) {
        ArrayList<ProcessSet> toRemove = new ArrayList<>();

        for (int i = 0; i < kernel_candidates.size(); i++) {
            ProcessSet set1 = kernel_candidates.get(i);

            for (int j = i+1; j < kernel_candidates.size(); j++) {
                ProcessSet set2 = kernel_candidates.get(j);

                if (set2.get_p_set().containsAll(set1.get_p_set())) {
                    toRemove.add(set1);
                    break;  // No need to check further once we've found a subset
                }
            }
        }
        kernel_candidates.removeAll(toRemove);
    }
}
