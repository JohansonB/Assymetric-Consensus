package debugging;

import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import pbft.EpochState;
import pbft.conditionalcollect.CollectedOutput;
import pbft.conditionalcollect.ConditionalCollect;
import pbft.epochconsensus.ECDecide;
import pbft.epochconsensus.ECPropose;
import pbft.epochconsensus.ECSetup;
import pbft.epochconsensus.EpochConsensus;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

public class EpochConsensusTester extends ConditionalGenericProtocol {
    public static String PROTO_NAME = "ec_tester";
    public static short PROTO_ID = 204;
    public EpochConsensusTester(){
        super(PROTO_NAME,PROTO_ID);
    }
    Proc self;
    Proc l;
    HashSet<Proc> peers = new HashSet<>();
    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        ConditionalReplyHandler<ECDecide> out_handler = (o, s) -> System.out.println(o.getVal());
        short outHandlerId = handler_map.register(out_handler);

        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            if(p.getId()==0)
                l = p;
            peers.add(p);

        }

        registerReplyHandler(
                ECDecide.REPLY_ID,
                (o)-> true,
                outHandlerId
        );
        try {
            EpochConsensus ec = new EpochConsensus();
            Babel.getInstance().registerProtocol(ec);
            ec.init(properties);

        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }
        sendRequest(new ECSetup(EpochState.init_state(),0,l),EpochConsensus.PROTO_ID);
        if(self.equals(l)){
            sendRequest(new ECPropose(Integer.toString(self.getId())),EpochConsensus.PROTO_ID);
        }
    }
    public static void main(String[] args) throws InvalidParameterException, IOException, ProtocolAlreadyExistsException, HandlerRegistrationException {
        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);


        EpochConsensusTester tester = new EpochConsensusTester();

        //Registers the protocol in babel
        babel.registerProtocol(tester);
        tester.init(props);


        //Starts babel
        babel.start();
    }
}
