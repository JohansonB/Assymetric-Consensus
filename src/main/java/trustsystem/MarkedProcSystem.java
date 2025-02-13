package trustsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class MarkedProcSystem extends ProcSystem{
    boolean p_set_found = false;
    HashMap<Proc,Boolean> seen = new HashMap<>();
    HashMap<Proc, ArrayList<Integer>> p_set_membership = new HashMap<>();
    HashMap<Integer,Integer> counts = new HashMap<>();
    HashMap<Integer,Integer> sizes = new HashMap<>();
    MarkedProcSystem(Collection<ProcSet> p_sets) {
        super(p_sets);
        int count = 0;
        for(ProcSet p_set : this.p_sets){
            counts.put(count,0);
            sizes.put(count,p_set.size());
            for(Proc p : p_set){
                if(!seen.containsKey(p)){
                    seen.put(p,false);
                }
                if(!p_set_membership.containsKey(p)){
                    p_set_membership.put(p,new ArrayList<>());
                }
                p_set_membership.get(p).add(count);
            }
            count++;
        }
    }
    void reset(){
        p_set_found = false;
        for(int i = 0; i<counts.size();i++){
            counts.put(i,0);
        }
    }
    public boolean getPSetFound(){
        return p_set_found;
    }
    //marks a process as seen and returns true when an entire ProcSet has been seen previously
    public boolean mark_proc(Proc p){
        if(p_set_found||!seen.containsKey(p)||seen.get(p)){
            return p_set_found;
        }
        int temp;
        seen.put(p,true);
        for(int index : p_set_membership.get(p)){
            temp = counts.get(index)+1;
            counts.put(index,temp);
            if(temp>=sizes.get(index)){
                p_set_found = true;
            }
        }
        return p_set_found;
    }
}
