package trustsystem;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ATSGenerator {

    private static final int INIT_PORT = 5000;

    public static class Index{
        ArrayList<Integer> indexes;
        public Index(ArrayList<Integer> indexes){
            this.indexes = new ArrayList<>(indexes);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Index)) return false;
            Index other = (Index) o;
            return Objects.equals(this.indexes, other.indexes);
        }
        public void insert(int index, int value){
            this.indexes.add(index, value);
        }

        public int get(int index){
            return indexes.get(index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(indexes);
        }
    }

    public static AsymmetricTrustSystem random_sub_threshold_system(HashSet<Proc> procs){
        ArrayList<Proc> procs_list = new ArrayList<>(procs);
        HashMap<Proc,TrustSystem> ats = new HashMap<>();
        HashSet<Proc> missed = null;
        int threshold = get_threshold(procs.size(), 3);
        Random random = new Random();
        for(Proc p : procs){
            ArrayList<ProcSet> p_sets = new ArrayList<>();
            missed = new HashSet<>(procs);
            while (!missed.isEmpty()){
                int inter = random.nextInt(threshold);
                Random_Selector r_s = new Random_Selector(procs.size());
                ArrayList<Integer> idxs  = r_s.sample(inter);
                HashSet<Proc> p_set = new HashSet<>();
                for(Integer idx : idxs){
                    p_set.add(procs_list.get(idx));
                }
                p_sets.add(new ProcSet(p_set));
                missed.removeAll(p_set);
            }
            ProcSystem.remove_subsets(p_sets);
            FaultSystem f_s = new FaultSystem(p_sets);
            QuorumSystem q_s = new QuorumSystem(f_s.complement(procs));
            ats.put(p,new TrustSystem(f_s,q_s));
        }
        return new AsymmetricTrustSystem(ats);

    }
    public static AsymmetricTrustSystem random_sub_threshold_system(int n){
        HashSet<Proc> procs = new HashSet<>();
        for(int i = 0; i<n ;i++){
            procs.add(new Proc(i,"127.0.0.1",INIT_PORT+i));
        }
        return random_sub_threshold_system(procs);
    }

    @Deprecated
    public static AsymmetricTrustSystem random_sub_hyperplane_system(ArrayList<Integer> dimensions){
        Random r = new Random();
        HashMap<Index,Proc> proc_map = new HashMap<>();
        //todo: fix this
        proc_map = new HashMap<>();//generate_proc_map(dimensions);
        HashSet<Proc> members = new HashSet<>(proc_map.values());
        HashMap<Proc,TrustSystem> ats = new HashMap<>();
        ArrayList<ProcSet> p_sets;
        for(Map.Entry<Index,Proc> entry : proc_map.entrySet()){
            p_sets = new ArrayList<>();
            int faults = r.nextInt(dimensions.size()+1);
            Random_Selector r_s = new Random_Selector(dimensions.size());
            ArrayList<Integer> samples = r_s.sample(faults);
            for(int sample : samples){
                int index = r.nextInt(dimensions.get(sample));
                ProcSet p_set = new ProcSet(get_hyper_plane(proc_map,sample,index));
                p_sets.add(p_set);
            }

            FaultSystem f_s = new FaultSystem(p_sets);
            QuorumSystem q_s = new QuorumSystem(f_s.complement(members));
            ats.put(entry.getValue(),new TrustSystem(f_s,q_s));
        }
        return new AsymmetricTrustSystem(ats);

    }

    private static class Random_Selector{
        int n;
        int cur;
        boolean[] selected;
        Random r = new Random();
        Random_Selector(int n){
            this.n = n;
            cur = n;
            selected = new boolean[n];
            for(int i = 0; i<selected.length;i++){
                selected[i] = false;
            }
        }
        public ArrayList<Integer> sample(int num){
            assert num <= cur;
            ArrayList<Integer> ret = new ArrayList<>();
            for(int i = 0; i<num;i++){
                ret.add(next());
            }
            return ret;
        }
        public int next(){
            int idx = r.nextInt(cur);
            int count1 = 0;
            int count2 = 0;
            while (count2<idx){
                if(!selected[count1]){
                    count2++;
                }
                if(count2<idx) {
                    count1++;
                }
            }
            selected[count1] = true;
            cur--;
            return count1;
        }


    }

    public static HashMap<Index, Proc> generate_proc_map(ArrayList<Integer> dimensions, ArrayList<Integer> td_array, HashMap<Proc, Integer> td_map) {
        HashMap<Index,Proc> map = new HashMap<>();
        generate_proc_map(dimensions,0, new ArrayList<>(), map, new AtomicInteger(0), td_array, td_map);
        return map;
    }

    private static void generate_proc_map(ArrayList<Integer> dimensions, int i, ArrayList<Integer> acc, HashMap<Index,Proc> map, AtomicInteger id_count, ArrayList<Integer> td_array, HashMap<Proc, Integer> td_map) {
        for(int d = 0 ; d<dimensions.get(i);d++){
            acc.add(d);
            if(i < dimensions.size()-1) {
                generate_proc_map(dimensions, i + 1, acc, map, id_count,td_array,td_map);
            }
            else{
                int id = id_count.getAndIncrement();
                Proc n_p = new Proc(id,"127.0.0.1",INIT_PORT+id);
                map.put(new Index(acc),n_p);
                td_map.put(n_p,td_array.get(id));
            }
            acc.remove(acc.size()-1);
        }
    }

    private static HashSet<Proc> get_hyper_plane(HashMap<Index,Proc> map, int dim, int index){
        HashSet<Proc> ret = new HashSet<>();
        for(Map.Entry<Index,Proc> entry : map.entrySet()){
            if(entry.getKey().indexes.get(dim)==index){
                ret.add(entry.getValue());
            }
        }
        return ret;
    }

    public static List<Integer> comp_subs(List<Integer> li, int n){
        List<Integer> ret = new ArrayList<>();
        for(int i = 0 ; i < n ; i++){
            if(!li.contains(i)){
                ret.add(i);
            }
        }
        return ret;
    }

    public static AsymmetricTrustSystem hyperplane_system(ArrayList<Integer> dimensions, ArrayList<Integer> trust_dim){
        HashMap<Proc, Integer> td_map = new HashMap<>();
        HashMap<Index, Proc> proc_map = generate_proc_map(dimensions,trust_dim,td_map);
        HashSet<Proc> members = new HashSet<>(proc_map.values());
        HashMap<Proc,TrustSystem> t_s_map = new HashMap<>();
        for(Proc q : members){
            t_s_map.put(q,t_s_by_dim(dimensions,td_map.get(q),proc_map));
        }
        return new AsymmetricTrustSystem(t_s_map);
    }

    private static TrustSystem t_s_by_dim(ArrayList<Integer> dimensions, int t_d, HashMap<Index, Proc> proc_map) {
        ArrayList<ProcSet> f_s = new ArrayList<>();
        //size of the trusted dimension
        int cur_d = dimensions.get(t_d);
        //number of fully failing values of the trusted dimension
        int tot_cap = get_threshold(cur_d,3);
        //all possible values that can fully fail together
        List<List<Integer>> t_dims = generateSubsets(cur_d,tot_cap);
        ArrayList<ArrayList<HashSet<Proc>>> combinations = new ArrayList<>();
        for(List<Integer> t_dim : t_dims){
            combinations = new ArrayList<>();
            //set of partially failing processes for the current set t_dim of fully failing prosesses
            List<Integer> comp = comp_subs(t_dim,cur_d);
            //generate complete failure set
            for(Integer i : t_dim){
                ArrayList<HashSet<Proc>> temp = new ArrayList<>();
                temp.add(get_hyper_plane(proc_map,t_d,i));
                combinations.add(temp);
            }
            for(Integer i : comp){
                combinations.add(all_sub_grids(proc_map,dimensions,t_d,i));
            }
            f_s.addAll(all_combinations(combinations));
        }
        FaultSystem fault_s = new FaultSystem(f_s);
        QuorumSystem q_s = fault_s.canonical_quorum_system(new HashSet<>(proc_map.values()));
        return new TrustSystem(fault_s,q_s);
    }

    private static ArrayList<ProcSet> all_combinations(ArrayList<ArrayList<HashSet<Proc>>> combinations) {
        ArrayList<ProcSet> ret = new ArrayList<>();
        all_combinations(combinations,0,new HashSet<>(),ret);
        return ret;
    }

    private static void all_combinations(ArrayList<ArrayList<HashSet<Proc>>> combinations, int start, HashSet<Proc> current, ArrayList<ProcSet> result) {
        if(start == combinations.size()){
            result.add(new ProcSet(current));
            return;
        }
        if(combinations.get(start).size()==0){
            all_combinations(combinations, start + 1, current, result);
        }
        for(int i = 0; i < combinations.get(start).size(); i++){
                current.addAll(combinations.get(start).get(i));
                all_combinations(combinations, start + 1, current, result);
                current.removeAll(combinations.get(start).get(i));

        }

    }

    private static ArrayList<HashSet<Proc> > all_sub_grids(HashMap<Index,Proc> proc_map, ArrayList<Integer> dimensions, int t_d, Integer index) {
     int k;
     //for every dimension different then the trust dimension select all coordinates at which failures can occur and
     //store it in the list all_subs where the first index selects the dimension and the second index
     // select the set of coordinates for a failure at that dimension.
     ArrayList<List<List<Integer>>> all_subs = new ArrayList<>();
     for(int i = 0 ; i < dimensions.size();i++){
         if(i == t_d){
             ArrayList<Integer> temp = new ArrayList<>();
             temp.add(index);
             ArrayList<List<Integer>> temp_temp = new ArrayList<>();
             temp_temp.add(temp);
             all_subs.add(temp_temp);
             continue;
         }
         k = get_threshold(dimensions.get(i),6);
         all_subs.add(generateSubsets(dimensions.get(i),k));
     }
     return all_f_sets(all_subs,proc_map);
    }

    public static int get_threshold(int n, int ratio){
        if(n%ratio == 0){
            return n/ratio -1;
        }
        else {
            return n/ratio;
        }
    }



    public static ArrayList<HashSet<Proc>> all_f_sets(List<List<List<Integer>>> points, HashMap<Index,Proc> proc_map){
        HashMap<Index, HashSet<Proc>> result = new HashMap<>();
        ArrayList<HashSet<Proc>> f_sets = new ArrayList<>();
        all_points(points, proc_map,0, new ArrayList<>(), new ArrayList<>(), result);
        for(Map.Entry<Index,HashSet<Proc>> entry : result.entrySet()){
            HashSet<Proc> set = entry.getValue();
            f_sets.add(set);
        }
        return f_sets;
    }

    private static void all_points(List<List<List<Integer>>> points, HashMap<Index, Proc> proc_map, int start, ArrayList<Integer> current, ArrayList<Integer> curr_path, HashMap<Index,HashSet<Proc>> result) {
        if(start >= points.size()){
            Index index = new Index(curr_path);
            Index index2 = new Index(current);
            if(!result.containsKey(index)){
                result.put(index,new HashSet<>());
            }
            result.get(index).add(proc_map.get(index2));
            return;
        }
        for(int i = 0; i < points.get(start).size(); i++){
            for(int j = 0; j < points.get(start).get(i).size(); j++){
                current.add(points.get(start).get(i).get(j));
                curr_path.add(i);
                all_points(points,proc_map, start + 1, current,curr_path, result);
                current.remove(current.size()-1);
            }
        }

    }


    public static List<List<Integer>> generateSubsets(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(0, n, k, new ArrayList<>(), result);
        return result;

    }

    private static void backtrack(int start, int n, int k, List<Integer> current, List<List<Integer>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < n; i++) {
            current.add(i);
            backtrack(i + 1, n, k, current, result);
            current.remove(current.size() - 1);
        }
    }
}
