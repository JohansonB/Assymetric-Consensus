package trustsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class ProcSet implements Iterable<Proc> {
    HashSet<Proc> p_set;
    public ProcSet(Collection<Proc> p_set){
        this.p_set = new HashSet<>(p_set);
    }
    public HashSet<Proc> get_p_set(){
        return p_set;
    }
    public boolean contains(Proc p){
        return p_set.contains(p);
    }
    public int size(){
        return p_set.size();
    }
    public boolean isEmpty(){
        return p_set.isEmpty();
    }

    public void merge(ProcSet other) {
        p_set.addAll(other.get_p_set());
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for(Proc p : p_set){
            sb.append(p.toString()).append(" ");
        }
        String result = sb.toString().trim();
        return result + "}";
    }

    public static ProcSet parse(String code){
        code = code.trim();
        code = code.substring(1, code.length() - 1);
        ArrayList<Proc> procs = new ArrayList<>();
        ArrayList<String> proc_codes = TrustSystemSerializer.flatten_brackets(code);
        for(String s : proc_codes){
            procs.add(Proc.parse(s));
        }
        return new ProcSet(procs);
    }

    public boolean equals(Object o){
        return p_set.equals(o);
    }

    public int hashCode(){
        return p_set.hashCode();
    }

    @Override
    public Iterator<Proc> iterator() {
        return p_set.iterator();
    }

    public ProcSet union(ProcSet quorum) {
        HashSet<Proc> temp = new HashSet<>(this.p_set);
        temp.addAll(quorum.get_p_set());
        return new ProcSet(temp);
    }
}
