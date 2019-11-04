package server.main;

import server.Server;

public class ServerLauncher {

    public static void main(String[] args) {

        Server server = null;

        try {

            server = new Server();
            server.run();

        } catch (Exception e) {

            // Exit cleanly if an error occurs
            if (server != null) {
                server.kill();
            }

            e.printStackTrace();
        }
    }

}
