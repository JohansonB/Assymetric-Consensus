package trustsystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GridRecognizer extends MarkedProcSystem {
    ArrayList<Integer> dims;
    ArrayList<Integer> counts;
    int my_dim;
    int cap;
    ArrayList<Proc> seen;
    int partial_satisfied;


    public GridRecognizer(ArrayList<Integer> dims, int my_dim){
        this.dims = dims;
        this.my_dim = my_dim;
        seen = new ArrayList<>();
        counts = new ArrayList<>();
        for(int i = 0; i<dims.get(my_dim);i++){
            counts.add(0);
        }
        cap = 1;
        for(int i = 0; i<dims.size();i++){
            cap*=dims.get(i);
        }
        cap /= dims.get(my_dim);
        cap = get_threshold(cap,6);
    }

    @Override
    public boolean mark_proc(Proc p) {
        if (seen.contains(p)||p_set_found){
            return p_set_found;
        }
        seen.add(p);
        List<Integer> ind = indexToCoordinates(p.id,dims);
        int att_ind = ind.get(my_dim);
        int count = counts.get(att_ind)+1;
        counts.add(att_ind, count);
        if(count== cap){
            partial_satisfied++;
        }
        if(3*partial_satisfied>=2*dims.size()){
            p_set_found = true;
            found_quorum = find_quorum();
        }
        return p_set_found;
    }

    private ProcSet find_quorum() {
        boolean found = true;
        ArrayList<Proc> seen = new ArrayList<>(this.seen);
        ArrayList<Proc> seen_inner;
        GridRecognizer rec = new GridRecognizer(dims,my_dim);
        while (found){
            found = false;
            for(int i = 0 ; i<seen.size();i++){
                seen_inner = new ArrayList<>();
                for(int j = seen.size()-1 ; j>=0;j--){
                    if(i == j){
                        continue;
                    }
                    seen_inner.add(seen.get(j));
                    if(rec.mark_proc(seen.get(j))){
                        found = true;
                        seen = seen_inner;
                        break;
                    }
                }
                if(found){
                    break;
                }
            }
            rec.reset();
        }
        return new ProcSet(seen);
    }

    @Override
    public void reset() {
        counts = new ArrayList<>();
        for(int i = 0; i<dims.get(my_dim);i++){
            counts.add(0);
        }
    }

    public boolean getPSetFound(){
        return  p_set_found;
    }

    public ProcSet getQuorum(){
        return found_quorum;
    }

    public static List<Integer> indexToCoordinates(int id,ArrayList<Integer> dims) {
        List<Integer> coordinates = new ArrayList<>();
        for (int i = dims.size() - 1; i >= 0; i--) {
            int dim = dims.get(i);
            coordinates.add(0, id % dim);
            id /= dim;
        }
        return coordinates;
    }

    public static int get_threshold(int n, int ratio){
        int num;
        if(n%ratio == 0){
            num =  n/ratio -1;
        }
        else {
            num =  n/ratio;
        }
        return n - num;

    }
}
