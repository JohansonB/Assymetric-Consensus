package binaryconsensus;

import abvbroadcast.MultiplexAbvBroadcast;
import abvbroadcast.MultiplexAbvBroadcastRequest;
import abvbroadcast.MultiplexAbvDeliver;
import abvbroadcast.abvDeliver;
import commoncoin.OutputCoinReply;
import commoncoin.ReleaseCoinRequest;
import commoncoin.UnsafeCommonCoinProtocol;
import communication.*;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import trustsystem.KernelSystem;
import trustsystem.MarkedProcSystem;
import trustsystem.Proc;
import trustsystem.TrustSystem;
import utils.CollectionSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public class BinaryConsensusProtocol extends GenericProtocol {
    private static final String PROTO_NAME = "binaryconsensus";
    private static final short PROTO_ID = 104;
    Proc self;
    int round = 0;
    HashSet<Boolean> values = new HashSet<>();
    HashMap<Proc,HashSet<Boolean>> aux = new HashMap<>();
    HashMap<Proc,Boolean> decided = new HashMap<>();
    boolean sentdecide = false;
    boolean coinreleased = false;
    boolean halted = false;

    private MarkedProcSystem zero_marked_kernel;
    private MarkedProcSystem one_marked_kernel;
    private MarkedProcSystem zero_marked_quorum_aux;
    private MarkedProcSystem one_marked_quorum_aux;
    private MarkedProcSystem zero_marked_quorum_decided;
    private MarkedProcSystem one_marked_quorum_decided;

    HashSet<Proc> peers;
    TrustSystem trustsystem;
    KernelSystem kernelSystem;
    short output_proto;
    public BinaryConsensusProtocol() {
        super(PROTO_NAME, PROTO_ID);
    }


    @Override
    public void init(Properties properties) throws IOException, HandlerRegistrationException {
        self = Proc.parse(properties.getProperty("self"));
        output_proto = new Short(properties.getProperty("output_proto"));
        ArrayList<String> peer_codes = CollectionSerializer.flatten_collection("peers");
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            aux.put(p,new HashSet<>());
            peers.add(p);
        }

        trustsystem = TrustSystem.parse(properties.getProperty("trustsystem"));
        kernelSystem = trustsystem.get_quorums().get_kernel_system();

        zero_marked_kernel = kernelSystem.get_marked();
        one_marked_kernel = kernelSystem.get_marked();
        zero_marked_quorum_aux = trustsystem.get_quorums().get_marked();
        one_marked_quorum_aux = trustsystem.get_quorums().get_marked();
        zero_marked_quorum_decided = trustsystem.get_quorums().get_marked();
        one_marked_quorum_decided = trustsystem.get_quorums().get_marked();


        registerRequestHandler(ACProposeRequest.REQUEST_ID,this::uponPropose);
        registerReplyHandler(OutputCoinReply.REPLY_ID,this::uponCoinOutput);
        registerReplyHandler(abvDeliver.REPSONSE_ID,this::uponAbvDeliver);
        registerReplyHandler(CommunicationReply.REPLY_ID,this::messageHandler);

    }

    private void uponPropose(ACProposeRequest request, short sourceProtocol){
        if(halted){
            return;
        }
        sendRequest(new MultiplexAbvBroadcastRequest(new HashMap<>(),request.getInput(),round), MultiplexAbvBroadcast.PROTO_ID);
    }

    private void uponAbvDeliver(MultiplexAbvDeliver reply, short sourceProtocol){
        if(halted){
            return;
        }
        if(reply.getRound()!=round){
            return;
        }
        values.add(reply.getOutput());
        HashMap<String,String> msg = new HashMap<>();
        msg.put("type","aux");
        msg.put("round",Integer.toString(round));
        msg.put("b",Boolean.toString(reply.getOutput()));
        for(Proc p : peers) {
            sendRequest(new SendMessageRequest(msg,new HashMap<>(),p,PROTO_ID), CommunicationProtocol.PROTO_ID);
        }
    }
    private void messageHandler(CommunicationReply reply,short sourceProtocol){
        HashMap<String,String> msg = reply.getMsg();
        String type = msg.get("type");
        Proc p = reply.getOrigin();
        boolean b = new Boolean(msg.get("b"));
        if(halted){

        }
        else if(type.equals("aux")){
            int r = new Integer(msg.get("round"));
            if(round==r){
                aux.get(p).add(b);
                MarkedProcSystem cur_marked_quorum = b ? one_marked_quorum_aux : zero_marked_quorum_aux;
                if(cur_marked_quorum.mark_proc(p)&&!coinreleased&&values.contains(b)){
                    coinreleased = true;
                    sendRequest(new ReleaseCoinRequest(round), UnsafeCommonCoinProtocol.PROTO_ID);
                }
            }
        }
        else if(type.equals("decide")){
            if(!decided.containsKey(p)){
                decided.put(p,b);
                MarkedProcSystem cur_marked_quorum = b ? one_marked_quorum_decided : zero_marked_quorum_decided;
                MarkedProcSystem cur_marked_kernel = b ? one_marked_kernel : zero_marked_kernel;
                if(!sentdecide&&cur_marked_kernel.mark_proc(p)){
                    HashMap<String,String> decide_msg = new HashMap<>();
                    msg.put("type","decide");
                    msg.put("b",Boolean.toString(b));
                    for(Proc q : peers){
                        sendRequest(new SendMessageRequest(decide_msg, new HashMap<>(), q, PROTO_ID), CommunicationProtocol.PROTO_ID);
                    }
                    sentdecide = true;
                }
                if(cur_marked_quorum.mark_proc(p)){
                    sendReply(new ACDecide(b),output_proto);
                    halted = true;
                }
            }
        }
        sendReply(new MessageACK(self),CommunicationProtocol.PROTO_ID);
    }
    private void uponCoinOutput(OutputCoinReply reply, short sourceProtocol){
        if(halted){
            return;
        }
        int r = reply.getRound();
        if(r!=round){
            return;
        }
        boolean s = reply.getOutput();
        round++;
        if(zero_marked_quorum_aux.getPSetFound()!=one_marked_quorum_aux.getPSetFound()){
            boolean b = one_marked_quorum_aux.getPSetFound() ? true : false;
            if(b==s && !sentdecide){
                HashMap<String,String> msg = new HashMap<>();
                msg.put("type","decide");
                msg.put("b",Boolean.toString(b));
                for(Proc p : peers) {
                    sendRequest(new SendMessageRequest(msg, new HashMap<>(), p, PROTO_ID), CommunicationProtocol.PROTO_ID);
                }
                sentdecide = true;
            }
            sendRequest(new MultiplexAbvBroadcastRequest(new HashMap<>(),b,round), MultiplexAbvBroadcast.PROTO_ID);
        }
        else{
            sendRequest(new MultiplexAbvBroadcastRequest(new HashMap<>(),s,round), MultiplexAbvBroadcast.PROTO_ID);
        }
        values = new HashSet<>();
        for(Proc p : peers){
            aux.put(p,new HashSet<>());
        }
        coinreleased = false;
    }

}
