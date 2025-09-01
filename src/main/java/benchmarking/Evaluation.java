package benchmarking;


import trustsystem.FiveATS;
import trustsystem.ToleratedSystem;

import java.io.*;
import java.util.*;

public class Evaluation {

        public static double computeEmpiricalAverage(String outputDir, String atsName, String algorithmName, int fileNumber) {
            File baseDir = new File(outputDir, atsName + "/" + algorithmName);
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                throw new IllegalArgumentException("Directory does not exist: " + baseDir.getAbsolutePath());
            }

            String targetFileName = fileNumber + ".txt";
            double sum = 0.0;
            int count = 0;

            File[] repetitionDirs = baseDir.listFiles(File::isDirectory);
            if (repetitionDirs == null) return Double.NaN;  // no repetitions found

            for (File repetitionDir : repetitionDirs) {
                File dataFile = new File(repetitionDir, targetFileName);
                if (!dataFile.exists()) {
                    System.err.println("Warning: Missing file " + dataFile.getAbsolutePath());
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
                    String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        sum += Double.parseDouble(line.trim());
                        count++;
                    }
                } catch (IOException | NumberFormatException e) {
                    System.err.println("Error reading file: " + dataFile.getAbsolutePath());
                    e.printStackTrace();
                }
            }

            return count == 0 ? Double.NaN : sum / count;
        }
        public static void main(String[] args){
            //System.out.println(computeEmpiricalAverage("Output","7pATS","RCReplica",1));
            ToleratedSystem t_s = new FiveATS().get_tolerated_system();
            System.out.println(t_s);
        }
}
