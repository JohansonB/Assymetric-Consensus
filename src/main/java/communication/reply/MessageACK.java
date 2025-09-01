package communication.reply;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class MessageACK extends ProtoReply {
    public static final short ACK_ID = 2;
    CommunicationReply reply;
    public MessageACK(CommunicationReply reply) {
        super(ACK_ID);
        this.reply = reply;
    }

    public CommunicationReply getReply() {
        return reply;
    }
}
