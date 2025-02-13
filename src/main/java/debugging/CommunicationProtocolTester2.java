package debugging;

import communication.CommunicationProtocol;
import communication.CommunicationReply;
import communication.MessageACK;
import communication.SendMessageRequest;
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
import java.util.Properties;

public class CommunicationProtocolTester2 extends GenericProtocol {
    public static final short PROTO_ID = 203;
    public static final String PROTO_NAME = "CommTester2";
    CommunicationProtocolTester2(){
        super(PROTO_NAME,PROTO_ID);
    }


    private ArrayList<Proc> peers = new ArrayList<>();
    private int count = 0;
    private String name;
    private Proc self;

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        name = properties.getProperty("name");
        self = Proc.parse(properties.getProperty("self"));

        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }

        registerReplyHandler(CommunicationReply.REPLY_ID,this::uponDeliverMsg);
        while(count<15) {
            send_msg_all();
        }

    }
    private void uponDeliverMsg(CommunicationReply reply, short sourceProtocol){
        String msg = reply.getMsg().get("msg");
        System.out.println(msg);
        sendReply(new MessageACK(reply),CommunicationProtocol.PROTO_ID);
    }

    void send_msg_all(){
        HashMap<String,String> to_send;
        count++;
        for(Proc p : peers) {
            to_send = new HashMap<>();
            to_send.put("msg",PROTO_NAME+" "+name+" "+count+" "+self+" "+p);
            sendRequest(new SendMessageRequest(to_send, new HashMap<>(),p,CommunicationProtocolTester1.PROTO_ID),CommunicationProtocol.PROTO_ID);
            sendRequest(new SendMessageRequest(to_send, new HashMap<>(),p,CommunicationProtocolTester2.PROTO_ID),CommunicationProtocol.PROTO_ID);

        }
    }

    public static void main(String[] args) throws InvalidParameterException, IOException, HandlerRegistrationException, ProtocolAlreadyExistsException {
        Proc p1 = new Proc(0, "127.0.0.1", 5000);
        Proc p2 = new Proc(1, "127.0.0.1", 5001);
        Proc p3 = new Proc(2, "127.0.0.1", 5002);

        ArrayList<Proc> peers = new ArrayList<>();
        peers.add(p1);
        peers.add(p2);
        peers.add(p3);


        System.out.println(p1);

        System.out.println(SerializerTools.encode_collection(peers));


        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

       CommunicationProtocolTester1 tester1 = new CommunicationProtocolTester1();
       CommunicationProtocolTester2 tester2 = new CommunicationProtocolTester2();
       CommunicationProtocol comm = new CommunicationProtocol();

        //Registers the protocol in babel
        babel.registerProtocol(tester1);
        babel.registerProtocol(tester2);
        babel.registerProtocol(comm);

        //Initializes the protocol
        comm.init(props);
        tester1.init(props);
        tester2.init(props);


        //Starts babel
        babel.start();
    }

}
