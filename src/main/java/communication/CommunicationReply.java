package communication;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import trustsystem.Proc;

import java.util.HashMap;

public class CommunicationReply extends ProtoReply {
    public static final short REPLY_ID = 1;
    HashMap<String,String> msg;
    HashMap<String,String> payload;
    Proc origin;
    short destination_proto;

    public CommunicationReply(HashMap<String,String> msg, HashMap<String,String> payload, Proc origin, short destination_proto) {
        super(CommunicationReply.REPLY_ID);
        this.msg = msg;
        this.payload = payload;
        this.origin = origin;
        this.destination_proto = destination_proto;
    }

    public Proc getDestination() {
        return origin;
    }

    public HashMap<String, String> getMsg() {
        return msg;
    }

    public HashMap<String, String> getPayload() {
        return payload;
    }

    public Proc getOrigin() {
        return origin;
    }

    public short getDestination_proto() {
        return destination_proto;
    }
}
