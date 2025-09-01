package trustsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class ProcSystem implements Iterable<ProcSet>  {
    ArrayList<ProcSet> p_sets;

    ProcSystem(){
        p_sets = new ArrayList<>();
    }

    ProcSystem( Collection<ProcSet> p_sets){
        this.p_sets = new ArrayList<>(p_sets);
    }


    public int size(){
        return p_sets.size();
    }

    public ProcSet get(int index) {
        return p_sets.get(index);
    }

    public ArrayList<ProcSet> get_p_sets() {
        return p_sets;
    }

    public MarkedProcSystem get_marked(){
        return new MarkedProcSystem(p_sets);
    }

    public MultiMarkedProcSystem get_multi_marked(){
        return new MultiMarkedProcSystem(p_sets);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for(ProcSet p_set : p_sets){
            sb.append(p_set.toString()).append(" ");
        }
        String result = sb.toString().trim();
        return result + "}";
    }
    public static ProcSystem parse(String code){
        code = code.trim();
        code = code.substring(1, code.length() - 1);
        ArrayList<ProcSet> procSets = new ArrayList<>();
        ArrayList<String> procset_codes = TrustSystemSerializer.flatten_brackets(code);
        for(String s : procset_codes){
            procSets.add(ProcSet.parse(s));
        }
        return new ProcSystem(procSets);
    }
    public static void removeSupersets(ArrayList<ProcSet> proc_sets) {
        ArrayList<ProcSet> toRemove = new ArrayList<>();

        for (int i = 0; i < proc_sets.size(); i++) {
            ProcSet set1 = proc_sets.get(i);

            for (int j = i+1; j < proc_sets.size(); j++) {
                ProcSet set2 = proc_sets.get(j);

                if (set1.get_p_set().containsAll(set2.get_p_set())) {
                    toRemove.add(set1);
                    break;
                }
            }
        }
        proc_sets.removeAll(toRemove);
    }
    public static void remove_subsets(ArrayList<ProcSet> proc_sets) {
        ArrayList<ProcSet> toRemove = new ArrayList<>();

        for (int i = 0; i < proc_sets.size(); i++) {
            ProcSet set1 = proc_sets.get(i);

            for (int j = i+1; j < proc_sets.size(); j++) {
                ProcSet set2 = proc_sets.get(j);

                if (set2.get_p_set().containsAll(set1.get_p_set())) {
                    toRemove.add(set1);
                    break;
                }
            }
        }
        proc_sets.removeAll(toRemove);
    }
    public ProcSystem complement(HashSet<Proc> members){
        ArrayList<ProcSet> ret = new ArrayList<>();
        for(ProcSet p_set : this){
            HashSet<Proc> cur = new HashSet<>(members);
            cur.removeAll(p_set.get_p_set());
            ret.add(new ProcSet(cur));
        }
        return new ProcSystem(ret);
    }

    @Override
    public Iterator<ProcSet> iterator() {
        return p_sets.iterator();
    }


}
