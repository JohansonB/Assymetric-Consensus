package communication;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import trustsystem.Proc;

public class MessageACK extends ProtoReply {
    public static final short ACK_ID = 2;
    Proc origin;
    public MessageACK(Proc origin) {
        super(ACK_ID);
        this.origin = origin;
    }

    public Proc getOrigin() {
        return origin;
    }
}
