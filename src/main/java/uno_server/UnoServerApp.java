package uno_server;

import uno_server.common.Server;

public class UnoServerApp {

    public static void main(String[] args) {
        try (Server server = new Server()) {
            while (true) {
                server.start();
            }
        }
    }
}