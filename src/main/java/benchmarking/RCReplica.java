package benchmarking;

import communication.AuthenticatedChannel;
import communication.CommunicationProtocolMessage;
import communication.PendingTimer;
import communication.reply.CommunicationReply;
import conditionalproto.ConditionHandler;
import conditionalproto.ConditionalCommunicationHandler;
import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.network.data.Host;
import randomizedconsensus.ACDecide;
import randomizedconsensus.ACProposeRequest;
import randomizedconsensus.BinaryConsensusProtocol;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.util.*;

public class RCReplica extends ConditionalGenericProtocol {
    public static final String PROTO_NAME = "RCReplica";
    public static final short PROTO_ID = 401;

    HashSet<Proc> peers = new HashSet<>();

    HashSet<Proc> is_setup = new HashSet<>();

    public static GenericProtocol consensus_protocol = new BinaryConsensusProtocol();

    boolean v;
    boolean proposed = false;

    int run_id;

    Proc client;

    public RCReplica() {
        super(PROTO_NAME,PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {



        properties.setProperty("output_proto",Short.toString(PROTO_ID));
        client = Proc.parse(properties.getProperty("client"));

        run_id = Integer.parseInt(properties.getProperty("run_id"));



        ConditionalCommunicationHandler startupMsgHandler = this::uponStartupMsg;
        short startupMsgHandlerId = handler_map.register(startupMsgHandler);

        ConditionalReplyHandler<ACDecide> decideHandler = this::uponACDecide;
        short decideHandlerId = handler_map.register(decideHandler);


        ConditionHandler uponIsReadyHandler = this::uponReady;
        short uponIsReadyHandlerId = handler_map.register(uponIsReadyHandler);

        ConditionalCommunicationHandler isSetupHandler = this::uponIsSetupMsg;
        short isSetupHandlerId = handler_map.register(isSetupHandler);

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


        registerCommunicationReplyHandler(
                "start",
                (v -> true),
                startupMsgHandlerId
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
                ACDecide.REPLY_ID,
                v -> true,
                decideHandlerId
        );
        try {
            Babel.getInstance().registerProtocol(consensus_protocol);
            consensus_protocol.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        HashMap<String,String> msg = new HashMap<>();
        for(Proc p : peers){
            sendMessage("isSetup",msg,p,RCReplica.PROTO_ID);
        }


    }


    private void uponIsSetupMsg(CommunicationReply reply){
        is_setup.add(reply.getOrigin());
    }

    private void uponReady() {
        HashMap<String,String> msg = new HashMap<>();
        msg.put("run_id",Integer.toString(run_id));
        sendMessage("isSetup",msg,client,RCClient.PROTO_ID);
    }


    private void uponStartupMsg(CommunicationReply reply){
        v = new Boolean(reply.getMsg().get("v"));
        if(!proposed){
            proposed = true;
            sendRequest(new ACProposeRequest(v),consensus_protocol.getProtoId());
        }
    }
    private void uponACDecide(ACDecide dec, short i){
        HashMap<String,String> msg = new HashMap<>();
        msg.put("run_id",Integer.toString(run_id));
        msg.put("decision",Boolean.toString(dec.getOutput()));
        sendMessage("decided",msg,client,RCClient.PROTO_ID);
    }
    public static void main(String args[]) throws IOException, InvalidParameterException, ProtocolAlreadyExistsException, HandlerRegistrationException {
        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

        RCReplica r = new RCReplica();

        babel.registerProtocol(r);

        r.init(props);

        babel.start();
    }
}
