package communication;

import communication.reply.CommunicationReply;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class DeliverTimer extends ProtoTimer {
    public static final short TIMER_ID = 2;
    public final CommunicationReply reply;

    public DeliverTimer(CommunicationReply reply) {
        super(TIMER_ID);
        this.reply = reply;
    }



    @Override
    public ProtoTimer clone() {
        return this;
    }
}