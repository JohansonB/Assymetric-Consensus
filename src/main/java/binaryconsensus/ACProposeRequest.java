package binaryconsensus;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ACProposeRequest extends ProtoRequest {
    public static final short REQUEST_ID = 5;
    private boolean input;

    public ACProposeRequest(boolean input){
        super(REQUEST_ID);
        this.input = input;
    }
    public boolean getInput(){
        return input;
    }
}
