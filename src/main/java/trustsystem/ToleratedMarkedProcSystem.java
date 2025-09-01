package trustsystem;

import java.util.Collection;
import java.util.HashSet;

public class ToleratedMarkedProcSystem extends MarkedProcSystem {
    AsymmetricTrustSystem ats;
    HashSet<Proc> seen = new HashSet<>();
    public ToleratedMarkedProcSystem(AsymmetricTrustSystem ats) {
        this.ats = ats;

    }

    @Override
    public boolean getPSetFound() {
        return super.getPSetFound();
    }
    @Override
    public void reset(){
        p_set_found = false;
        seen = new HashSet<>();
        found_quorum = null;
    }
    @Override
    public boolean mark_proc(Proc p){
        if(seen.contains(p)||p_set_found){
            return p_set_found;
        }
        seen.add(p);
        HashSet<Proc> g = contains_guild(seen);
        if(!g.isEmpty()){
            found_quorum = new ProcSet(g);
            p_set_found = true;
        }
        return p_set_found;

    }
    HashSet<Proc> contains_guild(HashSet<Proc> set){
        HashSet<Proc> ret = new HashSet<>(set);
        boolean repeat = true;
        Proc to_remove = null;
        while (repeat){
            repeat = false;
            for(Proc p : ret){
                if(!has_guild_quorum(p,ret)){
                    to_remove = p;
                    repeat = true;
                    break;
                }
            }
            if(repeat){
                ret.remove(to_remove);
            }
        }
        return ret;
    }
    boolean has_guild_quorum(Proc p, HashSet<Proc> set){
        for(ProcSet p_s : ats.getTrustSystem(p).get_quorums()){
            if(set.containsAll(p_s.get_p_set())){
                return true;
            }
        }
        return false;
    }


}
