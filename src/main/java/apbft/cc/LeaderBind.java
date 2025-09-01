package apbft.cc;

import pbft.EpochState;
import pbft.EpochStatePredicateTracker;
import pbft.PredicateTracker;
import trustsystem.MarkedProcSystem;
import trustsystem.Proc;
import trustsystem.ProcSet;
import trustsystem.TrustSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static pbft.EpochStatePredicateTracker.insert_kernel;

public class LeaderBind implements PredicateTracker {
    //implements the leaderbind predicate. The leader needs to detect a Quorum of processes that binds to a subset of
    //received states.
    //for this each received process becomes an associated bindTracker, that is updateted with all received processes states.
    //Once the bindTracker detects a bind, the process of that tracker is added to the qourumTracker of the leader.


    TrustSystem t_s;
    Proc cur_leader;
    HashMap<Proc, EpochStatePredicateTracker> bind_trackers = new HashMap<>();
    HashMap<Proc, ProcSet> bind_map = new HashMap<>();
    HashMap<Proc, EpochStatePredicateTracker.Tuple> state_map = new HashMap<>();
    HashMap<Proc,MarkedProcSystem> bind_certificate = new HashMap<>();
    HashMap<Proc,String> received;
    MarkedProcSystem leader_bind;
    boolean satisfied = false;
    HashSet<Proc> Q_l;
    HashMap<Proc,Boolean> been_sat = new HashMap<>();

    boolean l_proposed = false;

    String bind_val;
    int bind_ts = -2;

    @Override
    public void insert(Proc p, String m) {
        if(!bind_trackers.keySet().contains(p)){
            EpochStatePredicateTracker b_t = new EpochStatePredicateTracker(t_s,cur_leader);
            for(Map.Entry<Proc,String> entry : received.entrySet()){
                b_t.insert(entry.getKey(),entry.getValue());
            }
            b_t.leaderProposed(l_proposed);
            bind_trackers.put(p,b_t);
        }
        ArrayList<Proc> to_remove = new ArrayList<>();
        for(Map.Entry<Proc,EpochStatePredicateTracker> tracker : bind_trackers.entrySet()){
            if(!tracker.getValue().satisfied())
                tracker.getValue().insert(p, m);
            if(tracker.getValue().satisfied()){
                if(!been_sat.get(tracker.getKey())) {
                    been_sat.put(tracker.getKey(),true);
                    bind_map.put(tracker.getKey(), tracker.getValue().bind_set());
                    state_map.put(tracker.getKey(), tracker.getValue().bindTuple());
                    bind_certificate.put(tracker.getKey(), t_s.get_quorums().get_kernel_system().get_marked());
                    for(Map.Entry<Proc,String> state : received.entrySet()){
                        EpochState e_s = new EpochState(state.getValue());
                        insert_kernel(bind_val,bind_ts,e_s.getWs(),tracker.getKey(),bind_certificate.get(tracker.getKey()));
                    }
                }
                EpochState c_s = new EpochState(m);
                insert_kernel(bind_val,bind_ts,c_s.getWs(),p,bind_certificate.get(tracker.getKey()));
                leader_bind.mark_proc(tracker.getKey());
                to_remove.add(p);
            }
            received.put(p,m);
        }
        for(Proc q : to_remove){
            bind_trackers.remove(q);
        }
        if(leader_bind.getPSetFound()){
            Q_l = leader_bind.getQuorum().get_p_set();
            satisfied = true;
            //get highest bind
            String max_value = null;
            int max_ts = -2;
            for(Map.Entry<Proc, EpochStatePredicateTracker.Tuple> entry : state_map.entrySet()){
                if(entry.getValue().getKey()>max_ts){
                    max_ts = entry.getValue().getKey();
                    max_value = entry.getValue().getValue();
                }
            }
            bind_val = max_value;
            bind_ts = max_ts;


        }

    }

    @Override
    public boolean isInvalid(String m) {
        return new EpochState(m).isInvalid();
    }

    @Override
    public void reset(Proc leader) {

    }

    @Override
    public void leaderProposed(boolean b) {
        l_proposed = b;
        for(Map.Entry<Proc,EpochStatePredicateTracker> tracker : bind_trackers.entrySet()) {
            tracker.getValue().leaderProposed(l_proposed);
        }
    }

    public String getBindVal() {
        return bind_val;
    }

    public int getBindTs() {
        return bind_ts;
    }

    public HashSet<Proc> getQ_l() {
        return Q_l;
    }

    @Override
    public boolean satisfied() {
        return satisfied;
    }
}
