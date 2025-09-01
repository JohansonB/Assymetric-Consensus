package pbft;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class PBFTDecide extends ProtoReply {
    public static final short REPLY_ID = 11;
    String val;
    public PBFTDecide(String val){
        super(REPLY_ID);
        this.val = val;
    }

    public String getVal() {
        return val;
    }
}
