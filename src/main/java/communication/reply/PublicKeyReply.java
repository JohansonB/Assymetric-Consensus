package communication.reply;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import trustsystem.Proc;

import java.security.PublicKey;

public class PublicKeyReply extends ProtoReply {
    public static final short REPLY_ID = 10;
    private PublicKey key;
    private Proc p;
    public PublicKeyReply(PublicKey key, Proc p){
        super(REPLY_ID);
        this.key = key;
        this.p = p;
    }

    public PublicKey getKey() {
        return key;
    }

    public Proc getP() {
        return p;
    }
}
