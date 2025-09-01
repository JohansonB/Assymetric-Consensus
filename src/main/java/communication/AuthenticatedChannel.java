package communication;

import communication.reply.*;
import communication.request.*;
import org.apache.logging.log4j.core.config.Configurator;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.network.data.Host;
import randomizedconsensus.commoncoin.UnsafeCommonCoinProtocol;
import trustsystem.Proc;
import utils.MACUtils;
import utils.SerializerTools;

import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

public class AuthenticatedChannel extends GenericProtocol {
    private static final String PROTO_NAME = "authenticatedChannel";
    public static final short PROTO_ID = 107;


    Proc self;

    ArrayList<Proc> peers = new ArrayList<>();
    Short dest_proto = null;
    PrivateKey my_key;
    HashMap<Proc, PublicKey> peer_keys = new HashMap<>();
    HashMap<Proc,Queue<CommunicationReply>> pending_replies = new HashMap<>();
    HashMap<Proc,ArrayList<PublicKeyRequest>> pending_key_requests = new HashMap<>();
    int run_id;

    public AuthenticatedChannel(){
        super(PROTO_NAME,PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        Configurator.setLevel("AuthenticatedChannel", org.apache.logging.log4j.Level.DEBUG);


        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        run_id = Integer.parseInt(properties.getProperty("run_id"));
        if(properties.containsKey("dest_proto")) {
            dest_proto = new Short(properties.getProperty("dest_proto"));
        }
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }

        registerRequestHandler(SendMessageRequest.REQUEST_ID,this::uponMessageRequest);
        registerReplyHandler(CommunicationReply.REPLY_ID,this::uponMessageReply);

        registerRequestHandler(SetRunId.REQUEST_ID,this::uponSetRunId);

        registerRequestHandler(ReInitKeyRequest.REQUEST_ID,this::uponReInitKeys);

        //adding the possibility of retrieving the keys from the AuthenticatedChannel layer so no further key pairs
        //need to be generated and distributed
        registerRequestHandler(PrivateKeyRequest.REQUEST_ID,this::uponPrivateKeyRequest);
        registerRequestHandler(PublicKeyRequest.REQUEST_ID,this::uponPublicKeyRequest);

        KeyPair pair = MACUtils.generateKeyPair();
        my_key = pair.getPrivate();
        peer_keys.put(self,pair.getPublic());
        HashMap<String,String> msg = new HashMap<>();
        msg.put("public key",MACUtils.encodePublicKey(pair.getPublic()));

        for(Proc p : peers){
            pending_replies.put(p,new LinkedList<>());
            sendRequest(new SendMessageRequest(msg,new HashMap<>(),p,PROTO_ID), PROTO_ID);
        }

        try {
            properties.setProperty("dest_proto",Short.toString(PROTO_ID));
            PendingChannel p_c = new PendingChannel();
            Babel.getInstance().registerProtocol(p_c);
            p_c.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }


    }

    private void uponAllKeysUpRequest(AllKeysUp k, short source){
        sendReply(new AllKeysUpReply(peer_keys.size()==peers.size()),source);
    }

    private void uponReInitKeys(ReInitKeyRequest req, short id){
        peer_keys = new HashMap<>();
        KeyPair pair = MACUtils.generateKeyPair();
        my_key = pair.getPrivate();
        HashMap<String,String> msg = new HashMap<>();
        msg.put("public key",MACUtils.encodePublicKey(pair.getPublic()));

        for(Proc p : peers){
            pending_replies.put(p,new LinkedList<>());
            sendRequest(new SendMessageRequest(msg,new HashMap<>(),p,PROTO_ID), PROTO_ID);
        }
        sendReply(new ReInitKeyAck(), id);


    }

    private void uponSetRunId(SetRunId setReq, short source){
        run_id = setReq.getRunId();
        sendReply(new IdUpdatedReply(),setReq.getOriginProto());
    }

    private void uponPrivateKeyRequest(PrivateKeyRequest req, short sourceProto){
        sendReply(new PrivateKeyReply(my_key),sourceProto);
    }
    private void uponPublicKeyRequest(PublicKeyRequest req, short sourceProto){
        if(!peer_keys.containsKey(req.getP())){
            pending_key_requests
                    .computeIfAbsent(req.getP(), k -> new ArrayList<>())
                    .add(req);
            return;
        }
        handlePublicKeyRequest(req);
    }

    private void handlePublicKeyRequest(PublicKeyRequest req) {
        sendReply(new PublicKeyReply(peer_keys.get(req.getP()),req.getP()),req.getSource_proto());
    }

    private void uponMessageRequest(SendMessageRequest messageRequest, short sourceProtocol){
        HashMap<String,String> payload = messageRequest.getPayload();
        payload.put("run_id",Integer.toString(run_id));
        String to_sign = MACUtils.hashMapToSortedString(messageRequest.getMessage())+"&"+self.getId();
        payload.put("signature",MACUtils.signMessage(to_sign,my_key));
        sendRequest(messageRequest,PendingChannel.PROTO_ID);
    }

    private void uponMessageReply(CommunicationReply reply, short sourceProtocol){
        String sign = reply.getPayload().get("signature");
        String r_id = reply.getPayload().get("run_id");
        if(Integer.parseInt(r_id) != run_id){
            System.out.println("denied message"+reply.getMsg());
            return;
        }
        String to_sign = MACUtils.hashMapToSortedString(reply.getMsg())+"&"+reply.getOrigin().getId();
        //handle the case of receiving the public key
        if(reply.getDestination_proto() == PROTO_ID && reply.getMsg().containsKey("public key")){
            PublicKey p_b = MACUtils.decodePublicKey(reply.getMsg().get("public key"));
            if(MACUtils.verifySignature(to_sign,sign,p_b)){
                peer_keys.put(reply.getOrigin(),p_b);
                for(PublicKeyRequest req : pending_key_requests.getOrDefault(reply.getOrigin(),new ArrayList<>())){
                    handlePublicKeyRequest(req);
                }
                pending_key_requests.remove(reply.getOrigin());
            }
            else{
                return;
            }
        }
        Queue<CommunicationReply> pending_q = pending_replies.get(reply.getOrigin());
        //if we have not yet received the public key yet the message is buffered
        if(!peer_keys.containsKey(reply.getOrigin())){
            pending_q.add(reply);
            return;
        }
        send_pending(pending_q);
        //only send message if it is not a channel layer message
        if(reply.getDestination_proto() != PROTO_ID&&MACUtils.verifySignature(to_sign,sign,peer_keys.get(reply.getOrigin()))) {

            sendReply(reply, reply.getDestination_proto());
        }

    }

    private void send_pending(Queue<CommunicationReply> pending_q) {
        CommunicationReply cur_reply;
        String cur_sign;
        String cur_to_sign;
        while (!pending_q.isEmpty()){
            cur_reply = pending_q.remove();
            cur_sign = cur_reply.getPayload().get("signature");
            cur_to_sign = MACUtils.hashMapToSortedString(cur_reply.getMsg())+"&"+cur_reply.getOrigin().getId();
            if(MACUtils.verifySignature(cur_to_sign,cur_sign,peer_keys.get(cur_reply.getOrigin()))) {
                Short d_p = dest_proto;
                if(d_p == null){
                    d_p = cur_reply.getDestination_proto();
                }
                sendReply(cur_reply, d_p);
            }
        }
    }
}
