package trustsystem;

import org.jgrapht.Graph;
import org.jgrapht.alg.clique.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;

public class AsymmetricTrustSystem {
    HashMap<Proc,TrustSystem> asym_trust_system;
    ArrayList<Proc> members;

    private void set_members(){
        members = new ArrayList<>(asym_trust_system.keySet());
    }

    public boolean is_valid(){

        TrustSystem t_s_p1;
        TrustSystem t_s_p2;
        QuorumSystem p1_qs;
        QuorumSystem p2_qs;
        FaultSystem p1_fs;
        FaultSystem p2_fs;
        Proc p1;
        Proc p2;
        ProcSet p1_q;
        ProcSet p2_q;
        ProcSet p1_f;
        ProcSet p2_f;
        HashSet<Proc> intersection_q;
        HashSet<Proc> intersection_f;

        //checking the consistency property
        for(int pi = 0; pi<members.size();pi++){
            p1 = members.get(pi);
            for(int pj = pi+1; pj<members.size();pj++){
                p2 = members.get(pj);
                t_s_p1 = asym_trust_system.get(p1);
                t_s_p2 = asym_trust_system.get(p2);
                p1_qs = t_s_p1.get_quorums();
                p2_qs = t_s_p2.get_quorums();
                p1_fs = t_s_p1.get_fault_assumptions();
                p2_fs = t_s_p2.get_fault_assumptions();
                for(int q1_index = 0; q1_index<p1_qs.size();q1_index++){
                    p1_q = p1_qs.get(q1_index);

                    for(int q2_index = 0; q2_index<p2_qs.size();q2_index++){
                        p2_q = p2_qs.get(q2_index);
                        for(int f1_index = 0; f1_index<p1_fs.size();f1_index++){
                            p1_f = p1_fs.get(f1_index);
                            for(int f2_index = 0; f2_index<p2_fs.size();f2_index++){
                                p2_f = p2_fs.get(f2_index);
                                intersection_q = new HashSet<>(p1_q.get_p_set());
                                intersection_q.retainAll(p2_q.get_p_set());
                                intersection_f = new HashSet<>(p1_f.get_p_set());
                                intersection_f.retainAll(p2_f.get_p_set());
                                if(intersection_f.containsAll(intersection_q)){
                                    return false;
                                }

                            }
                        }

                    }
                }
            }
        }

        //checking the availability property
        boolean found = false;
        HashSet<Proc> intersect;
        for(Proc p : members){
            t_s_p1 = asym_trust_system.get(p);
            p1_fs = t_s_p1.get_fault_assumptions();
            p1_qs = t_s_p1.get_quorums();
            for(ProcSet f : p1_fs){
                for(ProcSet q : p1_qs){
                    intersect = new HashSet<>(f.get_p_set());
                    intersect.retainAll(q.p_set);
                    if(intersect.isEmpty()){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    return false;
                }
                found = false;
            }
        }
        return true;
    }
    private static class MarkedProcSet{
        HashSet<Proc> unchecked = new HashSet<>();
        HashSet<Proc> checked = new HashSet<>();
        ProcSet p_set;
        MarkedProcSet(ProcSet innit){
            p_set = new ProcSet(innit.p_set);
            unchecked.addAll(innit.get_p_set());
        }
        void add_p(Proc p){
            p_set.get_p_set().add(p);
            if(!checked.contains(p))
                unchecked.add(p);
        }
        void add_p_set(ProcSet p_set){
            for (Proc p : p_set){
                if(!checked.contains(p)) {
                    p_set.get_p_set().add(p);
                    unchecked.add(p);
                }
            }
        }
        void mark_p(Proc p){
            unchecked.remove(p);
            checked.add(p);
        }
        boolean has_unmarked(){
            return unchecked.size()!=0;
        }
        Proc get_unmarked(){
            return unchecked.iterator().next();
        }
        ProcSet get_p_set(){
            return p_set;
        }

        public MarkedProcSet clone() {
            MarkedProcSet cloned = new MarkedProcSet(p_set);
            cloned.unchecked = new HashSet<>(unchecked);
            cloned.checked = new HashSet<>(checked);
            return cloned;
        }

    }
    private static class GuildCandidate{
        MarkedProcSet m_p_set;
        HashSet<Proc> fault_set;
        Collection<Proc> members;
        GuildCandidate(ProcSet innit,Collection<Proc> members){
            this.members = members;
            m_p_set = new MarkedProcSet(innit);
            HashSet<Proc> members_s = new HashSet<>(members);
            members.removeAll(innit.get_p_set());
            fault_set = members_s;
        }

        void add_p(Proc p){
            m_p_set.add_p(p);
        }
        void add_p_set(ProcSet p_set){
            m_p_set.add_p_set(p_set);
            HashSet<Proc> f = new HashSet<>(members);
            f.removeAll(p_set.get_p_set());
            f.retainAll(fault_set);
            fault_set = f;
        }
        void mark_p(Proc p){
            m_p_set.mark_p(p);
        }
        boolean has_unmarked(){
            return m_p_set.has_unmarked();
        }

        ProcSet get_p_set(){
            return m_p_set.get_p_set();
        }
        Proc get_unmarked(){
            return m_p_set.get_unmarked();
        }

        public GuildCandidate clone() {
            GuildCandidate cloned = new GuildCandidate(get_p_set(),members);
            cloned.fault_set = new HashSet<>(fault_set);
            return cloned;
        }



    }
    private Set<ProcSet> get_all_guilds(){
        Queue<GuildCandidate> guild_candidates = new LinkedList<>();
        HashSet<GuildCandidate> guilds = new HashSet<>();
        GuildCandidate cur;
        for(Proc p : members) {
            for (ProcSet p_set : asym_trust_system.get(p).get_quorums()) {
                cur = new GuildCandidate(p_set,members);
                cur.add_p(p);
                cur.mark_p(p);
                guild_candidates.add(cur);
            }
        }

        Proc cur_p;
        GuildCandidate clone;
        while(!guild_candidates.isEmpty()){
            cur = guild_candidates.remove();
            if(!cur.has_unmarked()){
                guilds.add(cur);
                continue;
            }
            cur_p = cur.get_unmarked();
            for(ProcSet p_set : asym_trust_system.get(cur_p).get_quorums()){
                clone = cur.clone();
                clone.add_p_set(p_set);
                clone.mark_p(cur_p);
                guild_candidates.add(clone);
            }

        }
        return union_closure(guilds);

    }

    private Set<ProcSet> union_closure(HashSet<GuildCandidate> guilds) {
        Set<ProcSet> all_guilds = new HashSet<>();
        //first filter the Guild candidates removing all guilds with the empty fault set handling them seperatly.
        //further all guilds are removed that have a equal pset to another guild and a sub-fault set
        HashSet<GuildCandidate> empty_guilds = new HashSet<>();
        for (GuildCandidate g : guilds){
            if(g.fault_set.isEmpty()){
                empty_guilds.add(g);
            }
        }
        guilds.removeAll(empty_guilds);
        List<ProcSet> empty_guilds_p_sets = new ArrayList<>();
        for(GuildCandidate g : empty_guilds){
            empty_guilds_p_sets.add(g.get_p_set());
        }
        int i = 0;
        int j;
        HashSet<GuildCandidate> to_remove = new HashSet<>();
        for(GuildCandidate g1 : guilds){
            j = 0;
            for(GuildCandidate g2 : guilds){
                //check the condition for removal of g2
                if(g1.get_p_set().equals(g2.get_p_set())&&g1.fault_set.containsAll(g2.fault_set)){
                    //in the case that the fault sets are also equal we only want to remove one of them thus i<j
                    if(!g1.fault_set.equals(g2.fault_set)||i<j){
                        to_remove.add(g2);
                    }
                }
            j++;
            }
        i++;
        }
        guilds.removeAll(to_remove);

        //create a graph where each guild member is represented by a vertex and an edge between two vertices encodes
        //that they share elements in theire fault sets
        Graph<ProcSet, DefaultEdge> graph = buildGraph(guilds);
        Iterator<Set<ProcSet>> cliques = findMaximalCliques(graph);

        all_guilds.addAll(computeUnions(empty_guilds_p_sets));

        Set<ProcSet> cl;

        while (cliques.hasNext()) {
            cl = cliques.next();
            // Do something with the 'cl' set
            all_guilds.addAll(computeUnions(new ArrayList<>(cl)));
        }
        remove_duplicates(all_guilds);
        return all_guilds;
    }

    private void remove_duplicates(Set<ProcSet> all_guilds) {
        HashSet<ProcSet> to_remove = new HashSet<>();
        int i = 0;
        for(ProcSet p_set : all_guilds){
            int j = 0;
            for(ProcSet p_set2 : all_guilds){
                if(p_set.equals(p_set2)&&i<j){
                    to_remove.add(p_set);
                }
                j++;
            }
            i++;
        }
        all_guilds.remove(to_remove);
    }

    // Function to compute all possible unions of a set of sets
    public static Set<ProcSet> computeUnions(List<ProcSet> sets) {
        Set<ProcSet> result = new HashSet<>();

        // Get the number of sets
        int n = sets.size();

        // Iterate over all possible subsets of the list of sets
        // Each subset represents a possible combination of sets to union
        for (int i = 1; i < (1 << n); i++) {
            HashSet<Proc> union = new HashSet<>();

            // For each subset, add the sets included in the subset to the union
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) { // If the j-th set is included in the subset
                    union.addAll(sets.get(j).get_p_set());
                }
            }

            // Add the union of the current subset to the result set
            result.add(new ProcSet(union));
        }

        return result;
    }


    // Build the graph using JGraphT's SimpleGraph
    public static Graph<ProcSet, DefaultEdge> buildGraph(Set<GuildCandidate> guilds) {
        Graph<ProcSet, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        // Add vertices (guilds)
        for (GuildCandidate g : guilds) {
            graph.addVertex(g.get_p_set());
        }

        // Add edges between guilds that share elements in their fault sets
        for (GuildCandidate g1 : guilds) {
            for (GuildCandidate g2 : guilds) {
                if (!g1.equals(g2) && !Collections.disjoint(g1.fault_set, g2.fault_set)) {
                    graph.addEdge(g1.get_p_set(), g2.get_p_set());
                }
            }
        }

        return graph;
    }

    // Find maximal cliques using JGraphT's Bron-KerboschCliqueFinder
    public static Iterator<Set<ProcSet>> findMaximalCliques(Graph<ProcSet, DefaultEdge> graph) {
        BronKerboschCliqueFinder<ProcSet,DefaultEdge> cliqueFinder = new BronKerboschCliqueFinder<>(graph);
        return cliqueFinder.iterator();
    }

    private Set<ProcSet> get_guilds(int index){
        return get_guilds(get_faulties(index));
    }
    private Set<ProcSet> get_guilds(ArrayList<Proc> faulties){
        ArrayList<Proc> wises = get_wise_procs(faulties);
        HashMap<Proc,ArrayList<ProcSet>> wise_qs = get_wise_quorums(wises);
        Queue<MarkedProcSet> guild_candidates = new LinkedList<>();
        ArrayList<ProcSet> guilds = new ArrayList<>();
        MarkedProcSet cur;
        for(Proc p : wises) {
            for (ProcSet p_set : wise_qs.get(p)) {
                cur = new MarkedProcSet(p_set);
                cur.add_p(p);
                cur.mark_p(p);
                guild_candidates.add(cur);
            }
        }

        Proc cur_p;
        MarkedProcSet clone;
        while(!guild_candidates.isEmpty()){
            cur = guild_candidates.remove();
            if(!cur.has_unmarked()){
                guilds.add(cur.get_p_set());
                continue;
            }
            cur_p = cur.get_unmarked();
            for(ProcSet p_set : wise_qs.get(cur_p)){
                clone = cur.clone();
                clone.add_p_set(p_set);
                clone.mark_p(cur_p);
                guild_candidates.add(clone);
            }

        }
        Set<ProcSet> all_guilds = computeUnions(guilds);
        remove_duplicates(all_guilds);
        return all_guilds;
    }


    private HashMap<Proc,ArrayList<ProcSet>> get_wise_quorums(ArrayList<Proc> wises){
        HashSet<Proc> wises_s = new HashSet<>(wises);
        HashMap<Proc,ArrayList<ProcSet>> ret = new HashMap<>();
        boolean all_wise;
        for(Proc p : wises){
            ret.put(p,new ArrayList<>());
            for(ProcSet p_set : asym_trust_system.get(p).get_quorums()){
                all_wise = true;
                for(Proc q : p_set){
                    if(!wises_s.contains(q)){
                        all_wise = false;
                        break;
                    }
                }
                if(all_wise){
                    ret.get(p).add(p_set);
                }
            }
        }
        return ret;
    }


    private ArrayList<Proc> get_wise_procs(ArrayList<Proc> faulties) {
        TrustSystem t_s;
        HashSet<Proc> f_set = new HashSet<>(faulties);
        ArrayList<Proc> wises = new ArrayList<>();
        for(Proc p : members){
            if(f_set.contains(p)){
                continue;
            }
            t_s = asym_trust_system.get(p);
            for(ProcSet p_set : t_s.get_fault_assumptions()){
                if(p_set.get_p_set().containsAll(f_set)){
                    wises.add(p);
                    break;
                }
            }
        }
        return wises;
    }

    private ArrayList<Proc> get_faulties(int index) {
        int size = members.size();
        String binaryString = String.format("%" + size + "s", Integer.toBinaryString(index)).replace(' ', '0');
        ArrayList<Proc> faultyProces = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (binaryString.charAt(i) == '1') {
                faultyProces.add(members.get(i)); // Assuming members contains Proces
            }
        }

        return faultyProces;

    }
}
