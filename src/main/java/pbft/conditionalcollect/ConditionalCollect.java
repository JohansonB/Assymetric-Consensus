package pbft.conditionalcollect;

import communication.AuthenticatedChannel;
import communication.reply.CommunicationReply;
import communication.reply.PrivateKeyReply;
import communication.reply.PublicKeyReply;
import communication.request.PrivateKeyRequest;
import communication.request.PublicKeyRequest;
import conditionalproto.*;
import pbft.EpochState;
import pbft.EpochStatePredicateTracker;
import pbft.PredicateTracker;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import trustsystem.*;
import utils.MACUtils;
import utils.SerializerTools;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class ConditionalCollect  extends ConditionalGenericProtocol {
    private static final String PROTO_NAME = "ConditionalCollect";
    public static final short PROTO_ID = 110;



    Proc self;
    Proc cur_leader = null;
    boolean collected = false;
    PredicateTracker p_tracker;
    int ts;

    //holds the message sent by the leader has to be stored since we might not yet have all the public keys so we
    //have to verify gradually
    //assuming that messages in M are encoding a Hashmap<String,String> with entries "ts", an integer; "val", a String;
    //ws, a set of "ts" "val" pairs
    HashMap<Proc,String> M = new HashMap<>();
    HashMap<Proc,String> sigma = new HashMap<>();



    AsymmetricTrustSystem asymmTrustSystem;
    ToleratedSystem toleratedSystem;
    PrivateKey my_key = null;
    HashMap<Proc, PublicKey> peer_keys = new HashMap<>();


    short output_proto;

    boolean leader_b;

    String proposal;
    boolean proposed;

    ArrayList<Proc> peers = new ArrayList<>();

    public ConditionalCollect(){
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
            sendRequest(new PublicKeyRequest(p,PROTO_ID),getComProtoId());
        }
        sendRequest(new PrivateKeyRequest(),getComProtoId());

        output_proto = Short.parseShort(properties.getProperty("output_proto"));



        asymmTrustSystem = AsymmetricTrustSystem.parse(properties.getProperty("ats"));
        toleratedSystem = new ToleratedSystem(TrustSystem.parse(properties.getProperty("ts")));//asymmTrustSystem.get_tolerated_system();
        p_tracker = new EpochStatePredicateTracker(toleratedSystem,cur_leader);



        sendRequest(new PrivateKeyRequest(),getComProtoId());

        ConditionalRequestHandler<CCSetup> setupHandler = this::uponSetup;
        short setupHandlerId = handler_map.register(setupHandler);

        ConditionalReplyHandler<PublicKeyReply> publicKeyHandler = this::uponPublicKey;
        short publicKeyHandlerId = handler_map.register(publicKeyHandler);
        ConditionalReplyHandler<PrivateKeyReply> privateKeyHandler = this::uponPrivateKey;
        short privateKeyHandlerId = handler_map.register(privateKeyHandler);

        ConditionalRequestHandler<InputRequest> inputHandler = this::uponInput;
        short inputHandlerId = handler_map.register(inputHandler);

        ConditionalCommunicationHandler sendMsgHandler = this::uponGetSendMsg;
        short sendMsgHandlerId = handler_map.register(sendMsgHandler);

        ConditionHandler leaderBroadcastHandler = this::uponLeaderBroadcast;
        short leaderBroadcastHandlerId = handler_map.register(leaderBroadcastHandler);

        ConditionalCommunicationHandler collectedDeliverHandler = this::uponCollectedDeliver;
        short collectedDeliverHandlerId = handler_map.register(collectedDeliverHandler);

        ConditionHandler collectedOutputHandler = this::uponCollectedOutput;
        short collectedOutputHandlerId = handler_map.register(collectedOutputHandler);

        registerRequestHandler(
                CCSetup.REQUEST_ID,
                (CCSetup s) -> true,
                setupHandlerId
        );

        registerRequestHandler(
                InputRequest.REQUEST_ID,
                (InputRequest i) -> my_key!=null,
                inputHandlerId,
                privateKeyHandlerId
        );

        registerReplyHandler(
                PublicKeyReply.REPLY_ID,
                (PublicKeyReply p)->true,
                publicKeyHandlerId
        );

        registerReplyHandler(
                PrivateKeyReply.REPLY_ID,
                (PrivateKeyReply p) -> true,
                privateKeyHandlerId
        );

        registerCommunicationReplyHandler(
                "send",
                (CommunicationReply c) -> Integer.parseInt(c.getMsg().get("ts"))==ts&&!leader_b,
                sendMsgHandlerId,
                (CommunicationReply c) -> Integer.parseInt(c.getMsg().get("ts"))<ts,
                setupHandlerId
                );

        registerCommunicationReplyHandler(
                "collected",
                (CommunicationReply c)-> Integer.parseInt(c.getMsg().get("ts"))==ts&&c.getOrigin().equals(cur_leader),
                collectedDeliverHandlerId,
                (CommunicationReply c) -> Integer.parseInt(c.getMsg().get("ts"))<ts,
                setupHandlerId


        );

        registerEndlessConditionHandler(
                (Object o)->p_tracker.satisfied()&&!leader_b&&self.equals(cur_leader),
                leaderBroadcastHandlerId,
                sendMsgHandlerId, publicKeyHandlerId, setupHandlerId, leaderBroadcastHandlerId
        );

        registerEndlessConditionHandler(
                (Object o)->p_tracker.satisfied(),
                collectedOutputHandlerId,
                collectedDeliverHandlerId, publicKeyHandlerId
        );
    }

    private void uponPrivateKey(PrivateKeyReply privateKeyReply, short source) {
        my_key = privateKeyReply.getKey();
    }

    private void uponPublicKey(PublicKeyReply publicKeyReply, short source) {
        Proc p = publicKeyReply.getP();
        peer_keys.put(p,publicKeyReply.getKey());
        if(M.containsKey(p)){
            verify_M_entry(p);
        }
    }

    public void uponSetup(CCSetup s, short source){
        ts = s.getTs();
        cur_leader = s.getLeader();
        leader_b = false;
        collected = false;
        proposal = s.getProposal();
        p_tracker = new EpochStatePredicateTracker(toleratedSystem,cur_leader);


    }

    public void uponInput(InputRequest input, short source){
        String m = input.getState().toString();
        if(self.equals(cur_leader)){
            proposed = input.getState().getVal().equals(proposal);
            p_tracker.leaderProposed(proposed);
        }
        String message = "cc"+self.getId()+"INPUT"+m;
        String sing = MACUtils.signMessage(message,my_key);
        HashMap<String,String> msg = new HashMap<>();
        msg.put("m",m);
        msg.put("sign",sing);
        msg.put("ts",Integer.toString(ts));
        sendMessage("send",msg,cur_leader,PROTO_ID);
    }

    public void uponGetSendMsg(CommunicationReply reply){
        String m = reply.getMsg().get("m");
        Proc p = reply.getOrigin();
        String sign = reply.getMsg().get("sign");
        M.put(p,m);
        sigma.put(p,sign);
        if(peer_keys.containsKey(p)){
            verify_M_entry(p);
        }
    }
    public void verify_M_entry(Proc p){
        String m = M.get(p);
        String message = "cc"+p.getId()+"INPUT"+m;
        String sign = sigma.get(p);
        PublicKey key = peer_keys.get(p);
        if(!MACUtils.verifySignature(message,sign,key)||p_tracker.isInvalid(m)){
            M.remove(p);
            sigma.remove(p);
            return;
        }
        p_tracker.insert(p, m);

    }

    public void uponLeaderBroadcast(){
        HashMap<String,String> msg = new HashMap<>();
        msg.put("messages",SerializerTools.encode_hashmap(SerializerTools.proc_map_to_string_map(M)));
        msg.put("sigma",SerializerTools.encode_hashmap(SerializerTools.proc_map_to_string_map(sigma)));
        msg.put("proposed",Boolean.toString(proposed));
        msg.put("ts",Integer.toString(ts));
        leader_b = true;
        M = new HashMap<>();
        sigma = new HashMap<>();
        p_tracker.reset(cur_leader);
        for(Proc p : peers){
            sendMessage("collected",msg,p,PROTO_ID);
        }
    }

    public void uponCollectedDeliver(CommunicationReply reply){
        M = SerializerTools.string_map_to_proc_map(SerializerTools.decode_hashmap(reply.getMsg().get("messages")));
        sigma = SerializerTools.string_map_to_proc_map(SerializerTools.decode_hashmap(reply.getMsg().get("sigma")));
        proposed = Boolean.parseBoolean(reply.getMsg().get("proposed"));
        p_tracker.leaderProposed(proposed);
        for(Proc p : peers){
            if(M.containsKey(p)&&peer_keys.containsKey(p)){
                verify_M_entry(p);
            }
        }
    }

    public void uponCollectedOutput(){
        collected = true;
        p_tracker.reset(cur_leader);
        sendReply(new CollectedOutput(M,ts,proposed),output_proto);
    }

}
