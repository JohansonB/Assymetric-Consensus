package debugging;

import communication.*;
import communication.reply.CommunicationReply;
import communication.reply.MessageACK;
import communication.request.SendMessageRequest;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class CommunicationProtocolTester1 extends GenericProtocol {
    public static final short PROTO_ID = 202;
    public static final String PROTO_NAME = "CommTester1";
    CommunicationProtocolTester1(){
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


}
