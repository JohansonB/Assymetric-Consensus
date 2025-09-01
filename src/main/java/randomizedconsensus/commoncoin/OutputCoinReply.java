package randomizedconsensus.commoncoin;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class OutputCoinReply extends ProtoReply {
    public static final short REPLY_ID = 5;
    boolean output;
    int round;

    public OutputCoinReply(boolean output, int round){
        super(REPLY_ID);
        this.output = output;
        this.round = round;
    }
    public boolean getOutput(){
        return output;
    }

    public int getRound() {
        return round;
    }
}
