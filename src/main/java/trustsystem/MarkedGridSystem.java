package trustsystem;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class MarkedGridSystem extends  MarkedProcSystem {
    HashMap<ATSGenerator.Index, Proc> proc_map;
    ArrayList<Integer> dims;
    Integer t_d;
    HashSet<Proc> procs;
    HashSet<Proc> seen;
    HashMap<Integer,Proc> id_proc_map = new HashMap<>();
    HashMap<Integer, ATSGenerator.Index> id_index_map = new HashMap<>();
    //takes value to map mapping dimension to set of values of that dim seen
    HashMap<Integer,HashMap<Integer, HashSet<Integer>>> tracker = new HashMap();
    HashMap<Integer, Boolean> satisfied = new HashMap<>();
    MarkedGridSystem(ArrayList<Integer> dims, Integer t_d, HashSet<Proc> procs) {
        super(new HashSet<>());
        this.dims = dims;
        this.t_d = t_d;
        this.procs = procs;
        for(Proc p : procs){
            id_proc_map.put(p.getId(),p);
        }
        generate_proc_map();
        for(int i = 0; i< dims.get(t_d);i++){
            tracker.put(i,new HashMap<>());
            satisfied.put(i,false);
            HashMap<Integer,HashSet<Integer>> map = tracker.get(i);
            for(int j = 0; j<dims.size();j++){
                if(j != t_d) {
                    map.put(j,new HashSet<>());
                }
            }
        }
    }
    @Override
    public boolean mark_proc(Proc p){
        if(seen.contains(p)){
            return p_set_found;
        }
        seen.add(p);
        if(p_set_found){
            return p_set_found;
        }
        ATSGenerator.Index ind = id_index_map.get(p.getId());
        int t_d_v = ind.get(t_d);
        if(satisfied.get(t_d_v)){
            return p_set_found;
        }
        HashMap<Integer,HashSet<Integer>> cur_tracker = tracker.get(t_d_v);
        HashSet<Integer> cur_s;
        boolean val_sat = true;
        for(int i = 0; i<dims.size();i++){
            if(i == t_d){
                continue;
            }
            cur_s = cur_tracker.get(i);
            cur_s.add(ind.get(i));
            if(cur_s.size()<get_threshold(dims.get(i),6)){
                val_sat = false;
            }
        }
        satisfied.put(t_d_v,val_sat);
        int sat_c = 0;
        for(Integer i : satisfied.keySet()){
            if(satisfied.get(i)){
                sat_c++;
            }
        }
        p_set_found = sat_c >= get_threshold(dims.get(t_d),3);
        return p_set_found;
    }

    public static int get_threshold(int n, int ratio){
        int num;
        if(n%ratio == 0){
            num =  n/ratio -1;
        }
        else {
            num =  n/ratio;
        }
        return n - num;

    }
    @Override
    public void reset(){
        p_set_found = false;
        tracker = new HashMap<>();
        for(int i = 0; i< dims.get(t_d);i++){
            tracker.put(i,new HashMap<>());
            satisfied.put(i,false);
            HashMap<Integer,HashSet<Integer>> map = tracker.get(i);
            for(int j = 0; j<dims.size();j++){
                if(j != t_d) {
                    map.put(j,new HashSet<>());
                }
            }
        }
    }
    @Override
    //todo
    public ProcSet getQuorum(){
        return found_quorum;
    }

    private void generate_proc_map() {
        generate_proc_map(0, new ArrayList<>(), new AtomicInteger(0));
    }

    private void generate_proc_map( int i, ArrayList<Integer> acc, AtomicInteger id_count) {
        for(int d = 0 ; d<dims.get(i);d++){
            if(d != 0){
                acc.remove(acc.size()-1);
            }
            acc.add(d);
            if(i < dims.size()-1) {
                generate_proc_map(i + 1, acc, id_count);
            }
            else{
                int id = id_count.getAndIncrement();
                ATSGenerator.Index index = new ATSGenerator.Index(acc);
                proc_map.put(index, id_proc_map.get(id));
                id_index_map.put(id,index);
            }
        }
    }
}
