package trustsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class TrustSystemSerializer {
    //flattens one layer of {} adding each component to a list
   public static ArrayList<String> flatten_brackets(String encoding){
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
}
