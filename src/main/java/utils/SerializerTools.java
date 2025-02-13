package utils;

import io.netty.buffer.ByteBuf;
import trustsystem.Proc;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class SerializerTools {
    //flattens one layer of {} adding each component to a list
    public static ArrayList<String> flatten_outer_brackets(String encoding){
        int nest_count = 0;
        int start_index = 0;
        char cur_char;
        ArrayList<String> ret = new ArrayList<>();
        for(int cur_index = 0; cur_index<encoding.length();cur_index++){
            cur_char = encoding.charAt(cur_index);
            if(cur_char == '{'){
                nest_count++;
                if(nest_count==1) {
                    start_index = cur_index;
                }
            }
            if(cur_char == '}'){
                nest_count--;
                if(nest_count == 0){
                    ret.add(encoding.substring(start_index,cur_index+1));
                }
            }
        }
        return ret;
    }
    public static ArrayList<String> decode_collection(String code){
        code = code.substring(1, code.length() - 1);
        return flatten_outer_brackets(code);

    }
    public static String encode_collection(Collection<?> target){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for(Object o : target){
            sb.append(o.toString()).append(" ");
        }
        String result = sb.toString().trim();
        return result + "}";
    }

    public static void encodeUTF8(String message, ByteBuf out) {
        byte[] stringBytes = message.getBytes(StandardCharsets.UTF_8);
        out.writeInt(stringBytes.length);
        out.writeBytes(stringBytes);
    }

    public static String decodeUTF8(ByteBuf buff) {
        byte[] stringBytes = new byte[buff.readInt()];
        buff.readBytes(stringBytes);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    public static void serializeHashMap(HashMap<String, String> map, ByteBuf out) {
        out.writeInt(map.size()); // Write the number of entries
        for (Map.Entry<String, String> entry : map.entrySet()) {
            encodeUTF8(entry.getKey(), out);
            encodeUTF8(entry.getValue(), out);
        }
    }

    public static HashMap<String, String> deserializeHashMap(ByteBuf in) {
        int size = in.readInt();
        HashMap<String, String> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = decodeUTF8(in);
            String value = decodeUTF8(in);
            map.put(key, value);
        }
        return map;
    }

    public static void serializeProc(Proc proc, ByteBuf out) {
        out.writeInt(proc.getId());
        encodeUTF8(proc.getAddress(), out);
        out.writeInt(proc.getPort());
    }

    public static String[] fix_commandline(String[] in){
        ArrayList<String> ret = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for(String s : in){
            if(s.contains("=")){
                if(sb.length()!=0){
                    ret.add(sb.toString().trim());
                }
                sb = new StringBuilder();
            }
            sb.append(s).append(" ");
        }
        String temp = sb.toString().trim();
        if(!ret.contains(temp)){
            ret.add(temp);
        }
        return ret.toArray(new String[0]);
    }

    public static Proc deserializeProc(ByteBuf in) {
        int id = in.readInt();
        String address = decodeUTF8(in);
        int port = in.readInt();
        return new Proc(id, address, port);
    }

}
