package utils;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

//this structure is used to remember the replys which come in with higher round numbers
//so they can be executed once the round is reached
public class RoundReplyBuffer {
    //maps the reply type to the map which maps round tags to a list of replys that couldn't be handled yet
    HashMap<Short,HashMap<Integer, ArrayList<ProtoReply>>> replys = new HashMap<>();

    int round = 0;

    //associates to each replyid the function that shell be executed once the round condition is met
    HashMap<Short, Consumer<? extends ProtoReply>> functions_map;

    public RoundReplyBuffer( HashMap<Short, Consumer<? extends ProtoReply>> functions_map){
        this.functions_map = functions_map;
    }
}
