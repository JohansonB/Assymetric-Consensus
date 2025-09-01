package randomizedconsensus.commoncoin;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

public class UnsafeCommonCoinProtocol extends GenericProtocol {
    public static String PROTO_NAME = "unsafecommoncoin";
    public static short PROTO_ID = 200;
    private Random random;
    ArrayList<Boolean> history = new ArrayList<>();
    public UnsafeCommonCoinProtocol() {
        super(PROTO_NAME, PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws IOException, HandlerRegistrationException {
       long seed = new Long(properties.getProperty("seed"));
       random = new Random(seed);


        registerRequestHandler(ReleaseCoinRequest.REQUEST_ID,this::uponReleaseCoin);
    }

    private void uponReleaseCoin(ReleaseCoinRequest request, short sourceProto){
        int round = request.getRound();
        while (history.size()<=round) {
            history.add(random.nextBoolean());
        }
        sendReply(new OutputCoinReply(history.get(round),round),sourceProto);
    }

}
