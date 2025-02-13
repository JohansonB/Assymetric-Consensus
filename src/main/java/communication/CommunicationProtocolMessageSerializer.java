package communication;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import trustsystem.Proc;
import pt.unl.fct.di.novasys.network.ISerializer;
import utils.SerializerTools;


public class CommunicationProtocolMessageSerializer implements ISerializer<CommunicationProtocolMessage> {

    @Override
    public void serialize(CommunicationProtocolMessage msg, ByteBuf out) {
        // Serialize Proc origin
        SerializerTools.serializeProc(msg.getOrigin(),out);

        // Serialize Proc destination
        SerializerTools.serializeProc(msg.getDestination(),out);

        // Serialize message HashMap
        SerializerTools.serializeHashMap(msg.getMessage(), out);

        // Serialize payload HashMap
        SerializerTools.serializeHashMap(msg.getPayload(), out);

        // Serialize destination_proto
        out.writeShort(msg.getDestination_proto());
    }

    @Override
    public CommunicationProtocolMessage deserialize(ByteBuf in) {
        // Deserialize Proc origin
        Proc origin = SerializerTools.deserializeProc(in);

        // Deserialize Proc destination
        Proc destination = SerializerTools.deserializeProc(in);

        // Deserialize message HashMap
        HashMap<String, String> message = SerializerTools.deserializeHashMap(in);

        // Deserialize payload HashMap
        HashMap<String, String> payload = SerializerTools.deserializeHashMap(in);

        // Deserialize destination_proto
        short destination_proto = in.readShort();

        return new CommunicationProtocolMessage(origin, destination, message, payload, destination_proto);
    }


}

