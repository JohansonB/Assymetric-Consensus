package pbft;

import conditionalproto.ConditionHandler;
import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import conditionalproto.ConditionalRequestHandler;
import pbft.conditionalcollect.ConditionalCollect;
import pbft.epochchange.EpochChange;
import pbft.epochchange.StartEpoch;
import pbft.epochconsensus.*;
import pbft.leaderdetection.ByzantineLeaderDetector;
import pbft.leaderdetection.Complain;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public class PBFT extends ConditionalGenericProtocol {
    public static class ComplainTimer extends ProtoTimer {
        public static final short TIMER_ID = 2;
        Proc l;

        public ComplainTimer(Proc l) {
            super(TIMER_ID);
            this.l = l;
        }



        @Override
        public ProtoTimer clone() {
            return this;
        }
    }

    public static final short PROTO_ID = 112;
    public static final String PROTO_NAME = "pbft";

    public static final long DEFAULT_DELTA = 200;

    long cur_timer_id;

    short output_proto;
    long delta;

    String val;
    boolean proposed = false;
    boolean decided = false;
    Proc l;
    int ets;
    int newts;
    Proc newl;
    Proc self;

    HashSet<Proc> peers = new HashSet<>();
    HashMap<Integer,Proc> id_peers = new HashMap<>();






    public PBFT(){
        super(PROTO_NAME,PROTO_ID);
    }
    public void init(Properties properties) throws HandlerRegistrationException, IOException {

        output_proto = Short.parseShort(properties.getProperty("output_proto"));
        self = Proc.parse(properties.getProperty("self"));
        val = null;
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        if(properties.containsKey("delta")){
            delta = (long)Double.parseDouble(properties.getProperty("delta"));
        }
        else{
            delta = DEFAULT_DELTA;
        }
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            peers.add(p);
            id_peers.put(p.getId(),p);

        }
        ets = 0;
        l = leader(0);
        newts = 0;
        newl = null;

        ConditionalRequestHandler<PBFTPropose> proposeHandler = this::uponPropose;
        short proposeHandlerId = handler_map.register(proposeHandler);

        ConditionalReplyHandler<StartEpoch> startEpochHandler = this::uponStartEpoch;
        short startEpochHandlerId = handler_map.register(startEpochHandler);

        ConditionalReplyHandler<ECAborted> abortedHandler = this::uponAborted;
        short abortedHandlerId = handler_map.register(abortedHandler);

        ConditionHandler proposeTurnHandler = this::handleProposeTurn;
        short proposeTurnHandlerId = handler_map.register(proposeTurnHandler);

        ConditionalReplyHandler<ECDecide> decideHandler = this::handleECDecide;
        short decideHandlerId = handler_map.register(decideHandler);

        registerRequestHandler(
                PBFTPropose.PROTO_REQUEST,
                (PBFTPropose p) -> true,
                proposeHandlerId
        );
        registerReplyHandler(
                StartEpoch.REPLY_ID,
                (StartEpoch se) -> true,
                startEpochHandlerId
        );
        registerReplyHandler(
                ECAborted.REPLY_ID,
                (ECAborted a) -> a.getTs()==ets,
                abortedHandlerId,
                (ECAborted a) -> a.getTs()<ets
        );
        registerEndlessConditionHandler(
                (Object o) -> l.equals(self)&&val!=null&&!proposed,
                proposeTurnHandlerId,
                abortedHandlerId,proposeHandlerId

        );
        registerReplyHandler(
                ECDecide.REPLY_ID,
                (ECDecide d) -> d.getTs() ==ets,
                decideHandlerId,
                (ECDecide d) -> d.getTs() < ets
        );
        try {
            properties.setProperty("output_proto",Short.toString(PROTO_ID));
            EpochConsensus ec = new EpochConsensus();
            Babel.getInstance().registerProtocol(ec);
            ec.init(properties);

            properties.setProperty("output_proto",Short.toString(PROTO_ID));
            EpochChange eChange = new EpochChange();
            Babel.getInstance().registerProtocol(eChange);
            eChange.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }


        registerTimerHandler(ComplainTimer.TIMER_ID,this::uponComplainTimer);
        sendRequest(new ECSetup(EpochState.init_state(),ets,l), EpochConsensus.PROTO_ID);
        cur_timer_id=setupTimer(new ComplainTimer(l),delta);



    }
    void uponComplainTimer(ComplainTimer t, long id){
        sendRequest(new Complain(t.l), ByzantineLeaderDetector.PROTO_ID);
    }

    void uponPropose(PBFTPropose propose, short source){
        val = propose.getValue();
    }

    void uponStartEpoch(StartEpoch reply, short source){
        newts = reply.getTs();
        newl = reply.getLeader();
        sendRequest(new ECAbort(), EpochConsensus.PROTO_ID);
    }

    void uponAborted(ECAborted aborted, short source){
        ets = newts;
        l = newl;
        proposed = false;
        sendRequest(new ECSetup(aborted.getState(),ets,l),EpochConsensus.PROTO_ID);
        cancelTimer(cur_timer_id);
        cur_timer_id = setupTimer(new ComplainTimer(l),delta*(ets+1));
    }
    void handleProposeTurn(){
        proposed = true;
        sendRequest(new ECPropose(val),EpochConsensus.PROTO_ID);
    }
    void handleECDecide(ECDecide reply, short source){
        if(!decided){
            decided = true;
            sendReply(new PBFTDecide(reply.getVal()),output_proto);
        }
    }

    private Proc leader(int round){
        return id_peers.get(round);
    }

}
