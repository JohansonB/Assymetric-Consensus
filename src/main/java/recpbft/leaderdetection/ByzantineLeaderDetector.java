package recpbft.leaderdetection;

import communication.AuthenticatedChannel;
import communication.reply.CommunicationReply;
import conditionalproto.ConditionalCommunicationHandler;
import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalRequestHandler;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import trustsystem.KernelSystem;
import trustsystem.MarkedProcSystem;
import trustsystem.Proc;
import trustsystem.TrustSystem;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Predicate;

public class ByzantineLeaderDetector extends ConditionalGenericProtocol {
    private static final String PROTO_NAME = "ByzantineLeaderDetector";
    public static final short PROTO_ID = 108;


    Proc self;

    int round = 0;
    boolean complained = false;
    MarkedProcSystem complainedKernel;
    MarkedProcSystem complainedQuorum;
    TrustSystem trustSystem;
    KernelSystem kernelSystem;

    short output_proto;

    ArrayList<Proc> peers = new ArrayList<>();
    HashMap<Integer,Proc> id_peers = new HashMap<>();

    public ByzantineLeaderDetector(){
        super(PROTO_NAME,PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {


        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }

        output_proto = Short.parseShort(properties.getProperty("output_proto"));

        trustSystem = TrustSystem.parse(properties.getProperty("trustsystem"));
        kernelSystem = trustSystem.get_quorums().get_kernel_system();
        complainedKernel = kernelSystem.get_marked();
        complainedQuorum = trustSystem.get_quorums().get_marked();

        //initialize the communication Channel
        AuthenticatedChannel communication_channel = new AuthenticatedChannel();
        setupCommunicationChannel(communication_channel,properties);


        //Initialize the event Handlers and register them to the handler map so they can be associated with an id
        ConditionalRequestHandler<Complain> complainHandler = this::uponComplaint;
        short complainHandlerId = handler_map.register(complainHandler);
        Predicate<Complain> discard_condition = (Complain request) -> !request.getLeader().equals(leader(round))||complained;

        ConditionalCommunicationHandler complainMsgHandler = this::uponComplainMsg;
        short complainMsgHandlerId = handler_map.register(complainMsgHandler);

        registerRequestHandler(Complain.REQUEST_ID,
                discard_condition.negate(),
                complainHandlerId,
                discard_condition);

        registerCommunicationReplyHandler("complain",
                (CommunicationReply reply) -> Integer.parseInt(reply.getMsg().get("r"))==round,
                complainMsgHandlerId,
                (CommunicationReply reply) -> Integer.parseInt(reply.getMsg().get("r"))<round,
                complainMsgHandlerId);


        for(Proc p : peers){
           id_peers.put(p.getId(),p);
        }


    }
    private boolean complainedDiscard(Complain request){
        Proc round_leader = leader(round);
        boolean ret = !request.getLeader().equals(round_leader)||complained;
        return ret;
    }
    private void uponComplaint(Complain comp, short sourceProto){
        broadcastComplaint();
    }
    private void broadcastComplaint(){
        HashMap<String,String> msg = new HashMap<>();
        msg.put("r",Integer.toString(round));
        complained = true;
        for(Proc q : peers){
            sendMessage("complain",msg,q,PROTO_ID);
        }
    }

    private void uponComplainMsg(CommunicationReply reply){
        if(!complained&&complainedKernel.mark_proc(reply.getOrigin())){
            broadcastComplaint();
        }
        if(complainedQuorum.mark_proc(reply.getOrigin())){
            round++;
            complainedKernel.reset();
            complainedQuorum.reset();
            complained = false;
            sendReply(new TrustLeader(leader(round)),output_proto);
        }

    }

    private Proc leader(int round){
        return id_peers.get(round%peers.size());
    }
}
