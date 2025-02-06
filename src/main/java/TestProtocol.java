import org.apache.logging.log4j.core.config.Configurator;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

public class TestProtocol  extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(TestProtocol.class);


    private static final String PROTO_NAME = "test";
    public static final short PROTO_ID = 102;
    String my_port;
    private Host self;
    protected int channelId;

    TestProtocol(){
        super(PROTO_NAME,PROTO_ID);
    }
    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        Configurator.setLevel("TestProtocol", org.apache.logging.log4j.Level.DEBUG);
        my_port = properties.getProperty("port");
        int my_port_int = new Integer(my_port);
        Host target;
        Properties channelProps = new Properties();
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, "127.0.0.1");
        channelProps.setProperty(TCPChannel.PORT_KEY, my_port);
        self = new Host(InetAddress.getByName(channelProps.getProperty(TCPChannel.ADDRESS_KEY)),
                Short.parseShort(channelProps.getProperty(TCPChannel.PORT_KEY)));
        this.channelId = createChannel(TCPChannel.NAME, channelProps);

        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

        for(int cur_port = 5000;cur_port<5003;cur_port++){
            if(cur_port == my_port_int)
                continue;
            target = new Host(InetAddress.getByName("127.0.0.1"),cur_port);
            openConnection(target);

        }

    }

    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        Host peer = event.getNode();
        logger.debug("Out Connection to {} is up", peer);
    }

    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        Host peer = event.getNode();
        logger.debug("Connection to {} is down cause {}", peer, event.getCause());
    }

    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
        logger.debug("Connection to {} failed cause: {}", event.getNode(), event.getCause());
        openConnection(event.getNode());
    }

    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.trace("Connection from {} is up", event.getNode());
    }

    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        logger.trace("Connection from {} is down, cause: {}", event.getNode(), event.getCause());
    }


    public static void main(String[] args) throws IOException, HandlerRegistrationException, InvalidParameterException, ProtocolAlreadyExistsException {
        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

        //Creates a new instance of the FullMembership Protocol
        TestProtocol fullMembership = new TestProtocol();

        //Registers the protocol in babel
        babel.registerProtocol(fullMembership);

        //Initializes the protocol
        fullMembership.init(props);

        //Starts babel
        babel.start();


    }
}
