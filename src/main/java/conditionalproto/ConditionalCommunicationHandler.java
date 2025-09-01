package conditionalproto;

import communication.reply.CommunicationReply;

@FunctionalInterface
public interface ConditionalCommunicationHandler {
    void uponCommunicationEvent(CommunicationReply reply);
}
