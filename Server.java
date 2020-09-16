package advisor;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class Server {
    private HttpServer server;

    public Server(int port) {
        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress(port), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        server.stop(1);
    }

    public void getCode(Client client) {
        server.start();
        server.createContext("/",
                exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    String result;

                    if (query != null && query.contains("code=")) {
                        result = "Got the code. Return back to your program.";
                        client.setCode(query.replace("code=", ""));
                    } else {
                        result = "Not found authorization code. Try again.";
                    }
                    System.out.println(result);

                    exchange.sendResponseHeaders(200, result.length());
                    exchange.getResponseBody().write(result.getBytes());
                    exchange.close();
                });
    }
}
