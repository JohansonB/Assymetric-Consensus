package trustsystem;


import java.util.*;

public class QuorumSystem extends ProcSystem {

    public QuorumSystem( Collection<ProcSet> p_sets) {
        super(p_sets);
    }
    public QuorumSystem( ProcSystem p_s) {
        super(p_s.p_sets);
    }

    public static QuorumSystem majorityQuorums(Collection<Proc> procs) {
        int quorumSize = (int) Math.ceil((procs.size() + 1) / 2.0);
        return thresholdQuorums(procs,quorumSize);
    }
    public static QuorumSystem thresholdQuorums(Collection<Proc> procs, int size){
        List<Set<Proc>> temp_quorumSets = generateCombinations(new ArrayList<>(procs), size);
        List<ProcSet> quorumSets = new ArrayList<>();
        for(Set<Proc> s : temp_quorumSets){
            quorumSets.add(new ProcSet(s));
        }

        return new QuorumSystem(quorumSets);
    }

    private static <T> List<Set<T>> generateCombinations(List<T> elements, int k) {
        List<Set<T>> result = new ArrayList<>();
        generateCombinationsHelper(elements, k, 0, new LinkedHashSet<>(), result);
        return result;
    }

    private static <T> void generateCombinationsHelper(List<T> elements, int k, int start, Set<T> current, List<Set<T>> result) {
        if (current.size() == k) {
            result.add(new HashSet<>(current));
            return;
        }

        for (int i = start; i < elements.size(); i++) {
            current.add(elements.get(i));
            generateCombinationsHelper(elements, k, i + 1, current, result);
            current.remove(elements.get(i));
        }
    }

    public KernelSystem get_kernel_system(){
        ArrayList<ProcSet> kernel_candidates = new ArrayList<>();
        HashSet<Proc> intersect;
        HashSet<Proc> temp;
        ArrayList<ProcSet> new_candidates;
        for(ProcSet q : p_sets){
            //if the kernel_candidates are empty initialize them with a Proc in q each
            if( kernel_candidates.isEmpty()){
                for(Proc p : q){
                    kernel_candidates.add(new ProcSet(Collections.singleton(p)));
                }
                continue;
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
                    for(Proc p : q){
                        temp = new HashSet<>(c.get_p_set());
                        temp.add(p);
                        new_candidates.add(new ProcSet(temp));

                    }
                }
            }
            kernel_candidates.addAll(new_candidates);
        }
        removeSupersets(kernel_candidates);
        return new KernelSystem(kernel_candidates);
    }


}
