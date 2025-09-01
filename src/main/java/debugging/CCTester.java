package debugging;

import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import pbft.EpochState;
import pbft.conditionalcollect.CCSetup;
import pbft.conditionalcollect.CollectedOutput;
import pbft.conditionalcollect.ConditionalCollect;
import pbft.conditionalcollect.InputRequest;
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

public class CCTester extends ConditionalGenericProtocol {
    public static final String PROTO_NAME = "CCTester";
    public static final short PROTO_ID = 204;

    Proc self;
    HashSet<Proc> peers = new HashSet<>();
    Proc l;

    public CCTester(){
        super(PROTO_NAME,PROTO_ID);
    }
    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        ConditionalReplyHandler<CollectedOutput> out_handler = (o, s) -> System.out.println(o.getM());
        short outHandlerId = handler_map.register(out_handler);

        self = Proc.parse(properties.getProperty("self"));
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            if(p.getId()==2)
                l = p;
            peers.add(p);

        }

        registerReplyHandler(
                CollectedOutput.PROTO_REPLY,
                (o)-> true,
                outHandlerId
        );

        sendRequest(new CCSetup(l,2,null), ConditionalCollect.PROTO_ID);
        HashMap<Integer,String> map = new HashMap<>();
        map.put(0,"pudel");
        map.put(1,"nudel");
        if(self.getId()%2==0) {
            sendRequest(new InputRequest(new EpochState(0, "pudel", map)), ConditionalCollect.PROTO_ID);
        }
        else{
            sendRequest(new InputRequest(new EpochState(1, "nudel", map)), ConditionalCollect.PROTO_ID);

        }
    }

    public static void main(String[] args) throws InvalidParameterException, IOException, ProtocolAlreadyExistsException, HandlerRegistrationException {
        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);


        CCTester tester = new CCTester();
        ConditionalCollect col = new ConditionalCollect();

        //Registers the protocol in babel
        babel.registerProtocol(tester);
        babel.registerProtocol(col);

        //Initializes the protocol
        col.init(props);
        tester.init(props);


        //Starts babel
        babel.start();
    }

}
