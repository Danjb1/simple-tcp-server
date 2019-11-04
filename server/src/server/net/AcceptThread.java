package server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import server.Server;

public class AcceptThread implements Runnable {

    private Server server;

    private ServerSocket serverSocket;

    public AcceptThread(Server server, ServerSocket serverSocket)
            throws IOException {
        this.server = server;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {

        System.out.println(
                "Server listening on port " + Server.SERVER_PORT);

        while (!server.isExiting()) {

            Socket clientSocket = null;
            Client client = null;

            // Listen for a new Client
            try {
                clientSocket = serverSocket.accept();
                client = new Client(clientSocket);

            } catch (IOException e) {

                e.printStackTrace();

                // Close the new Socket if an error occurs
                SocketUtils.close(clientSocket);

                // Kill the server if the socket has closed
                if (serverSocket.isClosed()) {
                    server.kill();
                    break;
                }
            }

            // Add the new client
            if (client != null) {
                server.addClient(client);
            }
        }
    }

}
