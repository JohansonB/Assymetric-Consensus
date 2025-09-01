package debugging;

import pbft.epochchange.EpochChange;
import pbft.epochchange.StartEpoch;
import pbft.leaderdetection.ByzantineLeaderDetector;
import pbft.leaderdetection.Complain;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class EpochChangeTester extends GenericProtocol {
    public class ComplainTimer extends ProtoTimer {
        public static final short TIMER_ID = 1;

        public ComplainTimer() {
            super(TIMER_ID);
        }

        @Override
        public ProtoTimer clone() {
            return this;
        }
    }
    public static final short PROTO_ID = 204;
    public static final String PROTO_NAME = "leaderdetectordebugging";
    public EpochChangeTester() {
        super(PROTO_NAME,PROTO_ID);
    }

    Proc cur_leader;
    HashSet<Proc> peers = new HashSet<>();

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            Proc p = Proc.parse(code);
            if(p.getId()==0){
                cur_leader = p;
            }
            peers.add(p);
        }
        registerTimerHandler(ComplainTimer.TIMER_ID, this::uponTimer);
        registerReplyHandler(StartEpoch.REPLY_ID, this::uponChange);
        setupTimer(new ComplainTimer(), ThreadLocalRandom.current().nextLong(0, 2000));
    }
    void uponChange(StartEpoch l, short source){
        cur_leader = l.getLeader();
        System.out.println("new leader: "+cur_leader.toString()+" new ts"+l.getTs());
    }
    void uponTimer(ComplainTimer t, long timerId){
        sendRequest(new Complain(cur_leader), ByzantineLeaderDetector.PROTO_ID);
        setupTimer(new ComplainTimer(), ThreadLocalRandom.current().nextLong(0, 2000));
    }

    public static void main(String[] args) throws InvalidParameterException, IOException, HandlerRegistrationException, ProtocolAlreadyExistsException {


        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);


        EpochChange bld = new EpochChange();
        EpochChangeTester tester = new EpochChangeTester();

        //Registers the protocol in babel
        babel.registerProtocol(tester);
        babel.registerProtocol(bld);

        //Initializes the protocol
        bld.init(props);
        tester.init(props);


        //Starts babel
        babel.start();
    }
}
