package benchmarking;

import communication.BenchmarkingChannel;
import trustsystem.ATSGenerator;
import trustsystem.AsymmetricTrustSystem;
import trustsystem.SevenATS;
import trustsystem.Proc;
import utils.SerializerTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Starter {

    public static class One_shot_consensus {
        double startup_time;
        AsymmetricTrustSystem ats;
        Proc client;
        int run_id;
        public static final int CLIENT_PORT = 3500;
        public static final int CLIENT_ID = -1;

        public void APBFT_One_shot_consensus(ArrayList<Integer> dims, ArrayList<Integer> t_ds, BenchmarkingChannel.Distribution distribution, int run_id, double delta ) {
            client = new Proc(CLIENT_ID,"127.0.0.1",CLIENT_PORT);
            this.run_id = run_id;
            AsymmetricTrustSystem ats = ATSGenerator.hyperplane_system(dims,t_ds);
            Set<Proc> members = ats.getMembers();
            StringBuilder input_params = new StringBuilder();
            String startup_param = compute_startup_params(ats);
            input_params.append(startup_param);
            input_params.append(" ats="+ats.toString());
            input_params.append(" run_id="+run_id);



            ProcessBuilder pb = new ProcessBuilder();
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.command("java", "-jar", "Jars/Client.jar");
            String[] args = input_params.toString().trim().split("\\s+");
            pb.command().addAll(Arrays.asList(args));

            try {
                Process process = pb.start();
            } catch (Exception e) {
                e.printStackTrace();
            }

            input_params.append(" client="+client.toString());
            input_params.append(" delta="+delta);
            input_params.append(" peers="+ SerializerTools.encode_collection(members));
            input_params.append(" "+distribution.get_params());
            for(Proc p : members){
                StringBuilder copy = new StringBuilder(input_params.toString());
                copy.append(" self="+p.toString());
                copy.append(" trustsystem="+ats.getTrustSystem(p).toString());
                 args = copy.toString().trim().split("\\s+");

                pb = new ProcessBuilder();
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                pb.command("java", "-jar", "Jars/RCReplica.jar");
                pb.command().addAll(Arrays.asList(args));
                try {
                    Process process = pb.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }



        }
        public void APBFT_One_shot_consensus_toy_ats(BenchmarkingChannel.Distribution distribution, int run_id, double delta ) {
            client = new Proc(CLIENT_ID,"127.0.0.1",CLIENT_PORT);
            this.run_id = run_id;
            AsymmetricTrustSystem ats = new SevenATS();
            Set<Proc> members = ats.getMembers();
            StringBuilder input_params = new StringBuilder();
            String startup_param = compute_startup_params(ats);
            input_params.append(startup_param);
            input_params.append(" ats="+ats.toString());
            input_params.append(" run_id="+run_id);
            input_params.append(" peers="+ SerializerTools.encode_collection(members));
            StringBuilder copy = new StringBuilder(input_params.toString());
            copy.append(" self="+ client.toString());


            List<Process> allProcs = new ArrayList<>();

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

            ProcessBuilder pb = new ProcessBuilder();
            pb.command("java", "-jar", "Jars/Client.jar");
            String[] args = copy.toString().trim().split("\\s+");
            pb.command().addAll(Arrays.asList(args));

            try {
                Process process = pb.start();
                allProcs.add(process);
            } catch (Exception e) {
                e.printStackTrace();
            }

            input_params.append(" client="+client.toString());
            input_params.append(" delta="+delta);
            input_params.append(" seed="+0);
            input_params.append(" "+distribution.get_params());
            for(Proc p : members){
                copy = new StringBuilder(input_params.toString());
                copy.append(" self="+p.toString());
                copy.append(" trustsystem="+ats.getTrustSystem(p).toString());
                args = copy.toString().trim().split("\\s+");

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
                try {
                    Process process = pb.start();
                    allProcs.add(process);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            int count = 0;
            for (Process proc : allProcs) {
                try {
                    int exitCode = proc.waitFor();    // blocks until that child JVM exits
                    System.out.println("Child "+count++ +" exited with code " + exitCode);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted while waiting for child.");
                }
            }

        }
        private String compute_startup_params(AsymmetricTrustSystem ats) {
            long start_time = System.currentTimeMillis();
            StringBuilder startup_params = new StringBuilder();
            startup_params.append("ts=");
            startup_params.append(ats.get_tolerated_system().toString());
            long end_time = System.currentTimeMillis();
            startup_time = end_time - start_time;
            System.out.println(startup_time);
            return startup_params.toString();

        }
    }




    public static void main(String[] args){
        ArrayList<Integer> dims = new ArrayList<>();
        dims.add(7);
        dims.add(5);
        ArrayList<Integer> t_ds = new ArrayList<>();
        for(int i = 0; i<7*5-1;i++){
            t_ds.add(1);
        }
        t_ds.add(0);
        BenchmarkingChannel.Distribution distribution = new BenchmarkingChannel.Normal(0,0);
        int run_id = 0;
        double delta = 200;
        One_shot_consensus o_s_c = new One_shot_consensus();
        o_s_c.APBFT_One_shot_consensus_toy_ats(distribution, run_id, delta);
    }
}
