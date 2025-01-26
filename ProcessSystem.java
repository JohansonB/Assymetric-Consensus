import java.util.ArrayList;
import java.util.Collection;

public class ProcessSystem {
    ArrayList<ProcessSet> p_sets;

    ProcessSystem(Collection<ProcessSet> p_sets){
        this.p_sets = new ArrayList<>(p_sets);
    }

    public int size(){
        return p_sets.size();
    }

    public ProcessSet get(int index) {
        return p_sets.get(index);
    }

    public ArrayList<ProcessSet> get_p_sets() {
        return p_sets;
    }
}
