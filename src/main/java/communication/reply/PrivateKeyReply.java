package communication.reply;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

import java.security.PrivateKey;

public class PrivateKeyReply extends ProtoReply {
    public static final short REPLY_ID = 9;
    private PrivateKey key;
    public PrivateKeyReply(PrivateKey key){
        super(REPLY_ID);
        this.key = key;
    }

    public PrivateKey getKey() {
        return key;
    }
}
