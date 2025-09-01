package conditionalproto;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

@FunctionalInterface
public interface ConditionalReplyHandler<V extends ProtoReply> {
    void uponReply(V reply, short sourceProto);
}
