package pbft.epochconsensus;

import communication.AuthenticatedChannel;
import communication.reply.CommunicationReply;
import conditionalproto.ConditionalCommunicationHandler;
import conditionalproto.ConditionalGenericProtocol;

import conditionalproto.ConditionalReplyHandler;
import conditionalproto.ConditionalRequestHandler;
import pbft.EpochState;
import pbft.EpochStatePredicateTracker;
import pbft.conditionalcollect.CCSetup;
import pbft.conditionalcollect.CollectedOutput;
import pbft.conditionalcollect.ConditionalCollect;
import pbft.conditionalcollect.InputRequest;
import pbft.leaderdetection.ByzantineLeaderDetector;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import trustsystem.*;
import utils.SerializerTools;

import java.io.IOException;
import java.util.*;

public class EpochConsensus extends ConditionalGenericProtocol {
    private static final String PROTO_NAME = "EpochConsensus";
    public static final short PROTO_ID = 111;

    Proc self;
    HashSet<Proc> peers = new HashSet<>();
    int ets;
    EpochState state;
    Proc leader;
    short output_proto;
    boolean aborted;

    EpochStatePredicateTracker tracker;

    AsymmetricTrustSystem asymmTrustSystem;
    ToleratedSystem toleratedSystem;
    QuorumSystem quorums;
    String proposal;

    HashMap<String, MarkedProcSystem> writeQuorums;
    HashMap<String,MarkedProcSystem> acceptQuorums;




    public EpochConsensus(){
        super(PROTO_NAME,PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        //initialize the communication Channel
        AuthenticatedChannel communication_channel = new AuthenticatedChannel();
        setupCommunicationChannel(communication_channel,properties);


        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            peers.add(p);
        }
        output_proto = Short.parseShort(properties.getProperty("output_proto"));

        asymmTrustSystem = AsymmetricTrustSystem.parse(properties.getProperty("ats"));
        toleratedSystem = new ToleratedSystem(TrustSystem.parse(properties.getProperty("ts")));;
        quorums = asymmTrustSystem.getTrustSystem(self).get_quorums();
        tracker = new EpochStatePredicateTracker(toleratedSystem,leader);

        ConditionalRequestHandler<ECSetup> setupHandler = this::uponSetup;
        short setupHandlerId = handler_map.register(setupHandler);

        ConditionalRequestHandler<ECPropose> proposeHandler = this::uponPropose;
        short proposeHandlerId = handler_map.register(proposeHandler);

        ConditionalCommunicationHandler readMsgHandler = this::uponReadMsg;
        short readMsgHandlerId = handler_map.register(readMsgHandler);

        ConditionalReplyHandler<CollectedOutput> collectedHandler = this::uponCCCollect;
        short collectedHandlerId = handler_map.register(collectedHandler);

        ConditionalCommunicationHandler writeMsgHandler = this::uponWriteMsg;
        short writeMsgHandlerId = handler_map.register(writeMsgHandler);

        ConditionalCommunicationHandler acceptMsgHandler = this::uponAcceptMsg;
        short acceptMsgHandlerId = handler_map.register(acceptMsgHandler);

        ConditionalRequestHandler<ECAbort> abortHandler = this::uponAbort;
        short abortHandlerId = handler_map.register(abortHandler);

        registerRequestHandler(
                ECSetup.REQUEST_ID,
                (ECSetup setup) -> true,
                setupHandlerId
        );
        registerRequestHandler(
                ECPropose.REQUEST_ID,
                (ECPropose propose) -> !aborted,
                proposeHandlerId,
                (ECPropose propose) -> aborted
        );
        registerCommunicationReplyHandler(
                "read",
                (CommunicationReply reply) -> Integer.parseInt(reply.getMsg().get("ts"))==ets
                        &&reply.getOrigin().equals(leader)&&!aborted,
                readMsgHandlerId,
                (CommunicationReply reply) -> Integer.parseInt(reply.getMsg().get("ts"))<ets
                        ||!reply.getOrigin().equals(leader),
                setupHandlerId
        );
        registerReplyHandler(
                CollectedOutput.PROTO_REPLY,
                (CollectedOutput reply) -> reply.getTs() == ets&&!aborted,
                collectedHandlerId,
                (CollectedOutput reply) -> reply.getTs() < ets,
                setupHandlerId
        );
        registerCommunicationReplyHandler(
                "write",
                (CommunicationReply reply) -> Integer.parseInt(reply.getMsg().get("ts"))==ets&&!aborted,
                writeMsgHandlerId,
                (CommunicationReply reply) -> Integer.parseInt(reply.getMsg().get("ts"))<ets,
                setupHandlerId
        );
        registerCommunicationReplyHandler(
                "accept",
                (CommunicationReply reply) -> Integer.parseInt(reply.getMsg().get("ts"))==ets&&!aborted,
                acceptMsgHandlerId,
                (CommunicationReply reply) -> Integer.parseInt(reply.getMsg().get("ts"))<ets,
                setupHandlerId
        );
        registerRequestHandler(
                ECAbort.REQUEST_ID,
                (ECAbort abort) -> true,
                abortHandlerId
        );

        try {
            properties.setProperty("output_proto",Short.toString(PROTO_ID));
            ConditionalCollect cc = new ConditionalCollect();
            Babel.getInstance().registerProtocol(cc);
            cc.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }



    }

    public void uponSetup(ECSetup setup, short source){
        state = setup.getState();
        leader = setup.getLeader();
        tracker.reset(leader);
        writeQuorums = new HashMap<>();
        acceptQuorums = new HashMap<>();
        ets = setup.getEts();        aborted = false;
    }

    public void uponPropose(ECPropose propose, short source){
        if(state.getVal().equals("")){
            proposal = propose.getVal();
        }

        HashMap<String,String> msg = new HashMap<>();
        msg.put("ts",Integer.toString(ets));
        for(Proc p : peers){
            sendMessage("read", msg,p,PROTO_ID);
        }
    }

    public void uponReadMsg(CommunicationReply reply){
        sendRequest(new CCSetup(leader,ets,proposal), ConditionalCollect.PROTO_ID);
        EpochState temp = new EpochState(state.getTs(),state.getVal(),state.getWs());
        if(leader.equals(self)&&state.getVal().equals("")){
            temp.setVal(proposal);
        }
        sendRequest(new InputRequest(temp),ConditionalCollect.PROTO_ID);
    }

    public void uponCCCollect(CollectedOutput collected, short source){
        String tmpval = null;
        tracker.leaderProposed(collected.getProposed());
        //insert all collected messages into the tracker to identify the binded value or conclude its
        //unbound incase none is found
        for(Map.Entry<Proc,String> entry : collected.getM().entrySet()){
            tracker.insert(entry.getKey(),entry.getValue());
        }
        if(tracker.getBindVal()!=null){
             tmpval = tracker.getBindVal();
        }
        else if(tracker.satisfied()&&collected.getM().containsKey(leader)){
            EpochState leaderState = new EpochState(collected.getM().get(leader));
            if(!leaderState.isInvalid()){
                tmpval = leaderState.getVal();
            }
        }
        if(tmpval!=null){
            ArrayList<Integer> to_del = new ArrayList<>();
            for(Map.Entry<Integer,String> entry : state.getWs().entrySet()){
                if(entry.getValue().equals(tmpval)){
                    to_del.add(entry.getKey());
                }
            }
            for(Integer i : to_del){
                state.getWs().remove(i);
            }
            state.getWs().put(ets,tmpval);
            HashMap<String,String> msg = new HashMap<>();
            msg.put("ts",Integer.toString(ets));
            msg.put("v",tmpval);
            for(Proc p : peers){
                sendMessage("write",msg,p,PROTO_ID);
            }
        }
    }
    public void uponWriteMsg(CommunicationReply reply){
        String v = reply.getMsg().get("v");
        if(writeQuorums.
                computeIfAbsent(v,(String val)-> quorums.get_marked()).
                mark_proc(reply.getOrigin())){
            state.setVal(v);
            state.setTs(ets);
            writeQuorums = new HashMap<>();
            HashMap<String,String> msg = new HashMap<>();
            msg.put("v",v);
            msg.put("ts",Integer.toString(ets));
            for(Proc p : peers){
                sendMessage("accept",msg,p,PROTO_ID);
            }
        }
    }

    public void uponAcceptMsg(CommunicationReply reply){
        String v = reply.getMsg().get("v");
        if(acceptQuorums.
                computeIfAbsent(v,(String val)->quorums.get_marked()).
                mark_proc(reply.getOrigin())){
            acceptQuorums = new HashMap<>();
            sendReply(new ECDecide(v,ets), output_proto);
        }
    }

    public void uponAbort(ECAbort abort, short source){
        sendReply(new ECAborted(state,ets),output_proto);
        aborted = true;
    }



}
