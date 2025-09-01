package debugging;

import randomizedconsensus.abvbroadcast.*;
import communication.CommunicationProtocol;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;
import trustsystem.FaultSystem;
import trustsystem.Proc;
import trustsystem.QuorumSystem;
import trustsystem.TrustSystem;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class abvBroadcastTester extends GenericProtocol {
    Proc self;
    static class zeTimer extends ProtoTimer{
        public static final short TIMER_ID = 2;
        zeTimer(){
            super(TIMER_ID);
        }

        @Override
        public ProtoTimer clone() {
            return this;
        }
    }
    public static final String PROTO_NAME = "abvBroadcastTester";
    public static final short PROTO_ID = 203;

    abvBroadcastTester(){
        super(PROTO_NAME,PROTO_ID);
    }
    @Override
    public void init(Properties properties) throws IOException, HandlerRegistrationException {
        //self = Proc.parse(properties.getProperty("self"));
        registerReplyHandler(MultiplexAbvDeliver.REPSONSE_ID,this::uponDeliver);
        registerTimerHandler(zeTimer.TIMER_ID,this::uponTimer);
        setupTimer(new zeTimer(),3000);
        for(int i = 0; i<1;i++) {
            sendRequest(new MultiplexAbvBroadcastRequest(new HashMap<>(), false,i), MultiplexAbvBroadcast.PROTO_ID);
            //sendRequest(new MultiplexAbvBroadcastRequest(new HashMap<>(), true,i), MultiplexAbvBroadcast.PROTO_ID);
        }
    }
    private void uponDeliver(MultiplexAbvDeliver reply, short sourceProtocol){
        System.out.println(reply.getOutput()+" "+reply.getRound());
    }

    private void uponTimer(zeTimer timer, long timerId){
        //sendRequest(new abvBroadcastRequest(new HashMap<>(),true), abvBroadcast.PROTO_ID);
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

        //System.out.println(peers);
        //System.out.println(q_s);




        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);


        abvBroadcastTester tester = new abvBroadcastTester();
        MultiplexAbvBroadcast abvB = new MultiplexAbvBroadcast();
        CommunicationProtocol comm = new CommunicationProtocol();

        //Registers the protocol in babel
        babel.registerProtocol(tester);
        babel.registerProtocol(abvB);
        babel.registerProtocol(comm);

        //Initializes the protocol
        comm.init(props);
        abvB.init(props);
        tester.init(props);


        //Starts babel
        babel.start();
    }
}
