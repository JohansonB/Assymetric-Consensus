package randomizedconsensus.abvbroadcast;

import communication.AuthenticatedChannel;
import communication.FifoChannel;
import communication.reply.CommunicationReply;
import communication.reply.MessageACK;
import communication.request.SendMessageRequest;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import trustsystem.KernelSystem;
import trustsystem.MarkedProcSystem;
import trustsystem.Proc;
import trustsystem.TrustSystem;
import utils.SerializerTools;

import java.io.IOException;
import java.util.*;

public class MultiplexAbvBroadcast extends GenericProtocol {
    public static final String PROTO_NAME = "abvBroadcast";
    public static final short PROTO_ID = 101;

    private HashMap<Integer,Map<Proc, Set<Boolean>>> values = new HashMap<>();
    private HashMap<Integer,HashMap<Boolean,Boolean>> sentValue = new HashMap<>();
    private HashMap<Integer,HashMap<Boolean,Boolean>> delivereds = new HashMap<>();
    private Proc self;
    private Set<Proc> peers = new HashSet<>();
    private TrustSystem trustsystem;
    private KernelSystem kernelSystem;
    private HashMap<Integer,MarkedProcSystem> zero_marked_kernel = new HashMap<>();
    private HashMap<Integer,MarkedProcSystem> one_marked_kernel = new HashMap<>();
    private HashMap<Integer,MarkedProcSystem> zero_marked_quorum = new HashMap<>();
    private HashMap<Integer,MarkedProcSystem> one_marked_quorum = new HashMap<>();

    private short output_proto;
    private int enter_count = 0;


    public MultiplexAbvBroadcast() {
        super(PROTO_NAME, PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws IOException, HandlerRegistrationException {

        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            peers.add(p);
        }
        output_proto = new Short(properties.getProperty("output_proto"));
        trustsystem = TrustSystem.parse(properties.getProperty("trustsystem"));
        kernelSystem = trustsystem.get_quorums().get_kernel_system();


        registerRequestHandler(MultiplexAbvBroadcastRequest.REQUEST_ID,this::uponBroadcastRequest);

        registerReplyHandler(CommunicationReply.REPLY_ID,this::messageHandler);

    }

    private void messageHandler(CommunicationReply reply, short sourceProtocol) {
        if(reply.getMsg().containsKey("value")){
            int round = new Integer(reply.getMsg().get("round"));
            setup_round(round);
            boolean cur_bool = new Boolean(reply.getMsg().get("value"));
            Set cur_s = values.get(round).get(reply.getOrigin());
            if(!cur_s.contains(cur_bool)) {
                cur_s.add(cur_bool);
                Proc cur_p = reply.getOrigin();
                MarkedProcSystem cur_marked_kernel = cur_bool ? one_marked_kernel.get(round) : zero_marked_kernel.get(round);
                MarkedProcSystem cur_marked_quorum = cur_bool ? one_marked_quorum.get(round) : zero_marked_quorum.get(round);
                if(!sentValue.get(round).get(cur_bool)&&cur_marked_kernel.mark_proc(cur_p)){
                    uponBroadcastRequest(new MultiplexAbvBroadcastRequest(reply.getPayload(),cur_bool,round),PROTO_ID);
                }
                if(!delivereds.get(round).get(cur_bool)&&cur_marked_quorum.mark_proc(cur_p)){
                    delivereds.get(round).put(cur_bool,true);
                    sendReply(new MultiplexAbvDeliver(cur_bool,round,reply.getPayload()),output_proto);
                }


            }
        }
        sendReply(new MessageACK(reply),AuthenticatedChannel.PROTO_ID);
    }

    private void setup_round(int round) {
        if(values.containsKey(round)){
            return;
        }
        values.put(round,new HashMap<>());
        Map<Proc,Set<Boolean>> cur_values = values.get(round);
        for(Proc p : peers){
            cur_values.put(p,new HashSet<>());
        }

        sentValue.put(round, new HashMap<>());
        HashMap<Boolean,Boolean> cur_sent = sentValue.get(round);
        cur_sent.put(true,false);
        cur_sent.put(false,false);

        HashMap<Boolean,Boolean> cur_del = new HashMap<>();
        cur_del.put(true,false);
        cur_del.put(false,false);
        delivereds.put(round,cur_del);

        zero_marked_kernel.put(round, kernelSystem.get_marked());
        one_marked_kernel.put(round,kernelSystem.get_marked());
        zero_marked_quorum.put(round,trustsystem.get_quorums().get_marked());
        one_marked_quorum.put(round,trustsystem.get_quorums().get_marked());
    }

    private void uponBroadcastRequest(MultiplexAbvBroadcastRequest request, short sourceProtocol) {
        int round = request.getRound();
        setup_round(round);
        sentValue.get(round).put(request.getInput(),true);
        HashMap<String,String> msg = new HashMap<>();
        msg.put("value", Boolean.toString(request.getInput()));
        msg.put("round", Integer.toString(round));
        for(Proc p : peers){
            sendRequest(new SendMessageRequest(msg,new HashMap<>(),p,PROTO_ID), AuthenticatedChannel.PROTO_ID);
        }
    }
}
