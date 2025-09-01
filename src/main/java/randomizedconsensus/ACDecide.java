package randomizedconsensus;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class ACDecide extends ProtoReply {
    public static final short REPLY_ID = 6;
    private boolean output;
    public ACDecide(boolean output){
        super(REPLY_ID);
        this.output = output;
    }
    public boolean getOutput(){
        return output;
    }
}
