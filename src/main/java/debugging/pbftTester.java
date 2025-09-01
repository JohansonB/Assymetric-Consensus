package debugging;

import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import pbft.EpochState;
import pbft.PBFT;
import pbft.PBFTDecide;
import pbft.PBFTPropose;
import pbft.conditionalcollect.CCSetup;
import pbft.conditionalcollect.CollectedOutput;
import pbft.conditionalcollect.ConditionalCollect;
import pbft.conditionalcollect.InputRequest;
import pbft.leaderdetection.ByzantineLeaderDetector;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public class pbftTester extends ConditionalGenericProtocol {
    public static final String PROTO_NAME = "pbftTester";
    public static final short PROTO_ID = 204;

    Proc self;
    HashSet<Proc> peers = new HashSet<>();
    Proc l;

    public pbftTester(){
        super(PROTO_NAME,PROTO_ID);
    }
    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        ConditionalReplyHandler<PBFTDecide> decide_handler = (d, s) -> System.out.println(d.getVal());
        short decideHandlerId = handler_map.register(decide_handler);

        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            if(p.getId()==2)
                l = p;
            peers.add(p);

        }

        registerReplyHandler(
                PBFTDecide.REPLY_ID,
                (o)-> true,
                decideHandlerId
        );
        if(self.getId()%2==0) {
            sendRequest(new PBFTPropose("pepe"+self.getId()), PBFT.PROTO_ID);
        }
        else{
            sendRequest(new PBFTPropose("popo"+self.getId()), PBFT.PROTO_ID);

        }
    }

    public static void main(String[] args) throws InvalidParameterException, IOException, HandlerRegistrationException, ProtocolAlreadyExistsException {


        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);


        PBFT pbft = new PBFT();
        pbftTester tester = new pbftTester();

        //Registers the protocol in babel
        babel.registerProtocol(tester);
        babel.registerProtocol(pbft);

        //Initializes the protocol
        pbft.init(props);
        tester.init(props);


        //Starts babel
        babel.start();
    }
}
