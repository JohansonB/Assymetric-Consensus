package communication;

import communication.DeliverTimer;
import communication.reply.CommunicationReply;
import communication.request.SendMessageRequest;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;
import pt.unl.fct.di.novasys.network.data.Host;
import trustsystem.Proc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;


public class BenchmarkingChannel extends GenericProtocol {

    public static abstract class Distribution{
        public abstract double sample();
        public abstract String get_params();
    }
    public static class Normal extends Distribution{
        double var;
        double mean;
        Random random = new Random();
        public Normal(double mean, double var){
            this.mean = mean;
            this.var = var;
        }
        public double sample(){
            return mean + Math.sqrt(var) * random.nextGaussian();
        }

        @Override
        public String get_params() {
            return "distribution=Normal mean=" + mean + " var=" + var;
        }
    }
    public static class Uniform extends Distribution{
        double min;
        double max;
        Random random = new Random();
        public Uniform(double min, double max){
            this.min = min;
            this.max = max;
        }
        @Override
        public double sample() {
            double uniformSample = min + (max - min) * random.nextDouble();
            return min + (max - min) * random.nextDouble();
        }

        @Override
        public String get_params() {
            return "distribution=Uniform min=" + min + " max=" + max;
        }
    }
    public static final String PROTO_NAME = "benchmarking_channel";
    public static final short PROTO_ID = 120;
    GenericProtocol sub_channel;
    Distribution distribution;
    Proc self;

    int send_count = 0;
    int received_count = 0;
    int run_id;
    public BenchmarkingChannel(GenericProtocol sub_channel, Distribution distribution) {
        super(PROTO_NAME, PROTO_ID);
        this.sub_channel = sub_channel;
        this.distribution = distribution;

    }
    public BenchmarkingChannel(GenericProtocol sub_channel) {
        super(PROTO_NAME, PROTO_ID);
        this.sub_channel = sub_channel;

    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        self = Proc.parse(properties.getProperty("self"));
        run_id = Integer.parseInt(properties.getProperty("run_id"));
        String dist = properties.getProperty("distribution");
        if(dist.equals("Uniform")){
            int min = Integer.parseInt(properties.getProperty("min"));
            int max = Integer.parseInt(properties.getProperty("max"));
            distribution = new Uniform(min, max);
        }
        else if(dist.equals("Normal")){
            double mean = Double.parseDouble(properties.getProperty("mean"));
            double var = Double.parseDouble(properties.getProperty("var"));
            distribution = new Normal(mean, var);
        }

        registerRequestHandler(SendMessageRequest.REQUEST_ID,this::uponMessageRequest);
        registerReplyHandler(CommunicationReply.REPLY_ID,this::uponMessageReply);
        registerTimerHandler(DeliverTimer.TIMER_ID, this::upondeliverTimer);

        Host h;

        try {
            properties.setProperty("dest_proto",Short.toString(PROTO_ID));
            Babel.getInstance().registerProtocol(sub_channel);
            sub_channel.init(properties);
        } catch (ProtocolAlreadyExistsException e) {
            e.printStackTrace();
        }

    }



    private void upondeliverTimer(DeliverTimer t, long l) {
        sendReply(t.reply,t.reply.getDestination_proto());
    }

    private void uponMessageReply(CommunicationReply reply, short i) {
        received_count++;
        if(run_id!=Integer.parseInt(reply.getMsg().get("run_id"))){
            return;
        }
        long delay = (long)distribution.sample();
        setupTimer(new DeliverTimer(reply),delay);
    }

    private void uponMessageRequest(SendMessageRequest messageRequest, short i) {
        send_count++;
        messageRequest.getMessage().put("run_id",Integer.toString(run_id));
        sendRequest(messageRequest,sub_channel.getProtoId());
    }
}
