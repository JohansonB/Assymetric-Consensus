package communication.reply;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class ConnectionsClosedReply extends ProtoReply {
    public static final short REPLY_ID = 108;
    public ConnectionsClosedReply(){
        super(REPLY_ID);
    }

}
