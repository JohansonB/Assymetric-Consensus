package trustsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GridTrustSystem extends TrustSystem {
    private ArrayList<Integer> dims;
    int t_d;
    HashSet<Proc> procs;
    private QuorumSystem q_s = new GridQuorum();

    public GridTrustSystem(ArrayList<Integer> dims, int t_d, HashSet<Proc> procs){
        super(new FaultSystem(new HashSet<>()), new QuorumSystem(new HashSet<>()));
        this.dims = dims;
        this.t_d = t_d;
        this.procs = procs;
    }

    public class GridQuorum extends QuorumSystem{

        public GridQuorum() {
            super(new HashSet<>());
        }
        @Override
        public MarkedProcSystem get_marked(){
            return new MarkedGridSystem(dims,t_d,procs);
        }
    }


}
