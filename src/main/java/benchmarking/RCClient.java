package benchmarking;

import communication.AuthenticatedChannel;
import communication.BenchmarkingChannel;
import communication.reply.CommunicationReply;
import communication.reply.IdUpdatedReply;
import communication.reply.ReInitKeyAck;
import communication.request.ReInitKeyRequest;
import communication.request.SetRunId;
import conditionalproto.ConditionHandler;
import conditionalproto.ConditionalCommunicationHandler;
import conditionalproto.ConditionalGenericProtocol;
import conditionalproto.ConditionalReplyHandler;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import trustsystem.*;
import utils.SerializerTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RCClient extends ConditionalGenericProtocol {
    private static final File OUTPUT_DIR = new File("output");
    private static final short REPLICAID = RCReplica.PROTO_ID;
    private static final String REPLICANAME = RCReplica.PROTO_NAME;

    private static final String PROTO_NAME = "RCClient";
    public static final short PROTO_ID = 400;
    public static final double DELTA = 200;
    public static final int NUM_REPETITIONS = 20;
    public static final BenchmarkingChannel.Distribution DISTRIBUTION = new BenchmarkingChannel.Normal(0,0);
    public static final AsymmetricTrustSystem ATS = new SevenATS();



    ArrayList<Proc> peers = new ArrayList<>();
    HashSet<Proc> received = new HashSet<>();
    HashSet<Proc> is_setup = new HashSet<>();
    HashMap<Proc, MarkedProcSystem> q_markers = new HashMap<>();
    //first assume that the faults are computed using the tolerated set
    ArrayList<ProcSet> faults = new ArrayList();
    Proc self;

    int cur_run = 0;
    int cur_fault = 0;
    int repetition = 0;

    long start_time;
    long end_time;
    boolean channel_updated = true;

    AsymmetricTrustSystem ats;

    static String[] client_args;

    static OneShotConsensus osc = new OneShotConsensus();


    public RCClient() {
        super(PROTO_NAME, PROTO_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {

        AuthenticatedChannel communication_channel = new AuthenticatedChannel();
        setupCommunicationChannel(communication_channel,properties);

        ArrayList<String> peer_codes = SerializerTools.decode_collection(properties.getProperty("peers"));
        for(String code : peer_codes){
            peers.add(Proc.parse(code));
        }
        Proc self = Proc.parse(properties.getProperty("self"));
        ats = AsymmetricTrustSystem.parse(properties.getProperty("ats"));
        for(Proc p  : peers){
            q_markers.put(p,ats.getTrustSystem(p).get_quorums().get_marked());
        }
        ArrayList<Proc> peers_clone = new ArrayList<>(peers);
        peers_clone.add(self);
        properties.setProperty("peers",SerializerTools.encode_collection(peers_clone));

        ConditionHandler uponIsReadyHandler = this::uponReady;
        short uponIsReadyHandlerId = handler_map.register(uponIsReadyHandler);


        ConditionalCommunicationHandler decideHandler = this::uponDecideMsg;
        short decideHandlerId = handler_map.register(decideHandler);

        ConditionalCommunicationHandler isSetupHandler = this::uponIsSetupMsg;
        short isSetupHandlerId = handler_map.register(isSetupHandler);

        ConditionalReplyHandler idUpdateHandler = this::uponIdUpdate;
        short idUpdateHandlerId = handler_map.register(idUpdateHandler);

        ConditionalReplyHandler reInitKeyReplyHandler = this::uponReInitKeyAck;
        short reInitKeyReplyHandlerId = handler_map.register(reInitKeyReplyHandler);

        registerReplyHandler(
                IdUpdatedReply.REPLY_ID,
                (p)->true,
                idUpdateHandlerId
        );

        registerReplyHandler(
                ReInitKeyAck.ACK_ID,
                (p)->true,
                reInitKeyReplyHandlerId
        );


        registerCommunicationReplyHandler(
                "decided",
                (v -> true),
                decideHandlerId
        );

        registerCommunicationReplyHandler(
                "isSetup",
                (v -> true),
                isSetupHandlerId
        );

        registerEndlessConditionHandler(
                (v -> is_setup.size()==peers.size()&&channel_updated),
                uponIsReadyHandlerId,
                isSetupHandlerId, reInitKeyReplyHandlerId
        );

    }


    private void uponStartup(){
        HashMap<String,String> msg = new HashMap<>();
        msg.put("v",Boolean.toString(false));
        for(Proc q : peers){
            if(!faults.get(cur_fault).get_p_set().contains(q))
                sendMessage("start",msg,q, REPLICAID);
        }
        start_time = System.currentTimeMillis();
    }

    private void uponIsSetupMsg(CommunicationReply reply){
        is_setup.add(reply.getOrigin());
    }

    private void uponReady(){
        uponStartup();
    }

    private void uponDecideMsg(CommunicationReply reply){
        if(received.contains(reply.getOrigin())||Integer.parseInt(reply.getMsg().get("run_id"))!=cur_run){
            return;
        }
        received.add(reply.getOrigin());
        for(Proc p : peers){
            if(q_markers.get(p).mark_proc(reply.getOrigin())){
                end_time   = System.currentTimeMillis();
                double elapsedSeconds = (end_time - start_time) / 1000.0;

                // build the nested output path: OUTPUT_DIR/PROTO_NAME/repetition/
                File atsDir = new File(OUTPUT_DIR, ATS.name());
                File replicaDir = new File(atsDir, REPLICANAME);
                File repetitionDir = new File(replicaDir, Integer.toString(repetition));
                if (!repetitionDir.exists() && !repetitionDir.mkdirs()) {
                    System.err.println("ERROR: Could not create output directories: "
                            + repetitionDir.getAbsolutePath());
                }

                // write into <cur_fault>.txt
                File outFile = new File(repetitionDir, cur_fault + ".txt");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
                    writer.write(Double.toString(elapsedSeconds));
                    writer.newLine();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                osc.shutdownChildren();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(cur_run<=2*NUM_REPETITIONS){
                    cur_fault = (cur_fault+1)%2;
                    if(cur_fault==0) repetition++;
                    cur_run++;
                    //reset state
                    for(Proc q  : peers){
                        q_markers.put(q,ats.getTrustSystem(q).get_quorums().get_marked());
                    }
                    is_setup = new HashSet<>();
                    channel_updated = false;
                    received = new HashSet<>();

                    sendRequest(new SetRunId(cur_run,PROTO_ID),AuthenticatedChannel.PROTO_ID);
                }
                else{
                    System.exit(0);
                }
            }
        }
    }

    private void uponIdUpdate(ProtoReply protoReply, short i) {
        osc.APBFT_One_shot_consensus_toy_ats(DISTRIBUTION,cur_run,DELTA);
        sendRequest(new ReInitKeyRequest(),AuthenticatedChannel.PROTO_ID);
    }

    private void uponReInitKeyAck(ProtoReply req, short source){
        channel_updated = true;
    }


     public static class OneShotConsensus {

        double startup_time;
        Proc client;
        int run_id;
        List<Process> allProcs = new ArrayList<>();
        ToleratedSystem ts = null;
        Set<Proc> members;

        public static final int CLIENT_PORT = 3500;
        public static final int CLIENT_ID = -1;

        public void APBFT_One_shot_consensus_toy_ats(BenchmarkingChannel.Distribution distribution, int run_id, double delta ) {
            allProcs = new ArrayList<>();
            client = new Proc(CLIENT_ID,"127.0.0.1",CLIENT_PORT);
            this.run_id = run_id;
            AsymmetricTrustSystem ats = ATS;
            members = ats.getMembers();
            StringBuilder input_params = new StringBuilder();
            String startup_param = compute_startup_params(ats);
            input_params.append(startup_param);
            input_params.append(" ats="+ats.toString());
            input_params.append(" run_id="+run_id);
            input_params.append(" peers="+ SerializerTools.encode_collection(members));
            StringBuilder copy = new StringBuilder(input_params.toString());
            copy.append(" self="+ client.toString());

            // 2) Create the "params" directory if it doesn't exist
            File paramsDir = new File("params");
            if (!paramsDir.exists()) {
                if (!paramsDir.mkdirs()) {
                    System.err.println("ERROR: Could not create params directory.");
                }
            }

            // 3) Write the client's parameters to a file:
            //    params/params_run_<run_id>_client.txt
            String clientParamString = copy.toString().trim();
            File clientParamFile = new File(paramsDir, "params_run_" + run_id + "_client.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(clientParamFile))) {
                writer.write(clientParamString);
                writer.newLine();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            client_args = copy.toString().trim().split("\\s+");

            String[] args;
            ProcessBuilder pb;

            input_params.append(" client="+client.toString());
            input_params.append(" delta="+delta);
            input_params.append(" seed="+0);
            input_params.append(" "+distribution.get_params());
            for(Proc p : members){
                copy = new StringBuilder(input_params.toString());
                copy.append(" self="+p.toString());
                copy.append(" trustsystem="+ats.getTrustSystem(p).toString());
                args = copy.toString().trim().split("\\s+");

                // Ensure logs directory exists
                File logsDir = new File("logs");
                if (!logsDir.exists() && !logsDir.mkdirs()) {
                    System.err.println("ERROR: Could not create logs directory.");
                }

                String replicaParamString = copy.toString().trim();
                // Write replica params to:
                // params/params_run_<run_id>_replica_<procId>.txt
                // Here, I'm assuming Proc.toString() includes some identifier;
                // if Proc has a getId() method use that instead.
                String procIdSafe = p.toString().replaceAll("[^a-zA-Z0-9_\\-]", "_");
                File replicaParamFile = new File(
                        paramsDir,
                        "params_run_" + run_id + "_replica_" + procIdSafe + ".txt"
                );
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(replicaParamFile))) {
                    writer.write(replicaParamString);
                    writer.newLine();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                pb = new ProcessBuilder();
                pb.command("java", "-jar", "Jars/RCReplica.jar");
                pb.command().addAll(Arrays.asList(args));

                // Redirect output & error to files:
                File outFile = new File(logsDir, "replica_" + procIdSafe + ".out");
                File errFile = new File(logsDir, "replica_" + procIdSafe + ".err");
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
                pb.redirectError(ProcessBuilder.Redirect.appendTo(errFile));


                try {
                    Process process = pb.start();
                    allProcs.add(process);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                shutdownChildren();
            }));

        }
        public ArrayList<ProcSet> computeFaults(){
            ArrayList<ProcSet> ret = new ArrayList<>();
            int min = Integer.MAX_VALUE;
            ProcSet min_Q = new ProcSet(members);
            for(ProcSet Q : ts.get_quorums()){
                if(Q.size() < min){
                    min = Q.size();
                    min_Q = Q;
                }
            }
            HashSet<Proc> min_F = new HashSet<>(members);
            min_F.removeAll(min_Q.get_p_set());
            ret.add(new ProcSet(new HashSet<>()));
            ret.add(new ProcSet(min_F));
            return ret;
        }
        private String compute_startup_params(AsymmetricTrustSystem ats) {
            long start_time = System.currentTimeMillis();
            StringBuilder startup_params = new StringBuilder();
            startup_params.append("ts=");
            if(ts == null)
                ts = ats.get_tolerated_system();
            startup_params.append(ts.toString());
            long end_time = System.currentTimeMillis();
            startup_time = end_time - start_time;
            System.out.println(startup_time);
            return startup_params.toString();

        }

        public void shutdownChildren() {
            for (Process p : allProcs) {
                // politely ask it to exit
                p.destroy();
            }
            // wait a short time, then force‐kill any that are still alive
            for (Process p : allProcs) {
                try {
                    if (!p.waitFor(500, TimeUnit.MILLISECONDS)) {
                        p.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }




    public static void main(String[] args) throws IOException, InvalidParameterException, ProtocolAlreadyExistsException, HandlerRegistrationException {
        int run_id = 0;
        osc.APBFT_One_shot_consensus_toy_ats(DISTRIBUTION, run_id, DELTA);

        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        args = SerializerTools.fix_commandline(client_args);
        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

        RCClient c = new RCClient();
        c.faults = osc.computeFaults();

        babel.registerProtocol(c);

        c.init(props);

        babel.start();
    }
}
