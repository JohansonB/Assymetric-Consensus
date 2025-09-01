package communication;

import communication.reply.CommunicationReply;
import communication.reply.MessageACK;
import communication.request.SendMessageRequest;
import org.apache.logging.log4j.core.config.Configurator;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.network.data.Host;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class FifoChannel extends GenericProtocol {
    private static final String PROTO_NAME = "fifochannel";
    public static final short PROTO_ID = 106;

    HashMap<Proc,Boolean> pending_acks = new HashMap<>();
    HashMap<Proc, Queue<CommunicationReply>> reply_queues = new HashMap<>();

    Proc self;
    Host self_h;

    ArrayList<Proc> peers = new ArrayList<>();
    HashMap<Proc, Host> peer_h = new HashMap<>();
    HashMap<Host,Proc> h_p_map = new HashMap<>();

    public FifoChannel(){
        super(PROTO_NAME,PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {



        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }

        registerRequestHandler(SendMessageRequest.REQUEST_ID,this::uponMessageRequest);
        registerReplyHandler(MessageACK.ACK_ID,this::uponMessageACK);
        registerReplyHandler(CommunicationReply.REPLY_ID,this::uponMessageReply);

        Host h;

        for(Proc p : peers){
            h = new Host(InetAddress.getByName(p.getAddress()),p.getPort());
            reply_queues.put(p,new LinkedList<>());
            pending_acks.put(p,false);
            peer_h.put(p,h);
            h_p_map.put(h,p);
        }
        try {
            properties.setProperty("dest_proto",Short.toString(PROTO_ID));
            AuthenticatedChannel a_c = new AuthenticatedChannel();
            Babel.getInstance().registerProtocol(a_c);
            a_c.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }

    }

    private void uponMessageACK(MessageACK ack, short sourceProtocol) {
        Proc origin = ack.getReply().getOrigin();
        pending_acks.put(origin,false);
        Queue<CommunicationReply> cur_q = reply_queues.get(origin);

        if(!cur_q.isEmpty()){
            CommunicationReply reply = cur_q.remove();
            pending_acks.put(origin,true);
            sendReply(reply,reply.getDestination_proto());
        }
    }

    private void uponMessageRequest(SendMessageRequest messageRequest, short sourceProtocol){
        sendRequest(messageRequest,AuthenticatedChannel.PROTO_ID);
    }

    private void uponMessageReply(CommunicationReply reply, short sourceProtocol){
        Queue<CommunicationReply> cur_reply_q = reply_queues.get(reply.getOrigin());
        cur_reply_q.add(reply);
        if(!pending_acks.get(reply.getOrigin())){
            reply = cur_reply_q.remove();
            pending_acks.put(reply.getOrigin(),true);
            sendReply(reply,reply.getDestination_proto());
        }
    }
}
