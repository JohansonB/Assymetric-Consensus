package recpbft.conditionalcollect;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import recpbft.EpochState;

public class InputRequest extends ProtoRequest {
    public final static short REQUEST_ID = 8;
    private EpochState state;

    public InputRequest(EpochState state) {
        super(REQUEST_ID);
        this.state = state;
    }

    public EpochState getState() {
        return state;
    }
}
