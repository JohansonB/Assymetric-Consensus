package debugging;

import communication.AuthenticatedChannel;
import communication.PendingChannel;
import communication.reply.CommunicationReply;
import communication.request.SendMessageRequest;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public class AutLinkTester extends GenericProtocol {
    public static final short PROTO_ID = 205;
    public static final String PROTO_NAME = "authlinktester";
    HashSet<Proc> peers = new HashSet<>();
    Proc self;
    public AutLinkTester(){
        super(PROTO_NAME,PROTO_ID);
    }
    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        self = Proc.parse(properties.getProperty("self"));

        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }
        registerReplyHandler(CommunicationReply.REPLY_ID,this::uponDeliver);
        for(Proc p : peers){

            HashMap<String,String> msg = new HashMap();
            msg.put("sender",self.toString());
            msg.put("receiver",p.toString());
            sendRequest(new SendMessageRequest(msg,new HashMap<>(),p,PROTO_ID),AuthenticatedChannel.PROTO_ID);
        }


    }

    private void uponDeliver(CommunicationReply reply, short sourceProtocol){
        System.out.println(reply.getMsg().toString());
    }



    public static void main(String[] args) throws InvalidParameterException, IOException, HandlerRegistrationException, ProtocolAlreadyExistsException {


        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);


        AuthenticatedChannel autChan = new AuthenticatedChannel();
        AutLinkTester tester = new AutLinkTester();

        //Registers the protocol in babel
        babel.registerProtocol(tester);
        babel.registerProtocol(autChan);

        //Initializes the protocol
        autChan.init(props);
        tester.init(props);


        //Starts babel
        babel.start();
    }
}
