package recpbft;

import trustsystem.*;

import java.util.*;

public class EpochStatePredicateTracker implements PredicateTracker {
    private TreeSet<Tuple> treeSet;
    HashMap<Tuple, Tuple> tupleMap;
    HashMap<Tuple, MarkedProcSystem>  bind_quorums = new HashMap<>();
    HashMap<Tuple, MarkedProcSystem> bind_kernels = new HashMap<>();
    MarkedProcSystem bound_quorum;
    AsymmetricTrustSystem ats;
    Proc cur_leader;
    boolean leader_proposed = false;
    //these values being null when the predicate is satisfied means that an unbound quorum has been identified
    String bind_val = null;
    Tuple bind_state = null;
    ProcSet bind_q;

    HashSet<Proc> un_b = new HashSet<>();

    boolean satisfied = false;


    public EpochStatePredicateTracker(AsymmetricTrustSystem ats, Proc cur_leader) {
        this.treeSet = new TreeSet<>();
        this.tupleMap = new HashMap<>();
        this.ats = ats;
        this.cur_leader = cur_leader;
        bound_quorum = new ToleratedMarkedProcSystem(ats);
        leader_proposed = false;
    }
    public void leaderProposed(boolean b){
        leader_proposed = b;
    }
    public void reset(Proc cur_leader){
        this.cur_leader = cur_leader;
        this.treeSet = new TreeSet<>();
        this.tupleMap = new HashMap<>();
        bound_quorum.reset();
        satisfied = false;
        bind_val = null;
        HashSet<Proc> bind_q = new HashSet<>();
        leader_proposed = false;
    }
    public String getBindVal(){
        return bind_val;
    }
    public boolean isInvalid(String m){
        return new EpochState(m).isInvalid();
    }
    public void insert(Proc p, EpochState e_s){
        insert(p,e_s.getTs(),e_s.getVal(),e_s.getWs());
    }
    public void insert(Proc p, String m){
        insert(p,new EpochState(m));
    }
    //inserts a triple into the tracker structure.
    //returns true when the insertion leads to the satisfaction of the predicate
    public void insert(Proc p, int key, String value, HashMap<Integer,String> ws) {
        Tuple newTuple = new Tuple(key, value,p, ws);
        boolean contained = tupleMap.containsKey(newTuple);


        //check the unbound condition
        if(key==0&&value.equals("")||(leader_proposed&&p.equals(cur_leader))){
            bound_quorum.mark_proc(p);
            un_b.add(p);
            for(Tuple t : treeSet){
                bind_quorums.get(t).mark_proc(p);
            }
        }
        else{
            //if a tuple already exists for a received state then the
            if (contained) {
                //add the proc to the existing tuple
                Tuple existingTuple = tupleMap.get(newTuple);
                existingTuple.procs.add(p);
                existingTuple.writeSets.add(ws);
            } else {
                //if no matching tuple exists a new Tuple is created
                treeSet.add(newTuple);
                tupleMap.put(newTuple, newTuple);
                bind_quorums.put(newTuple,new ToleratedMarkedProcSystem(ats));
                bind_kernels.put(newTuple,new ToleratedKernelMarkedProcSystem(ats));
                MarkedProcSystem b_q =  bind_quorums.get(newTuple);
                for(Proc p2 : un_b){
                    b_q.mark_proc(p2);
                }
            }
            //check the kernel condition. For each Tuple check the condition for the new writeset and incase the tuple
            //is new also check vice versa
            for(Tuple t : treeSet){
                insert_kernel(t,ws,p);
                if(!contained){
                    insert_kernel(newTuple,t);
                }
            }
            //if newtuple is new, then every smaller tuple has to be marked
            if(!contained) {
                for (Tuple t : treeSet.headSet(newTuple, false)) {
                    for (Proc q : t.procs) {
                        bind_quorums.get(newTuple).mark_proc(q);
                    }
                }
            }
            //every larger tuple needs to insert the new Proc into its marked qourum
            for(Tuple t : treeSet.tailSet(newTuple,true)){
                bind_quorums.get(t).mark_proc(p);
            }

        }
        isSatisfied();
    }

    private boolean isSatisfied() {
        if(bound_quorum.getPSetFound()){
            satisfied = true;
            bind_q = bound_quorum.getQuorum();
            bind_state = Tuple.unbound();
            return true;
        }
        for(Tuple t : treeSet){
            if(bind_kernels.get(t).getPSetFound()&&bind_quorums.get(t).getPSetFound()){
                satisfied = true;
                bind_state = t;
                bind_val = t.value;
                bind_q = bind_kernels.get(t).getQuorum().union(bind_quorums.get(t).getQuorum());
                return true;
            }
        }
        return false;
    }

    public boolean satisfied(){
        return satisfied;
    }

    public ProcSet bind_set(){
        return bind_q;
    }

    public Tuple bindTuple(){
        return bind_state;
    }


    //the kernel condition is checked on every process in the existing Tuple
    private boolean insert_kernel(Tuple cur_tuple, Tuple existing){
        boolean bind_k_b = false;
        int count = 0;
        for(HashMap<Integer, String> ws : existing.writeSets) {
            bind_k_b = insert_kernel(cur_tuple,ws,existing.procs.get(count));
            count++;
        }
        return bind_k_b;
    }
    //p is marked as a kernel member for a state t if it has in its write set a write for the value in t with a larger
    //timestamp
    private boolean insert_kernel(Tuple t,HashMap<Integer, String> ws, Proc p){
        boolean bind_k_b = false;
        for (Map.Entry<Integer, String> entry : ws.entrySet()) {
            if (entry.getKey() >= t.key && entry.getValue().equals(t.value)) {
                bind_k_b = bind_kernels.get(t).mark_proc(p);
            }

        }
        return bind_k_b;
    }
    //p is marked as a kernel member for a state t if it has in its write set a write for the value in t with a larger
    //timestamp
    public static boolean insert_kernel(String v, int ts,HashMap<Integer, String> ws, Proc p,MarkedProcSystem kernels){
        boolean bind_k_b = false;
        for (Map.Entry<Integer, String> entry : ws.entrySet()) {
            if (entry.getKey() >= ts && entry.getValue().equals(v)) {
                bind_k_b = kernels.mark_proc(p);
            }

        }
        return bind_k_b;
    }

    public static class Tuple implements Comparable<Tuple> {
        int key;
        String value;
        ArrayList<Proc> procs;
        ArrayList<HashMap<Integer,String>> writeSets;

        //States of the replicas grouped by their time stamp and locked value.
        //The tuples are sorted by their time stamp.
        public Tuple(int key, String value,Proc p, HashMap<Integer,String> writeSet) {
            this.key = key;
            this.value = value;
            this.procs = new ArrayList<>();
            procs.add(p);
            this.writeSets = new ArrayList<>();
            writeSets.add(writeSet);
        }
        public Tuple(int key, String value) {
            this.key = key;
            this.value = value;
            this.procs = new ArrayList<>();
        }
        public static Tuple unbound(){
            return new Tuple(0,"");
        }

        public String getValue() {
            return value;
        }

        public int getKey() {
            return key;
        }

        public ArrayList<Proc> getProcs() {
            return procs;
        }

        @Override
        public int compareTo(Tuple other) {
            return Integer.compare(this.key, other.key);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Tuple tuple = (Tuple) obj;
            return key == tuple.key && Objects.equals(value, tuple.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

    }

}
