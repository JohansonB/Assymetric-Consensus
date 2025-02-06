package commoncoin;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ReleaseCoinRequest extends ProtoRequest {
    public static final short REQUEST_ID = 4;
    int round;
    public ReleaseCoinRequest(int round){
        super(REQUEST_ID);
        this.round = round;
    }

    public int getRound() {
        return round;
    }
}
