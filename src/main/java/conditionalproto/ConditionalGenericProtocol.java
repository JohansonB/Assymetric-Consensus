package conditionalproto;

import communication.reply.CommunicationReply;
import communication.request.SendMessageRequest;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.babel.handlers.ReplyHandler;
import pt.unl.fct.di.novasys.babel.handlers.RequestHandler;
import trustsystem.Proc;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public abstract class ConditionalGenericProtocol extends GenericProtocol {




    public static abstract class Event {
        short execute_id;
        Integer ts;

        Event(short execute_id) {
            this.execute_id = execute_id;
            this.ts = ts_factory++;
        }
    }

    public static class ConditionalRequest<V extends ProtoRequest> extends Event {
        Predicate<V> deliver_condition;
        ConditionalRequestHandler<V> execute_handler;

        Predicate<V> discard_condition;
        ConditionalRequestHandler<V> discard_handler;
        V request;
        short source;

        ConditionalRequest(Predicate<V> execute_condtion, ConditionalRequestHandler<V> execute_handler, V request, short execute_id, short source, Predicate<V> discard_condtion, ConditionalRequestHandler<V> discard_handler) {
            super(execute_id);
            this.deliver_condition = execute_condtion;
            this.execute_handler = execute_handler;
            this.discard_condition = discard_condtion;
            this.discard_handler = discard_handler;
            this.request = request;
            this.source = source;
        }
    }

    public static class ConditionalReply<V extends ProtoReply> extends Event {
        Predicate<V> deliver_condition;
        ConditionalReplyHandler<V> execute_handler;

        Predicate<V> discard_condition;
        ConditionalReplyHandler<V> discard_handler;
        V reply;
        short source;

        ConditionalReply(Predicate<V> execute_condtion, ConditionalReplyHandler<V> execute_handler, V reply, short execute_id, short source, Predicate<V> discard_condtion, ConditionalReplyHandler<V> discard_handler) {
            super(execute_id);
            this.deliver_condition = execute_condtion;
            this.execute_handler = execute_handler;
            this.discard_condition = discard_condtion;
            this.discard_handler = discard_handler;
            this.reply = reply;
            this.source = source;
        }
    }

    public static class CommunicationEvent extends Event {
        Predicate<CommunicationReply> deliver_condition;
        ConditionalCommunicationHandler execute_handler;

        Predicate<CommunicationReply> discard_condition;
        ConditionalCommunicationHandler discard_handler;

        CommunicationReply reply;
        String type;

        CommunicationEvent(Predicate<CommunicationReply> execute_condition, ConditionalCommunicationHandler execute_handler, CommunicationReply reply, short execute_id, Predicate<CommunicationReply> discard_condition, ConditionalCommunicationHandler discard_handler) {
            super(execute_id);
            this.deliver_condition = execute_condition;
            this.execute_handler = execute_handler;
            this.discard_condition = discard_condition;
            this.discard_handler = discard_handler;
            this.reply = reply;
            this.type = reply.getMsg().get("type");
        }

        CommunicationEvent(Predicate<CommunicationReply> execute_condtion, ConditionalCommunicationHandler execute_handler, String type, short execute_id, Predicate<CommunicationReply> discard_condition, ConditionalCommunicationHandler discard_handler) {
            super(execute_id);
            this.deliver_condition = execute_condtion;
            this.execute_handler = execute_handler;
            this.discard_condition = discard_condition;
            this.discard_handler = discard_handler;
            this.type = type;
        }
    }

    public static class ConditionEvent extends Event {
        Predicate execute_condition;
        ConditionHandler execute_handler;
        Predicate discard_condition;
        ConditionHandler discard_handler;

        ConditionEvent(Predicate execute_condition, ConditionHandler execute_handler, short execute_id,Predicate discard_condition, ConditionHandler discard_handler) {
            super(execute_id);
            this.execute_condition = execute_condition;
            this.execute_handler = execute_handler;

            this.discard_condition = discard_condition;
            this.discard_handler = discard_handler;


        }
    }


    private static int ts_factory = 0;
    short com_proto_id;
    HashMap<Short, LinkedList<ConditionalRequest>> request_event_queue = new HashMap<>();
    HashMap<Short, LinkedList<ConditionalRequest>> request_to_deliver_queue = new HashMap<>();

    HashMap<Short, LinkedList<ConditionalReply>> reply_event_queue = new HashMap<>();
    HashMap<Short, LinkedList<ConditionalReply>> reply_to_deliver_queue = new HashMap<>();

    public HashMap<Short, LinkedList<CommunicationEvent>> communication_event_queue = new HashMap<>();
    public HashMap<Short, LinkedList<CommunicationEvent>> communication_to_deliver_queue = new HashMap<>();

    HashMap<Short, LinkedList<ConditionEvent>> one_time_condition_event_queue = new HashMap<>();
    HashMap<Short, LinkedList<ConditionEvent>> one_time_condition_to_deliver_queue = new HashMap<>();

    HashMap<Short, LinkedList<ConditionEvent>> endless_condition_event_queue = new HashMap<>();
    HashMap<Short, LinkedList<ConditionEvent>> endless_condition_to_deliver_queue = new HashMap<>();


    HashMap<Short,List<Short>> dependency_map = new HashMap<>();
    HashMap<String,CommunicationEvent> type_handler_map = new HashMap<>();
    protected static HandlerMap handler_map = new HandlerMap();

    public ConditionalGenericProtocol(String protoName, short protoId) {
        super(protoName,protoId);
    }

    abstract public void init(Properties properties) throws HandlerRegistrationException, IOException;

    public short getComProtoId() {
        return com_proto_id;
    }

    // Overloaded registerRequestHandler methods to provide default inputs

    protected final <V extends ProtoRequest> void registerRequestHandler(
            short requestId, Predicate<V> execute_condition, short execute_handler, short... trigger_handlers)
            throws HandlerRegistrationException {
        registerRequestHandler(requestId, execute_condition, execute_handler, (V req) -> false, (V req, short src) -> {}, trigger_handlers);
    }

    protected final <V extends ProtoRequest> void registerRequestHandler(
            short requestId, Predicate<V> execute_condition, short execute_handler, Predicate<V> discard_condition, short... trigger_handlers)
            throws HandlerRegistrationException {
        registerRequestHandler(requestId, execute_condition, execute_handler, discard_condition, (V req, short src) -> {}, trigger_handlers);
    }

    protected final <V extends ProtoRequest> void registerRequestHandler(short requestId, Predicate<V> execute_condition, short execute_handler, Predicate<V> discard_condition, ConditionalRequestHandler<V> discard_handler, short ... trigger_handlers)
            throws HandlerRegistrationException {
        request_event_queue.put(execute_handler,new LinkedList<>());
        //add the handler id to the dependencies of s
        for(short s : trigger_handlers){
            dependency_map.computeIfAbsent(s,k -> new ArrayList<>()).add(execute_handler);
        }
        ConditionalRequestHandler<V> requestHandler2 = (V request, short source) -> {
           ((ConditionalRequestHandler)(handler_map.getHandlerById(execute_handler))).uponRequest(request,source);
           List<Short> list = dependency_map.get(execute_handler);
           if(list != null) {
               for (Short s : list) {
                   retest_predicate(s);
               }
           }
           deliver_next();

        };
        RequestHandler<V> requestHandler = (V request, short sourceProto) -> {
          if(discard_condition.test(request)){
              discard_handler.uponRequest(request,sourceProto);
              return;
          }
          if(!execute_condition.test(request)){
              request_event_queue.get(execute_handler).add(new ConditionalRequest(execute_condition,requestHandler2,request,execute_handler,sourceProto,discard_condition,discard_handler));
              deliver_next();
              return;
          }
          requestHandler2.uponRequest(request,sourceProto);
        };

        registerRequestHandler(requestId, requestHandler);
    }
    public <V extends ProtoReply> void registerReplyHandler(short replyId, Predicate<V> execute_condition, short execute_handler, Predicate<V> discard_condition,  short ... trigger_handlers) throws HandlerRegistrationException {
        registerReplyHandler(replyId,execute_condition,execute_handler,discard_condition, (V rep, short source)->{},trigger_handlers);

    }


    public <V extends ProtoReply> void registerReplyHandler(short replyId, Predicate<V> execute_condition, short execute_handler,  short ... trigger_handlers) throws HandlerRegistrationException {
        registerReplyHandler(replyId,execute_condition,execute_handler,(V rep) -> false, (V rep, short source)->{},trigger_handlers);

    }

    public <V extends ProtoReply> void registerReplyHandler(short replyId, Predicate<V> execute_condition, short execute_handler, Predicate<V> discard_condition,ConditionalReplyHandler<V> discard_handler,  short ... trigger_handlers)
            throws HandlerRegistrationException {
        reply_event_queue.put(execute_handler,new LinkedList<>());
        //add the handler id to the dependencies of s
        for(short s : trigger_handlers){
            dependency_map.computeIfAbsent(s,k -> new ArrayList<>()).add(execute_handler);
        }
        ConditionalReplyHandler<V> replyHandler2 = (V reply, short source) -> {
            ((ConditionalReplyHandler)(handler_map.getHandlerById(execute_handler))).uponReply(reply,source);
            List<Short> list = dependency_map.get(execute_handler);
            if(list!=null) {
                for (Short s : list) {
                    retest_predicate(s);
                }
            }
            deliver_next();

        };
        ReplyHandler<V> replyHandler = (V reply, short sourceProto) -> {
            if(discard_condition.test(reply)){
                discard_handler.uponReply(reply,sourceProto);
                return;
            }
            if(!execute_condition.test(reply)){
                reply_event_queue.get(execute_handler).add(new ConditionalReply(execute_condition,replyHandler2,reply,execute_handler,sourceProto,discard_condition,discard_handler));
                deliver_next();
                return;
            }
            replyHandler2.uponReply(reply,sourceProto);
        };
        registerReplyHandler(replyId, replyHandler);
    }

    public void registerCommunicationReplyHandler(
            String msg_type, Predicate<CommunicationReply> execute_condition, short execute_handler, short... trigger_handlers) {
        registerCommunicationReplyHandler(msg_type, execute_condition, execute_handler, (CommunicationReply rep) -> false, (CommunicationReply rep) -> {}, trigger_handlers);
    }

    public void registerCommunicationReplyHandler(
            String msg_type, Predicate<CommunicationReply> execute_condition, short execute_handler, Predicate<CommunicationReply> discard_condition, short... trigger_handlers) {
        registerCommunicationReplyHandler(msg_type, execute_condition, execute_handler, discard_condition, (CommunicationReply rep) -> {}, trigger_handlers);
    }

    public void registerCommunicationReplyHandler(String msg_type, Predicate<CommunicationReply> execute_condition, short execute_handler, Predicate<CommunicationReply> discard_condition, ConditionalCommunicationHandler discard_handler,short ... trigger_handlers){
        communication_event_queue.put(execute_handler,new LinkedList<>());
        for(short s : trigger_handlers){
            dependency_map.computeIfAbsent(s,k -> new ArrayList<>()).add(execute_handler);
        }
        ConditionalCommunicationHandler han = ((ConditionalCommunicationHandler)(handler_map.getHandlerById(execute_handler)));



        ConditionalCommunicationHandler replyHandler2 = (CommunicationReply reply) -> {
            han.uponCommunicationEvent(reply);
            List<Short> list = dependency_map.get(execute_handler);
            if(list!= null) {
                for (Short s : list) {
                    retest_predicate(s);
                }
            }
            deliver_next();

        };
        type_handler_map.put(msg_type,new CommunicationEvent(execute_condition,replyHandler2,msg_type,execute_handler,discard_condition,discard_handler));

    }

    public void registerOneTimeConditionHandler(Predicate execute_condition, short execute_handler_id, short... trigger_handlers) {
        registerOneTimeConditionHandler(execute_condition, execute_handler_id, (Predicate) obj -> false, (ConditionHandler) () -> {}, trigger_handlers);
    }

    public void registerOneTimeConditionHandler(Predicate execute_condition, short execute_handler_id, Predicate discard_condition, short... trigger_handlers) {
        registerOneTimeConditionHandler(execute_condition, execute_handler_id, discard_condition, (ConditionHandler) () -> {}, trigger_handlers);
    }

    public void registerOneTimeConditionHandler(Predicate execute_condition,short execute_handler_id, Predicate discard_condition,ConditionHandler discard_handler, short... trigger_handlers){
        for(short s : trigger_handlers){
            dependency_map.computeIfAbsent(s,k -> new ArrayList<>()).add(execute_handler_id);
        }
        ConditionHandler han = ((ConditionHandler)handler_map.getHandlerById(execute_handler_id));
        ConditionHandler han2 = ()->{
            han.handleCondition();
            List<Short> list = dependency_map.get(execute_handler_id);
            if(list!= null) {
                for (Short s : list) {
                    retest_predicate(s);
                }
            }
            deliver_next();
        };
        if(discard_condition.test(null)){
            discard_handler.handleCondition();
            return;
        }
        if(!execute_condition.test(null)){
            one_time_condition_event_queue.computeIfAbsent(execute_handler_id,k->new LinkedList<>()).add(new ConditionEvent(execute_condition,han,execute_handler_id,discard_condition,discard_handler));
            return;
        }
        han2.handleCondition();

    }
    public void registerEndlessConditionHandler(Predicate execute_condition, short execute_handler_id, short... trigger_handlers) {
        registerEndlessConditionHandler(execute_condition, execute_handler_id, (Predicate) obj -> false, (ConditionHandler) () -> {}, trigger_handlers);
    }

    public void registerEndlessConditionHandler(Predicate execute_condition, short execute_handler_id, Predicate discard_condition, short... trigger_handlers) {
        registerEndlessConditionHandler(execute_condition, execute_handler_id, discard_condition, (ConditionHandler) () -> {}, trigger_handlers);
    }

    public void registerEndlessConditionHandler(Predicate execute_condition,short execute_handler_id, Predicate discard_condition,ConditionHandler discard_handler, short... trigger_handlers){
        for(short s : trigger_handlers){
            dependency_map.computeIfAbsent(s,k -> new ArrayList<>()).add(execute_handler_id);
        }
        ConditionHandler han = ((ConditionHandler)handler_map.getHandlerById(execute_handler_id));
        ConditionHandler han2 = ()->{
            han.handleCondition();
            List<Short> list = dependency_map.get(execute_handler_id);
            if(list!= null) {
                for (Short s : list) {
                    retest_predicate(s);
                }
            }
            deliver_next();
        };

        if(discard_condition.test(null)){
            discard_handler.handleCondition();
            return;
        }
        endless_condition_event_queue.computeIfAbsent(execute_handler_id,k->new LinkedList<>()).add(new ConditionEvent(execute_condition,han2,execute_handler_id,discard_condition,discard_handler));
        if(!execute_condition.test(null)){
            return;
        }
        han2.handleCondition();
    }


    public void setupCommunicationChannel(GenericProtocol communication_proto,Properties props){

        try{
            com_proto_id = communication_proto.getProtoId();
            ReplyHandler<CommunicationReply> replyHandler = (CommunicationReply reply, short sourceProto) -> {
                //System.out.println("received message CP"+reply.getMsg());
                CommunicationEvent comm_event = type_handler_map.get(reply.getPayload().get("type"));
                if(comm_event.discard_condition.test(reply)){
                    comm_event.discard_handler.uponCommunicationEvent(reply);
                    return;
                }
                if(!comm_event.deliver_condition.test(reply)){
                    communication_event_queue.get(comm_event.execute_id).add(new CommunicationEvent(comm_event.deliver_condition,comm_event.execute_handler,reply,comm_event.execute_id,comm_event.discard_condition,comm_event.discard_handler));
                    deliver_next();
                    return;
                }
                comm_event.execute_handler.uponCommunicationEvent(reply);
            };
            registerReplyHandler(CommunicationReply.REPLY_ID, replyHandler);
            Babel.getInstance().registerProtocol(communication_proto);
            communication_proto.init(props);

        } catch (ProtocolAlreadyExistsException e) {

        } catch (IOException e) {
            e.printStackTrace();
        } catch (HandlerRegistrationException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String type, HashMap<String,String> msg, Proc destination, short dest_proto){
        HashMap<String,String> payload = new HashMap<>();
        payload.put("type",type);
        sendRequest(new SendMessageRequest(msg,payload,destination,dest_proto),com_proto_id);
    }


    public void signUp(Properties props) throws HandlerRegistrationException, IOException, ProtocolAlreadyExistsException {
        init(props);
        Babel.getInstance().registerProtocol(this);
    }


    private void retest_predicate(Short s) {
        if (request_event_queue.containsKey(s)) {
            request_to_deliver_queue.put(s, new LinkedList<>());
            Iterator<ConditionalRequest> iterator = request_event_queue.get(s).iterator();
            while (iterator.hasNext()) {
                ConditionalRequest r = iterator.next();
                if (r.discard_condition.test(r.request)) {
                    r.discard_handler.uponRequest(r.request,r.source);
                    iterator.remove();
                } else if (r.deliver_condition.test(r.request)) {
                    request_to_deliver_queue.get(s).add(r);
                }
            }
        }
        else if (reply_event_queue.containsKey(s)) {
            reply_to_deliver_queue.put(s, new LinkedList<>());
            Iterator<ConditionalReply> iterator = reply_event_queue.get(s).iterator();
            while (iterator.hasNext()) {
                ConditionalReply r = iterator.next();
                if (r.discard_condition.test(r.reply)) {
                    r.discard_handler.uponReply(r.reply,r.source);
                    iterator.remove();
                }
                else if (r.deliver_condition.test(r.reply)) {
                    reply_to_deliver_queue.get(s).add(r);
                }
            }
        }
        else if (communication_event_queue.containsKey(s)) {
            communication_to_deliver_queue.put(s, new LinkedList<>());
            Iterator<CommunicationEvent> iterator = communication_event_queue.get(s).iterator();
            while (iterator.hasNext()) {
                CommunicationEvent r = iterator.next();
                if (r.discard_condition.test(r.reply)) {
                    r.discard_handler.uponCommunicationEvent(r.reply);
                    iterator.remove();
                }
                else if (r.deliver_condition.test(r.reply)) {
                    communication_to_deliver_queue.get(s).add(r);
                }
            }
        }
        else if (one_time_condition_event_queue.containsKey(s)) {
            one_time_condition_to_deliver_queue.put(s, new LinkedList<>());
            Iterator<ConditionEvent> iterator = one_time_condition_event_queue.get(s).iterator();
            while (iterator.hasNext()) {
                ConditionEvent r = iterator.next();
                if (r.discard_condition.test(null)) {
                    r.discard_handler.handleCondition();
                    iterator.remove();
                }
                else if (r.execute_condition.test(null)) {
                    one_time_condition_to_deliver_queue.get(s).add(r);
                }
            }
        }
        else if (endless_condition_event_queue.containsKey(s)) {
            endless_condition_to_deliver_queue.put(s, new LinkedList<>());
            Iterator<ConditionEvent> iterator = endless_condition_event_queue.get(s).iterator();
            while (iterator.hasNext()) {
                ConditionEvent r = iterator.next();
                if (r.discard_condition.test(null)){
                    r.discard_handler.handleCondition();
                    iterator.remove();
                }
                else if (r.execute_condition.test(null)) {
                    endless_condition_to_deliver_queue.get(s).add(r);
                }
            }
        }
    }



    private void deliver_next() {
        //find the queue with the smallest timestamp
        Integer min_ts = null;
        Short min_s = null;
        for(Map.Entry<Short,LinkedList<ConditionalRequest>> entry : request_to_deliver_queue.entrySet()){
            ConditionalRequest cur = entry.getValue().peek();
            if(cur!=null&&(min_ts==null||(cur!=null&&min_ts>cur.ts))){
                min_ts = cur.ts;
                min_s = cur.execute_id;
            }
        }
        for(Map.Entry<Short,LinkedList<ConditionalReply>> entry : reply_to_deliver_queue.entrySet()){
            ConditionalReply cur = entry.getValue().peek();
            if(cur!=null&&(min_ts==null||(cur!=null&&min_ts>cur.ts))){
                min_ts = cur.ts;
                min_s = cur.execute_id;
            }
        }
        for(Map.Entry<Short,LinkedList<CommunicationEvent>> entry : communication_to_deliver_queue.entrySet()){
            CommunicationEvent cur = entry.getValue().peek();
            if(cur!=null&&(min_ts==null||(cur!=null&&min_ts>cur.ts))){
                min_ts = cur.ts;
                min_s = cur.execute_id;
            }
        }
        for(Map.Entry<Short,LinkedList<ConditionEvent>> entry : one_time_condition_to_deliver_queue.entrySet()){
            ConditionEvent cur = entry.getValue().peek();
            if(cur!=null&&(min_ts==null||(cur!=null&&min_ts>cur.ts))){
                min_ts = cur.ts;
                min_s = cur.execute_id;
            }
        }
        for(Map.Entry<Short,LinkedList<ConditionEvent>> entry : endless_condition_to_deliver_queue.entrySet()){
            ConditionEvent cur = entry.getValue().peek();
            if(cur!=null&&(min_ts==null||(cur!=null&&min_ts>cur.ts))){
                min_ts = cur.ts;
                min_s = cur.execute_id;
            }
        }
        //deque from that queue and deliver the event
        for(Map.Entry<Short,LinkedList<ConditionalRequest>> entry : request_to_deliver_queue.entrySet()){
            ConditionalRequest cur = entry.getValue().peek();
            if(cur!=null&&min_ts==cur.ts){
                entry.getValue().pop();
                request_event_queue.get(cur.execute_id).remove(cur);
                cur.execute_handler.uponRequest(cur.request,cur.source);
                return;
            }
        }
        for(Map.Entry<Short,LinkedList<ConditionalReply>> entry : reply_to_deliver_queue.entrySet()){
            ConditionalReply cur = entry.getValue().peek();
            if(cur!=null&&min_ts==cur.ts){
                entry.getValue().pop();
                reply_event_queue.get(cur.execute_id).remove(cur);
                cur.execute_handler.uponReply(cur.reply,cur.source);
                return;
            }
        }
        for(Map.Entry<Short,LinkedList<CommunicationEvent>> entry : communication_to_deliver_queue.entrySet()){
            CommunicationEvent cur = entry.getValue().peek();
            if(cur!=null&&min_ts==cur.ts){
                entry.getValue().pop();
                communication_event_queue.get(cur.execute_id).remove(cur);
                cur.execute_handler.uponCommunicationEvent(cur.reply);
                return;
            }
        }
        for(Map.Entry<Short,LinkedList<ConditionEvent>> entry : one_time_condition_to_deliver_queue.entrySet()){
            ConditionEvent cur = entry.getValue().peek();
            if(cur!=null&&min_ts==cur.ts){
                entry.getValue().pop();
                one_time_condition_event_queue.get(cur.execute_id).remove(cur);
                cur.execute_handler.handleCondition();
                return;
            }
        }
        for(Map.Entry<Short,LinkedList<ConditionEvent>> entry : endless_condition_to_deliver_queue.entrySet()){
            ConditionEvent cur = entry.getValue().peek();
            if(cur!=null&&min_ts==cur.ts){
                entry.getValue().pop();
                cur.execute_handler.handleCondition();
                return;
            }
        }
    }



}

