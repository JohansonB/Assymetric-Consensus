package benchmarking;

import communication.AuthenticatedChannel;
import communication.CommunicationProtocolMessage;
import communication.PendingChannel;
import communication.PendingTimer;
import communication.reply.CommunicationReply;
import communication.reply.ConnectionsClosedReply;
import communication.request.CloseConnectionsRequest;
import conditionalproto.ConditionHandler;
import conditionalproto.ConditionalCommunicationHandler;
import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import recpbft.PBFT;
import recpbft.PBFTDecide;
import recpbft.PBFTPropose;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.network.data.Host;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.util.*;

public class RecPbftReplica extends ConditionalGenericProtocol {
    public static final String PROTO_NAME = "RecPbftReplica";
    public static final short PROTO_ID = 601;

    HashSet<Proc> peers = new HashSet<>();

    HashSet<Proc> is_setup = new HashSet<>();

    public static GenericProtocol consensus_protocol = new PBFT();

    boolean v;
    boolean proposed = false;

    int run_id;

    Proc client;

    public RecPbftReplica() {
        super(PROTO_NAME,PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {



        properties.setProperty("output_proto",Short.toString(PROTO_ID));
        client = Proc.parse(properties.getProperty("client"));

        run_id = Integer.parseInt(properties.getProperty("run_id"));

        ConditionalCommunicationHandler startupMsgHandler = this::uponStartupMsg;
        short startupMsgHandlerId = handler_map.register(startupMsgHandler);

        ConditionalReplyHandler<PBFTDecide> decideHandler = this::uponPBFTDecide;
        short decideHandlerId = handler_map.register(decideHandler);

        ConditionalReplyHandler<ConnectionsClosedReply> connectionsClosedHandler = this::uponConnectionsClosed;
        short connectionsClosedHandlerId = handler_map.register(connectionsClosedHandler);


        ConditionHandler uponIsReadyHandler = this::uponReady;
        short uponIsReadyHandlerId = handler_map.register(uponIsReadyHandler);

        ConditionalCommunicationHandler isSetupHandler = this::uponIsSetupMsg;
        short isSetupHandlerId = handler_map.register(isSetupHandler);

        ConditionalCommunicationHandler uponShutdownMsgHandler = this::uponShutdownMsg;
        short uponShutdownMsgHandlerId = handler_map.register(uponShutdownMsgHandler);

        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }


        ArrayList<String> peers_clone = SerializerTools.decode_collection(properties.getProperty("peers"));
        peers_clone.add(client.toString());
        Properties properties1 = (Properties) properties.clone();
        properties1.setProperty("peers",SerializerTools.encode_collection(peers_clone));

        AuthenticatedChannel aut = new AuthenticatedChannel();
        setupCommunicationChannel(aut,properties1);

        registerTimerHandler(PendingTimer.TIMER_ID, this::uponNextPendingTimer);
        setupPeriodicTimer(new PendingTimer(),5000,5000);



        registerCommunicationReplyHandler(
                "start",
                (v -> true),
                startupMsgHandlerId
        );

        registerCommunicationReplyHandler(
                "shutdown",
                (v -> true),
                uponShutdownMsgHandlerId
        );

        registerCommunicationReplyHandler(
                "isSetup",
                (v -> true),
                isSetupHandlerId
        );
        registerEndlessConditionHandler(
                (v -> is_setup.size()==peers.size()),
                uponIsReadyHandlerId,
                isSetupHandlerId
        );


        registerReplyHandler(
                PBFTDecide.REPLY_ID,
                v -> true,
                decideHandlerId
        );
        registerReplyHandler(
                ConnectionsClosedReply.REPLY_ID,
                v -> true,
                connectionsClosedHandlerId
        );
        try {
            Babel.getInstance().registerProtocol(consensus_protocol);
            consensus_protocol.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }
        /*try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/


        HashMap<String,String> msg = new HashMap<>();
        for(Proc p : peers){
            sendMessage("isSetup",msg,p,RecPbftReplica.PROTO_ID);
        }


    }

    private void uponShutdownMsg(CommunicationReply rep){
        sendRequest(new CloseConnectionsRequest(), PendingChannel.PROTO_ID);
    }

    private void uponConnectionsClosed(ConnectionsClosedReply rep, short source){
        System.exit(0);
    }

    private void uponNextPendingTimer(PendingTimer timer, long timerId) {
        System.out.println(super.communication_event_queue);
        System.out.println(super.communication_to_deliver_queue);
    }

    private void uponIsSetupMsg(CommunicationReply reply){
        is_setup.add(reply.getOrigin());
        System.out.println(is_setup.size());
    }

    private void uponReady() {
        HashMap<String,String> msg = new HashMap<>();
        msg.put("run_id",Integer.toString(run_id));
        sendMessage("isSetup",msg,client,RecPbftClient.PROTO_ID);
    }


    private void uponStartupMsg(CommunicationReply reply){
        v = new Boolean(reply.getMsg().get("v"));
        if(!proposed){
            proposed = true;
            sendRequest(new PBFTPropose(Boolean.toString(v)),consensus_protocol.getProtoId());
        }
    }
    private void uponPBFTDecide(PBFTDecide dec, short i){
        HashMap<String,String> msg = new HashMap<>();
        msg.put("run_id",Integer.toString(run_id));
        msg.put("decision",dec.getVal());
        sendMessage("decided",msg,client,RecPbftClient.PROTO_ID);
    }
    public static void main(String args[]) throws IOException, InvalidParameterException, ProtocolAlreadyExistsException, HandlerRegistrationException {
        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

        RecPbftReplica r = new RecPbftReplica();

        babel.registerProtocol(r);

        r.init(props);

        babel.start();
    }
}
