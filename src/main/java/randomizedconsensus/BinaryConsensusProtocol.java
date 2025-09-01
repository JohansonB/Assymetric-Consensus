package randomizedconsensus;

import communication.AuthenticatedChannel;
import communication.FifoChannel;
import communication.reply.CommunicationReply;
import communication.reply.MessageACK;
import conditionalproto.ConditionalCommunicationHandler;
import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import conditionalproto.ConditionalRequestHandler;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import randomizedconsensus.abvbroadcast.MultiplexAbvBroadcast;
import randomizedconsensus.abvbroadcast.MultiplexAbvBroadcastRequest;
import randomizedconsensus.abvbroadcast.MultiplexAbvDeliver;
import randomizedconsensus.commoncoin.OutputCoinReply;
import randomizedconsensus.commoncoin.ReleaseCoinRequest;
import randomizedconsensus.commoncoin.UnsafeCommonCoinProtocol;
import trustsystem.KernelSystem;
import trustsystem.MarkedProcSystem;
import trustsystem.Proc;
import trustsystem.TrustSystem;
import utils.SerializerTools;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class BinaryConsensusProtocol extends ConditionalGenericProtocol {
    public static final String PROTO_NAME = "conditionalbinaryconsensus";
    public static final short PROTO_ID = 104;
    Proc self;
    int round = 0;
    HashSet<Boolean> values = new HashSet<>();
    HashMap<Proc,HashSet<Boolean>> aux = new HashMap<>();
    HashMap<Proc,Boolean> decided = new HashMap<>();
    boolean sentdecide = false;
    boolean coinreleased = false;
    boolean halted = false;


    private MarkedProcSystem zero_marked_kernel;
    private MarkedProcSystem one_marked_kernel;
    private MarkedProcSystem zero_marked_quorum_aux;
    private MarkedProcSystem one_marked_quorum_aux;
    private MarkedProcSystem zero_marked_quorum_decided;
    private MarkedProcSystem one_marked_quorum_decided;

    private MarkedProcSystem release_coin_quorum;

    HashSet<Proc> peers = new HashSet<>();
    TrustSystem trustsystem;
    KernelSystem kernelSystem;
    short output_proto;
    public BinaryConsensusProtocol() {
        super(PROTO_NAME, PROTO_ID);
    }


    @Override
    public void init(Properties properties) throws IOException, HandlerRegistrationException {
        self = Proc.parse(properties.getProperty("self"));
        output_proto = new Short(properties.getProperty("output_proto"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            aux.put(p,new HashSet<>());
            peers.add(p);
        }

        trustsystem = TrustSystem.parse(properties.getProperty("trustsystem"));
        kernelSystem = trustsystem.get_quorums().get_kernel_system();

        zero_marked_kernel = kernelSystem.get_marked();
        one_marked_kernel = kernelSystem.get_marked();
        zero_marked_quorum_aux = trustsystem.get_quorums().get_marked();
        one_marked_quorum_aux = trustsystem.get_quorums().get_marked();
        zero_marked_quorum_decided = trustsystem.get_quorums().get_marked();
        one_marked_quorum_decided = trustsystem.get_quorums().get_marked();
        release_coin_quorum = trustsystem.get_quorums().get_marked();

        //initialize the communication Channel
        Properties properties1 = (Properties) properties.clone();
        properties1.remove("output_proto");
        AuthenticatedChannel communication_channel = new AuthenticatedChannel();
        setupCommunicationChannel(communication_channel,properties);

        //Initialize the event Handlers and register them to the handler map so they can be associated with an id
        ConditionalRequestHandler<ACProposeRequest> proposeHandler = this::uponPropose;
        short proposeHandlerId = handler_map.register(proposeHandler);

        ConditionalReplyHandler<OutputCoinReply> coinReplyHandler = this::uponCoinOutput;
        short coinHandlerId = handler_map.register(coinReplyHandler);
        //coin is delivered when a quorum for 0 or 1 exists and the round tag is equal to the current round
        Predicate<OutputCoinReply> coinExeCondition = (OutputCoinReply reply)->
                reply.getRound()==round&&(zero_marked_quorum_aux.getPSetFound()||one_marked_quorum_aux.getPSetFound());

        ConditionalReplyHandler<MultiplexAbvDeliver> abvDeliverHandler = this::uponAbvDeliver;
        short abvDeliverHandlerId = handler_map.register(abvDeliverHandler);

        ConditionalCommunicationHandler auxMessageHandler = this::uponAuxMessage1;
        short auxMessageHandlerId = handler_map.register(auxMessageHandler);
        //when discarded the message still needs to be acknowledged to prevent a deadlock
        ConditionalCommunicationHandler discardAuxHandler = (CommunicationReply reply) -> sendReply(new MessageACK(reply),getComProtoId());

        ConditionalCommunicationHandler decideMessageHandler = this::uponDecideMessage;
        short decideMessageHandlerId = handler_map.register(decideMessageHandler);


        registerRequestHandler(ACProposeRequest.REQUEST_ID,
                (ACProposeRequest request) -> true,
                proposeHandlerId,
                (ACProposeRequest request) -> halted,
                (ACProposeRequest request, short source) -> {});

        registerReplyHandler(OutputCoinReply.REPLY_ID,
                coinExeCondition,
                coinHandlerId,
                (OutputCoinReply reply) -> reply.getRound()<round||halted,
                (OutputCoinReply reply,short source)->{},
                auxMessageHandlerId,coinHandlerId);

        registerReplyHandler(MultiplexAbvDeliver.REPSONSE_ID,
                (MultiplexAbvDeliver reply) -> reply.getRound()==round,
                abvDeliverHandlerId,
                (MultiplexAbvDeliver reply) -> reply.getRound()<round||halted,
                (MultiplexAbvDeliver reply, short source) -> {},
                coinHandlerId);

        registerCommunicationReplyHandler("aux",
                (CommunicationReply reply) ->
                        Integer.parseInt(reply.msg.get("round"))==round
                        &&
                        values.contains(Boolean.parseBoolean(reply.msg.get("b"))),
                auxMessageHandlerId,
                (CommunicationReply reply) -> Integer.parseInt(reply.msg.get("round"))<round||halted,
                discardAuxHandler,
                coinHandlerId,abvDeliverHandlerId);

        registerCommunicationReplyHandler("decide",
                (CommunicationReply reply) -> true,
                decideMessageHandlerId,
                (CommunicationReply reply) -> halted,
                (CommunicationReply reply) -> {},
                coinHandlerId);

        //initialize the protocols used by the protocol (try catch since another protocol might have already done this)
        try {
            UnsafeCommonCoinProtocol coin = new UnsafeCommonCoinProtocol();
            Babel.getInstance().registerProtocol(coin);
            coin.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }
        try{
            MultiplexAbvBroadcast abv = new MultiplexAbvBroadcast();
            Babel.getInstance().registerProtocol(abv);
            properties.setProperty("output_proto",Short.toString(PROTO_ID));
            abv.init(properties);

        } catch (ProtocolAlreadyExistsException e) {

        }

    }



    //abvBroadcast the proposed value
    private void uponPropose(ACProposeRequest request, short sourceProtocol){
        sendRequest(new MultiplexAbvBroadcastRequest(new HashMap<>(),request.getInput(),round), MultiplexAbvBroadcast.PROTO_ID);
    }

    //send the abvDelivered value to all peers
    private void uponAbvDeliver(MultiplexAbvDeliver reply, short sourceProtocol){
        values.add(reply.getOutput());
        HashMap<String,String> msg = new HashMap<>();
        msg.put("round",Integer.toString(round));
        msg.put("b",Boolean.toString(reply.getOutput()));
        for(Proc p : peers) {
            sendMessage("aux",msg,p,PROTO_ID);
        }

    }


    private void uponAuxMessage1(CommunicationReply reply){
        Proc p = reply.getOrigin();
        HashMap<String,String> msg = reply.getMsg();
        boolean b = new Boolean(msg.get("b"));
        aux.get(p).add(b);
        //choose the QuorumConditionChecker according to the received value
        MarkedProcSystem cur_marked_quorum = b ? one_marked_quorum_aux : zero_marked_quorum_aux;
        //Update the Quorum condition and check if it is satisfied, if so release the coin
        if(cur_marked_quorum.mark_proc(p)&&!coinreleased){
            coinreleased = true;
            sendRequest(new ReleaseCoinRequest(round), UnsafeCommonCoinProtocol.PROTO_ID);
        }
        sendReply(new MessageACK(reply),AuthenticatedChannel.PROTO_ID);
    }

    private void uponAuxMessage2(CommunicationReply reply){
        Proc p = reply.getOrigin();
        HashMap<String,String> msg = reply.getMsg();
        boolean b = new Boolean(msg.get("b"));
        aux.get(p).add(b);
        //choose the QuorumConditionChecker according to the received value
        MarkedProcSystem cur_marked_quorum = b ? one_marked_quorum_aux : zero_marked_quorum_aux;
        cur_marked_quorum.mark_proc(p);
        //Update the Quorum condition and check if it is satisfied, if so release the coin
        if(release_coin_quorum.mark_proc(p)&&!coinreleased){
            coinreleased = true;
            sendRequest(new ReleaseCoinRequest(round), UnsafeCommonCoinProtocol.PROTO_ID);
        }
        sendReply(new MessageACK(reply),AuthenticatedChannel.PROTO_ID);
    }

    void uponDecideMessage(CommunicationReply reply){
        HashMap<String,String> msg = reply.getMsg();
        Proc p = reply.getOrigin();
        boolean b = new Boolean(msg.get("b"));
        if(!decided.containsKey(p)){
            decided.put(p,b);
            MarkedProcSystem cur_marked_quorum = b ? one_marked_quorum_decided : zero_marked_quorum_decided;
            MarkedProcSystem cur_marked_kernel = b ? one_marked_kernel : zero_marked_kernel;
            if(!sentdecide&&cur_marked_kernel.mark_proc(p)){
                send_decide(b);
            }
            if(cur_marked_quorum.mark_proc(p)){
                sendReply(new ACDecide(b),output_proto);
                halted = true;
            }
        }
        sendReply(new MessageACK(reply),AuthenticatedChannel.PROTO_ID);
    }

    private void send_decide(boolean b){
        HashMap<String,String> decide_msg = new HashMap<>();
        decide_msg.put("b",Boolean.toString(b));
        for(Proc q : peers){
            sendMessage("decide",decide_msg,q,PROTO_ID);
        }
        sentdecide = true;
    }
    private void uponCoinOutput(OutputCoinReply reply, short sourceProtocol){
        boolean s = reply.getOutput();
        round++;
        //this condition checks if only one of the quorum conditions is satisfied (since at least one is)
        if(zero_marked_quorum_aux.getPSetFound()!=one_marked_quorum_aux.getPSetFound()){
            boolean b = one_marked_quorum_aux.getPSetFound() ? true : false;
            if(b==s && !sentdecide){
                send_decide(b);
            }
            sendRequest(new MultiplexAbvBroadcastRequest(new HashMap<>(),b,round), MultiplexAbvBroadcast.PROTO_ID);
        }
        else{
            sendRequest(new MultiplexAbvBroadcastRequest(new HashMap<>(),s,round), MultiplexAbvBroadcast.PROTO_ID);
        }
        resetVariables();

    }
    void resetVariables(){
        //reset bookkeeping variables
        values = new HashSet<>();
        for(Proc p : peers){
            aux.put(p,new HashSet<>());
        }
        coinreleased = false;
        one_marked_quorum_aux.reset();
        one_marked_quorum_aux.reset();
        release_coin_quorum.reset();
    }

}
