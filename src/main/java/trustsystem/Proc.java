package trustsystem;


import java.util.Objects;

public class Proc {
    int id;
    String address;
    int port;
    public Proc(int id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Proc other = (Proc) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public int getId() {
        return id;
    }

    public String getAddress(){
        return address;
    }

    public int getPort(){
        return port;
    }
    @Override
    public String toString() {
        return "{"+id +" "+address+" "+port+"}";
    }
    public static Proc parse(String code){
        code = code.trim();
        code = code.substring(1, code.length() - 1);
        String[] components = code.split(" ");
        return new Proc(new Integer(components[0]), components[1], new Integer(components[2]));
    }
}
