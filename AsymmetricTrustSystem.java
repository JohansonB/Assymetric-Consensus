import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AsymmetricTrustSystem {
    HashMap<Process,TrustSystem> asym_trust_system;
    ArrayList<Process> members;

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
        Process p1;
        Process p2;
        ProcessSet p1_q;
        ProcessSet p2_q;
        ProcessSet p1_f;
        ProcessSet p2_f;
        HashSet<Process> intersection_q;
        HashSet<Process> intersection_f;

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
        HashSet<Process> intersect;
        for(Process p : members){
            t_s_p1 = asym_trust_system.get(p);
            p1_fs = t_s_p1.get_fault_assumptions();
            p1_qs = t_s_p1.get_quorums();
            for(ProcessSet f : p1_fs.get_p_sets()){
                for(ProcessSet q : p1_qs.get_p_sets()){
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
    public ToleratedSystem get_tolerated_system(){
        ArrayList<Process> faulties;
        ArrayList<Process> wises;
        ArrayList<ProcessSet> guilds;
        //array that remembers what faulty executions have been considered.
        //the binary representation of the array index encodes the faulty processes of the execution
        boolean[] bookkeeping = new boolean[(int)Math.pow(2,members.size())];
        for(int i = 0; i<bookkeeping.length;i++){
            if(!bookkeeping[i]){
                faulties = get_faulties(i);
                wises = get_wise_processes(faulties);
                guilds = get_guilds(wises);
            }
        }
    }

    private ArrayList<ProcessSet> get_guilds(ArrayList<Process> wises) {
        HashSet<Process> seen_wises = new HashSet<>();
        ArrayList<ProcessSet> guild_candidates = new ArrayList<>();
        ArrayList<ProcessSet> to_remove;
        ArrayList<ProcessSet> to_add;
        ArrayList<ProcessSet> wise_quorums;
        ProcessSet copy_ps;
        HashSet<Process> copy_set;
        for(Process wise : wises){
            //check if the process is in a current candidate guild. If so check if the processe satisfies the guild
            //property and extend the candidate guild if needed
            to_remove = new ArrayList<>();
            to_add = new ArrayList<>();
            wise_quorums = get_wise_quorums(wise);
            for(ProcessSet g_c : guild_candidates){
                if(g_c.contains(wise)){
                    //when wise quorums is empty this means that wise cannot be part of a guild therefore g_c is an
                    //invalid candidate otherwise the candidate has to be extended by the wise quorums of wise if the
                    // wise quorum does not contain an already seen wise process not already contained in g_c
                    to_remove.add(g_c);
                    for(ProcessSet w_q : wise_quorums){
                        copy_set = new HashSet<>(w_q.get_p_set());
                        copy_set.removeAll(g_c.get_p_set());
                        copy_set.retainAll(seen_wises);
                        if(!copy_set.isEmpty()){
                            continue;
                        }
                        copy_ps = new ProcessSet(new HashSet<>(g_c.get_p_set()));
                        copy_ps.merge(w_q);
                        to_add.add(copy_ps);
                    }

                }
            }
        }
    }

    private ArrayList<Process> get_wise_processes(ArrayList<Process> faulties) {
        TrustSystem t_s;
        HashSet<Process> f_set = new HashSet<>(faulties);
        ArrayList<Process> wises = new ArrayList<>();
        for(Process p : members){
            if(f_set.contains(p)){
                continue;
            }
            t_s = asym_trust_system.get(p);
            for(ProcessSet p_set : t_s.get_fault_assumptions().get_p_sets()){
                if(p_set.get_p_set().containsAll(f_set)){
                    wises.add(p);
                    break;
                }
            }
        }
        return wises;
    }

    private ArrayList<Process> get_faulties(int index) {
        int size = members.size();
        String binaryString = String.format("%" + size + "s", Integer.toBinaryString(index)).replace(' ', '0');
        ArrayList<Process> faultyProcesses = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (binaryString.charAt(i) == '1') {
                faultyProcesses.add(members.get(i)); // Assuming members contains Processes
            }
        }

        return faultyProcesses;

    }
}
