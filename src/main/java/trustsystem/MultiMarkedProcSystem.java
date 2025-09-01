package trustsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class MultiMarkedProcSystem extends ProcSystem {
    HashMap<String,Boolean> p_set_found = new HashMap<>();
    HashMap<String,HashMap<Proc,Boolean>> seen = new HashMap<>();
    HashMap<Proc, ArrayList<Integer>> p_set_membership = new HashMap<>();
    HashMap<String,HashMap<Integer,Integer>> counts = new HashMap<>();
    HashMap<Integer,Integer> sizes = new HashMap<>();
    HashSet<Proc> procs = new HashSet<>();
    MultiMarkedProcSystem(Collection<ProcSet> p_sets) {
        super(p_sets);
        int count = 0;
        for(ProcSet p_set : this.p_sets){
            sizes.put(count,p_set.size());
            for(Proc p : p_set){
                procs.add(p);
                if(!p_set_membership.containsKey(p)){
                    p_set_membership.put(p,new ArrayList<>());
                }
                p_set_membership.get(p).add(count);
            }
            count++;
        }
    }
    void reset(){
        p_set_found = new HashMap<>();
        counts = new HashMap<>();
        seen = new HashMap<>();
    }
    public boolean getPSetFound(String target){
        return p_set_found.getOrDefault(target,false);
    }
    //marks a process as seen and returns true when an entire ProcSet has been seen previously
    public boolean mark_proc(Proc p, String target){
        if(!p_set_found.containsKey(target)){
            p_set_found.put(target,false);
            seen.put(target, new HashMap<>());
            counts.put(target,new HashMap<>());
            for(Proc q : procs){
                seen.get(target).put(q,false);
            }
            for(int i = 0; i<sizes.size();i++){
                counts.get(target).put(i,0);
            }


        }
        boolean cur_p_set_found = p_set_found.get(target);
        HashMap<Proc,Boolean> cur_seen = seen.get(target);
        HashMap<Integer,Integer> cur_counts = counts.get(target);
        if(cur_p_set_found||!cur_seen.containsKey(p)||cur_seen.get(p)){
            return cur_p_set_found;
        }
        int temp;
        cur_seen.put(p,true);
        for(int index : p_set_membership.get(p)){
            temp = cur_counts.get(index)+1;
            cur_counts.put(index,temp);
            if(temp>=sizes.get(index)){
                p_set_found.put(target,true);
            }
        }
        return p_set_found.get(target);
    }
}
