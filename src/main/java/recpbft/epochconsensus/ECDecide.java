package recpbft.epochconsensus;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class ECDecide extends ProtoReply {
    public static final short REPLY_ID = 10;
    String val;
    int ts;
    public ECDecide(String val, int ts){
        super(REPLY_ID);
        this.val = val;
        this.ts = ts;
    }

    public String getVal() {
        return val;
    }

    public int getTs() {
        return ts;
    }
}
