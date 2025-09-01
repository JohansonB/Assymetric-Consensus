package conditionalproto;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

@FunctionalInterface
public interface ConditionalRequestHandler<V extends ProtoRequest>  {
    void uponRequest(V request, short sourceProto);
}
