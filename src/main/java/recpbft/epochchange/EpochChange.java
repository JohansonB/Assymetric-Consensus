package recpbft.epochchange;

import communication.AuthenticatedChannel;
import communication.reply.CommunicationReply;
import conditionalproto.ConditionHandler;
import conditionalproto.ConditionalCommunicationHandler;
import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import org.apache.logging.log4j.core.config.Configurator;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import recpbft.leaderdetection.ByzantineLeaderDetector;
import recpbft.leaderdetection.TrustLeader;
import trustsystem.KernelSystem;
import trustsystem.MarkedProcSystem;
import trustsystem.Proc;
import trustsystem.TrustSystem;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class EpochChange extends ConditionalGenericProtocol {
    private static final String PROTO_NAME = "EpochChange";
    public static final short PROTO_ID = 109;


    Proc self;

    int lastts = 0;
    int nextts = 0;
    Proc trusted;
    MarkedProcSystem newEpochKernel;
    MarkedProcSystem newEpochQuorum;
    TrustSystem trustSystem;
    KernelSystem kernelSystem;


    short output_proto;

    ArrayList<Proc> peers = new ArrayList<>();
    HashMap<Integer,Proc> id_peers = new HashMap<>();


    public EpochChange(){
        super(PROTO_NAME,PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        Configurator.setLevel("AuthenticatedChannel", org.apache.logging.log4j.Level.DEBUG);


        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }

        output_proto = Short.parseShort(properties.getProperty("output_proto"));

        trustSystem = TrustSystem.parse(properties.getProperty("trustsystem"));
        kernelSystem = trustSystem.get_quorums().get_kernel_system();
        newEpochKernel = kernelSystem.get_marked();
        newEpochQuorum = trustSystem.get_quorums().get_marked();

        for(Proc p : peers){
            id_peers.put(p.getId(),p);
        }
        trusted = leader(lastts);


        //initialize the communication Channel
        AuthenticatedChannel communication_channel = new AuthenticatedChannel();
        setupCommunicationChannel(communication_channel,properties);

        ConditionalReplyHandler<TrustLeader> trustLeaderHandler = this::uponNewLeader;
        short trustLeaderHandlerId = handler_map.register(trustLeaderHandler);

        ConditionalCommunicationHandler newEpochMsgHandler = this::handleNewEpochReply;
        short newEpochMsgHandlerId = handler_map.register(newEpochMsgHandler);

        ConditionHandler broadcastConditionHandler = ()->{
            nextts = lastts+1;broadcastEpoch();
        };
        short broadcastConditionHandlerId = handler_map.register(broadcastConditionHandler);

        ConditionHandler kernelConditionHandler = this::handleKernelCondtion;
        short kernelConditionHandlerId = handler_map.register(kernelConditionHandler);

        ConditionHandler quorumConditionHandler = this::handleQuorumCondition;
        short quorumConditionHandlerId = handler_map.register(quorumConditionHandler);

        registerEndlessConditionHandler(
                (Object o)->broadcastEpochCondition(),
                broadcastConditionHandlerId,
                trustLeaderHandlerId,
                quorumConditionHandlerId,trustLeaderHandlerId,broadcastConditionHandlerId);

        registerReplyHandler(
                TrustLeader.REPLY_ID,
                (TrustLeader l)->true,
                trustLeaderHandlerId);

        registerEndlessConditionHandler(
                (Object o)->newEpochKernel.getPSetFound()&&nextts==lastts,
                kernelConditionHandlerId,
                newEpochMsgHandlerId);

        registerEndlessConditionHandler(
                (Object o)->newEpochQuorum.getPSetFound()&&nextts>lastts,
                quorumConditionHandlerId,
                newEpochMsgHandlerId,kernelConditionHandlerId,broadcastConditionHandlerId);

        registerCommunicationReplyHandler(
                "newEpoch",
                (CommunicationReply c) -> Integer.parseInt(c.getMsg().get("ts"))==lastts+1,
                newEpochMsgHandlerId,
                quorumConditionHandlerId);

        try {
            properties.setProperty("output_proto",Short.toString(PROTO_ID));
            ByzantineLeaderDetector detector = new ByzantineLeaderDetector();
            Babel.getInstance().registerProtocol(detector);
            detector.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }

    }

    private void handleNewEpochReply(CommunicationReply reply) {
        newEpochKernel.mark_proc(reply.getOrigin());
        newEpochQuorum.mark_proc(reply.getOrigin());
    }
    private void handleKernelCondtion(){
        nextts = lastts+1;
        broadcastEpoch();
    }

    private void handleQuorumCondition(){
        lastts = nextts;
        newEpochKernel.reset();
        newEpochQuorum.reset();
        sendReply(new StartEpoch(leader(lastts),lastts),output_proto);
    }

    private void uponNewLeader(TrustLeader reply, short sourceProto){
        trusted = reply.getLeader();
    }

    private void broadcastEpoch() {
        HashMap<String,String> msg = new HashMap<>();
        msg.put("ts",Integer.toString(nextts));
        for(Proc q : peers){
            sendMessage("newEpoch",msg,q,PROTO_ID);
        }
    }
    boolean broadcastEpochCondition(){
        boolean b = nextts==lastts&&!trusted.equals(leader(lastts));
        return b;
    }
    private Proc leader(int round){
        return id_peers.get(round%peers.size());
    }

    private Proc min(ArrayList<Proc> peers) {
        int min = Integer.MAX_VALUE;
        Proc min_p = null;
        for(Proc p : peers){
            if(min>p.getId()){
                min = p.getId();
                min_p = p;
            }
        }
        return min_p;
    }

}
