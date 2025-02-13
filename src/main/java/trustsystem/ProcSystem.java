package trustsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ProcSystem implements Iterable<ProcSet>  {
    ArrayList<ProcSet> p_sets;

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

    @Override
    public Iterator<ProcSet> iterator() {
        return p_sets.iterator();
    }


}
