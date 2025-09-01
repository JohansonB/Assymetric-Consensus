package recpbft;

import trustsystem.Proc;

public interface PredicateTracker {

    public void insert(Proc p, String m);


    public boolean isInvalid(String m);

    public void reset(Proc leader);

    public void leaderProposed(boolean b);

    boolean satisfied();
}
