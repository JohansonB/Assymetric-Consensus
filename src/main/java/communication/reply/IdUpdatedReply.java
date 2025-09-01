package communication.reply;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class IdUpdatedReply extends ProtoReply {
    public static final short REPLY_ID = 101;


    public IdUpdatedReply() {
        super(REPLY_ID);
    }
}
