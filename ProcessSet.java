import java.util.Collection;
import java.util.HashSet;

public class ProcessSet {
    HashSet<Process> p_set;
    public ProcessSet(Collection<Process> p_set){
        this.p_set = new HashSet<>(p_set);
    }
    public HashSet<Process> get_p_set(){
        return p_set;
    }
    public boolean contains(Process p){
        return p_set.contains(p);
    }
    public int size(){
        return p_set.size();
    }
    public boolean isEmpty(){
        return p_set.isEmpty();
    }

    public void merge(ProcessSet other) {
        p_set.addAll(other.get_p_set());
    }
}
