package trustsystem;


import utils.SerializerTools;

import java.util.*;

public class AsymmetricTrustSystem {
    HashMap<Proc,TrustSystem> asym_trust_system;
    ArrayList<Proc> members;

    AsymmetricTrustSystem(HashMap<Proc,TrustSystem> asym_trust_system){
        this.asym_trust_system = asym_trust_system;
        this.members = new ArrayList<>(asym_trust_system.keySet());
    }
    private void set_members(){
        members = new ArrayList<>(asym_trust_system.keySet());
    }

    public TrustSystem getTrustSystem(Proc p){
        return asym_trust_system.get(p);
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

    public Set<Proc> getMembers() {
        return asym_trust_system.keySet();
    }

    public interface Selector{
        GuildCandidate next();
        boolean isEmpty();
        void add(GuildCandidate c);
    }
    public class GreedySelector implements Selector{
        HashMap<Integer,LinkedList<GuildCandidate>> candidates = new HashMap<>();
        TreeSet<Integer> sorted_keys = new TreeSet<>();
        @Override
        public GuildCandidate next() {
            int key = sorted_keys.first();
            LinkedList<GuildCandidate> temp = candidates.get(key);
            GuildCandidate ret = temp.pop();
            if(temp.isEmpty()){
                sorted_keys.remove(key);
            }
            return ret;
        }

        @Override
        public boolean isEmpty() {
            return sorted_keys.isEmpty();
        }

        @Override
        public void add(GuildCandidate c) {
            sorted_keys.add(c.unchecked.size());
            candidates.computeIfAbsent(c.unchecked.size(),k->new LinkedList<>()).add(c);

        }
    }
    private static class GuildCandidate{
        HashSet<Proc> unchecked = new HashSet<>();
        HashSet<Proc> checked = new HashSet<>();
        ProcSet p_set;
        GuildCandidate(ProcSet innit){
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

        public GuildCandidate clone() {
            GuildCandidate cloned = new GuildCandidate(p_set);
            cloned.unchecked = new HashSet<>(unchecked);
            cloned.checked = new HashSet<>(checked);
            return cloned;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            GuildCandidate other = (GuildCandidate) obj;
            return Objects.equals(p_set.get_p_set(), other.p_set.get_p_set()) &&
                    Objects.equals(checked, other.checked);
        }

        @Override
        public int hashCode() {
            return Objects.hash(p_set.get_p_set(), checked);
        }

    }
    public AsymmetricTrustSystem permute(){
        return permute(random_permutation(members));
    }

    public AsymmetricTrustSystem permute(HashMap<Proc,Proc> permutation){
        HashMap<Proc,TrustSystem> temp = new HashMap<>();
        for(Map.Entry<Proc,TrustSystem> e : asym_trust_system.entrySet()){
            temp.put(permutation.get(e.getKey()),e.getValue().permute(permutation));
        }
        return new AsymmetricTrustSystem(temp);
    }

    private HashSet<ProcSet> get_guilds(Selector s){
        HashSet<GuildCandidate> seen = new HashSet<>();
        HashSet<ProcSet> guilds = new HashSet<>();
        GuildCandidate cur;
        for(Proc p : members) {
            for (ProcSet p_set : asym_trust_system.get(p).get_quorums()) {
                cur = new GuildCandidate(p_set);
                cur.add_p(p);
                cur.mark_p(p);
                if(!seen.contains(cur)) {
                    seen.add(cur);
                    s.add(cur);
                }
            }
        }

        Proc cur_p;
        GuildCandidate clone;
        boolean cont = false;
        while(!s.isEmpty()){
            //check if the guildcandidate contains an already existing guild
            cur = s.next();
            for(ProcSet guild : guilds){
                cont = false;
                if (cur.get_p_set().p_set.containsAll(guild.get_p_set())){
                    cont = true;
                    break;
                }
            }
            if(cont)
                continue;

            if(!cur.has_unmarked()){
                drop_supersets(guilds,cur.get_p_set());
                guilds.add(cur.get_p_set());
                continue;
            }
            cur_p = cur.get_unmarked();
            for(ProcSet p_set : asym_trust_system.get(cur_p).get_quorums()){
                clone = cur.clone();
                clone.add_p_set(p_set);
                clone.mark_p(cur_p);
                if(!seen.contains(clone)) {
                    seen.add(clone);
                    s.add(clone);
                }
            }

        }
        return guilds;
    }

    private void drop_supersets(HashSet<ProcSet> guilds, ProcSet p_set) {
        HashSet<ProcSet> to_remove = new HashSet<>();
        for(ProcSet p_s : guilds){
            if(p_s.get_p_set().containsAll(p_set.get_p_set())){
                to_remove.add(p_s);
            }
        }
        guilds.removeAll(to_remove);
    }

    public ToleratedSystem get_tolerated_system(){
        return get_tolerated_system(new GreedySelector());
    }
    public ToleratedSystem get_tolerated_system(Selector s){
        return new ToleratedSystem(new FaultSystem(new HashSet<>()),new QuorumSystem(get_guilds(s)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<Proc, TrustSystem> entry : asym_trust_system.entrySet()) {
            if (!first) sb.append(" ");
            first = false;
            sb.append(entry.getKey().toString()).append(entry.getValue().toString());
        }
        sb.append("}");
        return sb.toString();
    }
    public static AsymmetricTrustSystem parse(String code) {
        code = code.trim();

        if (code.length() < 2 || code.charAt(0) != '{' || code.charAt(code.length() - 1) != '}') {
            throw new IllegalArgumentException("Invalid format for AsymmetricTrustSystem");
        }
        code = code.substring(1, code.length() - 1);
        // Extract `{Proc}` and `{TrustSystem}` pairs using flatten_brackets
        ArrayList<String> entries = SerializerTools.flatten_outer_brackets(code);

        HashMap<Proc, TrustSystem> asym_trust_system = new HashMap<>();

        for (int i = 0; i < entries.size(); i += 2) {
            if (i + 1 >= entries.size()) {
                throw new IllegalArgumentException("Unmatched Proc and TrustSystem entries in: " + code);
            }

            // Extract and parse Proc
            Proc proc = Proc.parse(entries.get(i));

            // Extract and parse TrustSystem
            TrustSystem trustSystem = TrustSystem.parse(entries.get(i + 1));

            asym_trust_system.put(proc, trustSystem);
        }

        return new AsymmetricTrustSystem(asym_trust_system);
    }

    public static AsymmetricTrustSystem symmetric_threshold_system(Collection<Proc> procs, int threshold){
        HashMap<Proc,TrustSystem> map = new HashMap<>();
        for(Proc p : procs) {
            map.put(p,TrustSystem.threshold_system(procs,threshold));
        }
        return new AsymmetricTrustSystem(map);
    }
    public static AsymmetricTrustSystem symmetric_threshold_system(Collection<Proc> procs){
        HashMap<Proc,TrustSystem> map = new HashMap<>();
        for(Proc p : procs) {
            map.put(p,TrustSystem.threshold_system(procs));
        }
        return new AsymmetricTrustSystem(map);
    }

    public static HashMap<Proc,Proc> random_permutation(Collection<Proc> procs){
        HashMap<Proc,Proc> ret = new HashMap<>();
        ArrayList<Proc> l1 = new ArrayList<>(procs);
        ArrayList<Proc> l2 = new ArrayList<>(l1);
        Collections.shuffle(l2);;
        for(int i = 0; i< l1.size();i++){
            ret.put(l1.get(i),l2.get(i));
        }
        return ret;
    }

    public static void main(String[] args){
        Proc p1 = new Proc(0, "127.0.0.1", 5000);
        Proc p2 = new Proc(1, "127.0.0.1", 5001);
        Proc p3 = new Proc(2, "127.0.0.1", 5002);
        Proc p4 = new Proc(3, "127.0.0.1", 5003);
        Proc p5 = new Proc(4, "127.0.0.1", 5004);

        ArrayList<Proc> peers = new ArrayList<>();
        peers.add(p1);
        peers.add(p2);
        peers.add(p3);
        peers.add(p4);
        //peers.add(p5);
        AsymmetricTrustSystem a_t_s = symmetric_threshold_system(peers);
        ToleratedSystem tol = a_t_s.get_tolerated_system();
        AsymmetricTrustSystem clone = AsymmetricTrustSystem.parse(a_t_s.toString());
        System.out.println(clone);
    }

    public String name(){
        return "";
    }



}
