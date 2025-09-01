package communication.reply;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import trustsystem.Proc;

import java.security.PublicKey;

public class AllKeysUpReply extends ProtoReply {
    public static final short REPLY_ID = 105;
    private boolean allUp;
    public AllKeysUpReply(boolean allUp){
        super(REPLY_ID);
        this.allUp = allUp;
    }

    public boolean isAllUp() {
        return allUp;
    }
}
