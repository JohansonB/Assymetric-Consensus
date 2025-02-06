package communication;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import trustsystem.Proc;

import java.util.HashMap;

public class CommunicationProtocolMessage extends ProtoMessage {
    public static final short MSG_ID = 1;
    private final HashMap<String,String> payload;
    private final HashMap<String,String> message;
    private final Proc origin;
    private final Proc destination;
    private final short destination_proto;

    public CommunicationProtocolMessage(Proc origin,Proc destination, HashMap<String,String> message, HashMap<String,String> payload, short destination_proto) {
        super(CommunicationProtocolMessage.MSG_ID);
        this.origin = origin;
        this.destination = destination;
        this.message = message;
        this.payload = payload;
        this.destination_proto = destination_proto;

    }

    public short getDestination_proto() {
        return destination_proto;
    }

    public HashMap<String, String> getMessage() {
        return message;
    }

    public Proc getOrigin() {
        return origin;
    }

    public HashMap<String, String> getPayload() {
        return payload;
    }

    public Proc getDestination() {
        return destination;
    }
}
