package communication;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class PendingTimer extends ProtoTimer {
    public static final short TIMER_ID = 1;

    public PendingTimer() {
        super(TIMER_ID);
    }



    @Override
    public ProtoTimer clone() {
        return this;
    }
}
