package communication;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import trustsystem.Proc;

import java.util.HashMap;

public class SendMessageRequest extends ProtoRequest {
    public static final short REQUEST_ID = 1;
    private final HashMap<String,String> message;
    private  final  HashMap<String,String> payload;
    private final Proc destination;
    private final short destination_proto;
    public SendMessageRequest(HashMap<String,String> message, HashMap<String,String> payload, Proc destination, short destination_proto) {
        super(SendMessageRequest.REQUEST_ID);
        this.message = message;
        this.payload = payload;
        this.destination = destination;
        this.destination_proto = destination_proto;
    }

    public HashMap<String, String> getMessage() {
        return message;
    }

    public HashMap<String, String> getPayload() {
        return payload;
    }

    public short getDestination_proto() {
        return destination_proto;
    }

    public Proc getDestination() {
        return destination;
    }
}
