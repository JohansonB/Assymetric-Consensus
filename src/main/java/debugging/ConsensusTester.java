package debugging;

import randomizedconsensus.ACDecide;
import randomizedconsensus.ACProposeRequest;
import randomizedconsensus.BinaryConsensusProtocol;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import trustsystem.FaultSystem;
import trustsystem.Proc;
import trustsystem.QuorumSystem;
import trustsystem.TrustSystem;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class ConsensusTester extends GenericProtocol {
    Proc self;
    public static final String PROTO_NAME = "ConsensusTester";
    public static final short PROTO_ID = 204;

    ConsensusTester(){
        super(PROTO_NAME,PROTO_ID);
    }
    @Override
    public void init(Properties properties) throws IOException, HandlerRegistrationException {
        self = Proc.parse(properties.getProperty("self"));
        registerReplyHandler(ACDecide.REPLY_ID,this::uponDeliver);
        if(self.getPort()%2==0) {
            sendRequest(new ACProposeRequest(true), BinaryConsensusProtocol.PROTO_ID);
        }
        else{
            sendRequest(new ACProposeRequest(false), BinaryConsensusProtocol.PROTO_ID);
        }
    }
    private void uponDeliver(ACDecide reply, short sourceProtocol){
        System.out.println(reply.getOutput());
    }




    public static void main(String[] args) throws InvalidParameterException, IOException, HandlerRegistrationException, ProtocolAlreadyExistsException {
        Proc p1 = new Proc(0, "127.0.0.1", 5000);
        Proc p2 = new Proc(1, "127.0.0.1", 5001);
        Proc p3 = new Proc(2, "127.0.0.1", 5002);
        Proc p4 = new Proc(3, "127.0.0.1", 5003);
        Proc p5 = new Proc(4, "127.0.0.1", 5004);

        ArrayList<Proc> peers = new ArrayList<>();
        peers.add(p1);
        peers.add(p2);
        peers.add(p3);
        peers.add(p4);
        //peers.add(p5);

        FaultSystem f_s = new FaultSystem(new ArrayList<>());
        QuorumSystem q_s = QuorumSystem.majorityQuorums(peers);
        TrustSystem t_s = new TrustSystem(f_s,q_s);




        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);


        ConsensusTester tester = new ConsensusTester();
        BinaryConsensusProtocol con = new BinaryConsensusProtocol();

        //Registers the protocol in babel
        babel.registerProtocol(tester);
        babel.registerProtocol(con);

        //Initializes the protocol
        con.init(props);
        tester.init(props);


        //Starts babel
        babel.start();
    }
}
