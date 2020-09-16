package advisor;

public class Main {

    public static void main(String[] args) {
        Client client = new Client();
        if (args.length > 0) {
            client.setAccess(args[1]);
            client.setResource(args[3]);
            client.setPageSize(Integer.parseInt(args[5]));
        }
        client.start();
    }
}
