package abvbroadcast;
import communication.CommunicationProtocol;
import communication.CommunicationReply;
import communication.MessageACK;
import communication.SendMessageRequest;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import trustsystem.*;
import utils.CollectionSerializer;

import java.io.IOException;
import java.util.*;
public class abvBroadcast extends GenericProtocol{
    public static final String PROTO_NAME = "abvBroadcast";
    public static final short PROTO_ID = 101;

    private Map<Proc, Set<Boolean>> values = new HashMap<>();
    private HashMap<Boolean,Boolean> sentValue = new HashMap<>();
    private Proc self;
    private Set<Proc> peers = new HashSet<>();
    private TrustSystem trustsystem;
    private KernelSystem kernelSystem;
    private MarkedProcSystem zero_marked_kernel;
    private MarkedProcSystem one_marked_kernel;
    private MarkedProcSystem zero_marked_quorum;
    private MarkedProcSystem one_marked_quorum;

    private short output_proto;

    public abvBroadcast() {
        super(PROTO_NAME, PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws IOException, HandlerRegistrationException {
        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = CollectionSerializer.flatten_collection("peers");
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            peers.add(p);
            values.put(p,new HashSet<>());
        }
        sentValue.put(true,false);
        sentValue.put(false,false);
        output_proto = new Short(properties.getProperty("output_proto"));
        trustsystem = TrustSystem.parse(properties.getProperty("trustsystem"));
        kernelSystem = trustsystem.get_quorums().get_kernel_system();
        zero_marked_kernel = kernelSystem.get_marked();
        one_marked_kernel = kernelSystem.get_marked();
        zero_marked_quorum = trustsystem.get_quorums().get_marked();
        one_marked_quorum = trustsystem.get_quorums().get_marked();

        registerRequestHandler(abvBroadcastRequest.REQUEST_ID,this::uponBroadcastRequest);

        registerReplyHandler(CommunicationReply.REPLY_ID,this::messageHandler);
    }

    private void messageHandler(CommunicationReply reply, short sourceProtocol) {
        if(reply.getMsg().containsKey("value")){
            boolean cur_bool = new Boolean(reply.getMsg().get("value"));
            Set cur_s = values.get(reply.getOrigin());
            if(!cur_s.contains(cur_bool)) {
                cur_s.add(cur_bool);
                Proc cur_p = reply.getOrigin();
                MarkedProcSystem cur_marked_kernel = cur_bool ? one_marked_kernel : zero_marked_kernel;
                MarkedProcSystem cur_marked_quorum = cur_bool ? one_marked_quorum : zero_marked_quorum;
                if(!sentValue.get(cur_bool)&&cur_marked_kernel.mark_proc(cur_p)){
                    uponBroadcastRequest(new abvBroadcastRequest(reply.getPayload(),cur_bool),PROTO_ID);
                }
                if(cur_marked_quorum.mark_proc(cur_p)){
                    sendReply(new abvDeliver(cur_bool,reply.getPayload()),output_proto);
                }


            }
        }
        sendReply(new MessageACK(self),CommunicationProtocol.PROTO_ID);
    }

    private void uponBroadcastRequest(abvBroadcastRequest request, short sourceProtocol) {
        sentValue.put(request.getInput(),true);
        HashMap<String,String> msg = new HashMap<>();
        msg.put("value", Boolean.toString(request.getInput()));
        for(Proc p : peers){
            sendRequest(new SendMessageRequest(msg,new HashMap<>(),p,PROTO_ID), CommunicationProtocol.PROTO_ID);
        }
    }
}
