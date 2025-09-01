package pbft;

import utils.ListEncoderDecoder;
import utils.SerializerTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpochState {
    private static final Triple INVALID_TRIPLE = new Triple(-1, "INVALID", new HashMap<>());
    private Triple triple;

    public EpochState(int ts, String val, HashMap<Integer,String> ws){
        triple = new Triple(ts,val,ws);
    }
    public EpochState(String code){
        triple = EpochState.decodeTriple(code);
    }

    //empty string is used to encode init state
    public static EpochState init_state() {
        return new EpochState(0,"",new HashMap<>());
    }

    public int getTs(){
        return triple.ts;
    }
    public String getVal(){
        return triple.val;
    }
    public HashMap<Integer,String> getWs(){
        return triple.ws;
    }

    public void setVal(String v){
        triple.val = v;
    }
    public void setTs(int ts){
        triple.ts = ts;
    }

    public boolean isInvalid(){
        return triple == INVALID_TRIPLE;
    }

    public static class Triple {
        int ts;
        String val;
        HashMap<Integer, String> ws;

        public Triple(int ts, String val, HashMap<Integer, String> ws) {
            this.ts = ts;
            this.val = val;
            this.ws = ws;
        }

    }
    public String toString(){
        return encodeTriple(triple.ts,triple.val,triple.ws);
    }
    // Encoder for a triple (ts, val, ws) using ListEncoder
    public static String encodeTriple(int ts, String val, HashMap<Integer, String> ws) {
        // Create a list with the ts, val, and ws
        ArrayList<String> tripleList = new ArrayList<>();
        tripleList.add(String.valueOf(ts));                // Add the timestamp as a string
        tripleList.add(val);                               // Add the value directly without escaping
        tripleList.add(encodeWriteSet(ws));                // Encode the write set and add it to the list

        // Use ListEncoderDecoder to encode the list into a string
        return ListEncoderDecoder.encode(tripleList);
    }

    // Decoder for a triple (ts, val, ws) using ListDecoder
    public static Triple decodeTriple(String encoded) {
        try {
            // Decode the string into an ArrayList
            List<String> parts = ListEncoderDecoder.decode(encoded);
            if (parts.size() < 3) throw new IllegalArgumentException("Invalid encoded triple format");

            int ts = Integer.parseInt(parts.get(0));         // First element is the timestamp
            String val = parts.get(1);                       // Second element is the value
            HashMap<Integer, String> ws = decodeWriteSet(parts.get(2)); // Third element is the encoded write set

            // Return the Triple object
            return new Triple(ts, val, ws);
        }
        catch (Exception e) {
            return INVALID_TRIPLE;  // Handle invalid input gracefully
        }
    }

    // Encodes a HashMap<Integer, String> (write set) into a single String
    public static String encodeWriteSet(HashMap<Integer, String> map) {
        HashMap<String, String> temp = new HashMap<>();
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            temp.put(Integer.toString(entry.getKey()), entry.getValue());
        }
        return SerializerTools.encode_hashmap(temp);
    }

    // Decodes a single String back into a HashMap<Integer, String> (write set)
    public static HashMap<Integer, String> decodeWriteSet(String encoded) {
        HashMap<Integer, String> map = new HashMap<>();
        HashMap<String, String> temp = SerializerTools.decode_hashmap(encoded.replace("\\;",";"));
        for (Map.Entry<String, String> entry : temp.entrySet()) {
            map.put(new Integer(entry.getKey()), entry.getValue());
        }
        return map;
    }
}
