package trustsystem;

import java.util.Arrays;
import java.util.HashSet;

class EncodingDecodingTest {

    public static void main(String[] args) {
        // Step 1: Create some Proc objects
        Proc p1 = new Proc(1, "127.0.0.1", 5000);
        Proc p2 = new Proc(2, "127.0.0.1", 5001);
        Proc p3 = new Proc(3, "127.0.0.1", 5002);
        Proc p4 = new Proc(4, "127.0.0.1", 5003);

        // Step 2: Create ProcSets
        ProcSet set1 = new ProcSet(new HashSet<>(Arrays.asList(p1, p2)));
        ProcSet set2 = new ProcSet(new HashSet<>(Arrays.asList(p3, p4)));
        ProcSet set3 = new ProcSet(new HashSet<>(Arrays.asList(p2, p3)));

        // Step 3: Create a QuorumSystem
        QuorumSystem originalQuorumSystem = new QuorumSystem(Arrays.asList(set1, set2, set3));

        // Step 4: Serialize the QuorumSystem
        String encoded = originalQuorumSystem.toString();
        System.out.println("Encoded QuorumSystem: " + encoded);

        // Step 5: Deserialize it back
        QuorumSystem decodedQuorumSystem = new QuorumSystem(ProcSystem.parse(encoded).get_p_sets());

        // Step 6: Print and compare
        System.out.println("Decoded QuorumSystem: " + decodedQuorumSystem);

        // Step 7: Check if the original and decoded are equal
        if (originalQuorumSystem.toString().equals(decodedQuorumSystem.toString())) {
            System.out.println(" Encoding and Decoding successful! The data matches.");
        } else {
            System.out.println(" Encoding and Decoding failed! The data does not match.");
        }
    }
}
