package communication;
import communication.reply.CommunicationReply;
import communication.reply.MessageACK;
import communication.request.SendMessageRequest;
import org.apache.logging.log4j.core.config.Configurator;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class CommunicationProtocol  extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(CommunicationProtocol.class);
    private static final int DEFAULT_QUEUE_MILLIS = 100;


    private static final String PROTO_NAME = "communication";
    public static final short PROTO_ID = 103;
    Proc self;
    Host self_h;
    int queue_millis;
    boolean queue_timer_running;


    HashMap<Proc,Boolean> pending_acks = new HashMap<>();
    HashMap<Proc,Queue<CommunicationReply>> reply_queues = new HashMap<>();

    ArrayList<Proc> peers = new ArrayList<>();
    HashMap<Proc,Host> peer_h = new HashMap<>();
    HashMap<Host,Proc> h_p_map = new HashMap<>();
    HashSet<Host> pending = new HashSet<>();
    HashSet<Host> connected = new HashSet<>();
    HashMap<Host, Queue<CommunicationProtocolMessage>> pending_msg_queue = new HashMap<>();



    protected int channelId;

    public CommunicationProtocol(){
        super(PROTO_NAME,PROTO_ID);
    }
    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        Configurator.setLevel("CommunicationProtocol", org.apache.logging.log4j.Level.DEBUG);

        if(properties.containsKey("queue_millis")){
            queue_millis = new Integer(properties.getProperty("queue_millis"));
        }
        else{
            queue_millis = DEFAULT_QUEUE_MILLIS;
        }


        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }

        Properties channelProps = new Properties();
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, self.getAddress());
        channelProps.setProperty(TCPChannel.PORT_KEY, Integer.toString(self.getPort()));
        this.channelId = createChannel(TCPChannel.NAME, channelProps);
        self_h = new Host(InetAddress.getByName(channelProps.getProperty(TCPChannel.ADDRESS_KEY)),
                Short.parseShort(channelProps.getProperty(TCPChannel.PORT_KEY)));

        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

        registerMessageHandler(this.channelId,CommunicationProtocolMessage.MSG_ID,this::uponCommunicationProtocolMessage,this::uponMessageFail);

        registerRequestHandler(SendMessageRequest.REQUEST_ID,this::uponMessageRequest);
        registerReplyHandler(MessageACK.ACK_ID,this::uponMessageACK);

        registerMessageSerializer(channelId, CommunicationProtocolMessage.MSG_ID, new CommunicationProtocolMessageSerializer());

        registerTimerHandler(PendingTimer.TIMER_ID, this::uponNextPendingTimer);
        setupPeriodicTimer(new PendingTimer(),queue_millis,queue_millis);
        queue_timer_running = true;
        Host h;

        for(Proc p : peers){
            h = new Host(InetAddress.getByName(p.getAddress()),p.getPort());
            reply_queues.put(p,new LinkedList<>());
            pending_acks.put(p,false);
            peer_h.put(p,h);
            h_p_map.put(h,p);
            pending_msg_queue.put(peer_h.get(p),new LinkedList<>());
            if(!p.equals(self)) {
                pending.add(peer_h.get(p));
                openConnection(peer_h.get(p));
            }
        }

    }

    private void uponMessageACK(MessageACK ack, short sourceProtocol) {
        Proc origin = ack.getReply().getOrigin();
        pending_acks.put(origin,false);
        Queue<CommunicationReply> cur_q = reply_queues.get(origin);

        if(!cur_q.isEmpty()){
            CommunicationReply reply = cur_q.remove();
            pending_acks.put(origin,true);
            sendReply(reply,reply.getDestination_proto());
        }
    }

    private void uponNextPendingTimer(PendingTimer timer, long timerId) {
        Queue<CommunicationProtocolMessage> cur_q;
        for(Host h : connected){
            cur_q = pending_msg_queue.get(h);
            while(!cur_q.isEmpty()){
                sendMessage(cur_q.remove(),h);
            }
        }
        if(pending.isEmpty()){
            cancelTimer(timerId);
            queue_timer_running = false;
        }

    }

    private void uponMessageRequest(SendMessageRequest messageRequest, short sourceProtocol){
        Proc dest = messageRequest.getDestination();
        Host dest_h = peer_h.get(dest);
        Queue<CommunicationProtocolMessage> cur_q = pending_msg_queue.get(dest_h);
        CommunicationProtocolMessage msg = new CommunicationProtocolMessage(self,dest,messageRequest.getMessage(),messageRequest.getPayload(),messageRequest.getDestination_proto());

        if(dest.equals(self)){
            Queue<CommunicationReply> cur_reply_q = reply_queues.get(self);
            CommunicationReply reply = new CommunicationReply(msg.getMessage(),msg.getPayload(),self,msg.getDestination_proto());
            cur_reply_q.add(reply);
            if(!pending_acks.get(self)){
                reply = cur_reply_q.remove();
                pending_acks.put(self,true);
                sendReply(reply,reply.getDestination_proto());
            }
            return;
        }


        if(pending.contains(dest_h)){
            cur_q.add(msg);
            return;
        }
        while(!cur_q.isEmpty()){
            sendMessage(cur_q.remove(), dest_h);
        }
        sendMessage(msg,dest_h);


    }

    private void uponCommunicationProtocolMessage(CommunicationProtocolMessage msg, Host from, short source_proto, int channelId){
        CommunicationReply reply = new CommunicationReply(msg.getMessage(),msg.getPayload(),h_p_map.get(from),msg.getDestination_proto());
        Proc origin = h_p_map.get(from);
        Queue<CommunicationReply> cur_q = reply_queues.get(origin);
        cur_q.add(reply);
        if(!pending_acks.get(origin)){
            reply = cur_q.remove();
            pending_acks.put(origin,true);
            sendReply(reply,reply.getDestination_proto());
        }
    }

    private void uponMessageFail(ProtoMessage msg, Host host, short destProto, Throwable throwable, int channelId){
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }

    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        Host peer = event.getNode();
        pending.remove(peer);
        connected.add(peer);
        logger.debug("Out Connection to {} is up", peer);
    }

    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        Host peer = event.getNode();
        connected.remove(peer);
        pending.add(peer);
        openConnection(peer);
        if(!queue_timer_running){
            queue_timer_running = true;
            setupPeriodicTimer(new PendingTimer(),queue_millis,queue_millis);
        }
        logger.debug("Connection to {} is down cause {}", peer, event.getCause());
    }

    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
        logger.debug("Connection to {} failed cause: {}", event.getNode(), event.getCause());
        Host peer = event.getNode();
        connected.remove(peer);
        pending.add(peer);
        openConnection(peer);
        if(!queue_timer_running){
            queue_timer_running = true;
            setupPeriodicTimer(new PendingTimer(),queue_millis,queue_millis);
        }
    }

    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.trace("Connection from {} is up", event.getNode());
    }

    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        logger.trace("Connection from {} is down, cause: {}", event.getNode(), event.getCause());
    }



}
