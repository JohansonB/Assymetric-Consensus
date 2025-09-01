package communication.reply;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class ReInitKeyAck extends ProtoReply {
    public static final short ACK_ID = 103;
    public ReInitKeyAck() {
        super(ACK_ID);
    }
}
