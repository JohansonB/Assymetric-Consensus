package trustsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
            for(ProcSet f : p1_fs.get_p_sets()){
                for(ProcSet q : p1_qs.get_p_sets()){
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


    private ArrayList<Proc> get_wise_Proces(ArrayList<Proc> faulties) {
        TrustSystem t_s;
        HashSet<Proc> f_set = new HashSet<>(faulties);
        ArrayList<Proc> wises = new ArrayList<>();
        for(Proc p : members){
            if(f_set.contains(p)){
                continue;
            }
            t_s = asym_trust_system.get(p);
            for(ProcSet p_set : t_s.get_fault_assumptions().get_p_sets()){
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
