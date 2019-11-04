package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import server.net.AcceptThread;
import server.net.Client;
import server.net.SendPacketTask;
import server.net.SocketUtils;
import server.packets.Packet;
import server.packets.builders.LoginSuccessPacketBuilder;
import server.packets.from_server.KickPacket;

public class Server {

    private static final int MS_PER_FRAME = 16; // 60 fps

    public static final int SERVER_PORT = 7780;

    // Kick reasons
    public static final byte SKIP_REASON = 0;
    public static final byte SERVER_CLOSED = 1;

    /**
     * Time to wait for all clients to get kicked before shutting down.
     */
    private static final long SHUTDOWN_TIME = 0;

    private ExecutorService executor;

    private ServerSocket serverSocket;

    private boolean exiting;
    private volatile boolean dead;

    private List<Client> clients = new ArrayList<>();

    public Server() throws IOException {
        serverSocket = new ServerSocket(SERVER_PORT);
        executor = Executors.newCachedThreadPool();
    }

    public void requestExit() {
        exiting = true;
    }

    public boolean isExiting() {
        return exiting;
    }

    public void kill() {

        // Ensure this method is only called once
        if (dead) {
            return;
        }

        dead = true;
        exiting = true;

        // Stop accepting new clients
        SocketUtils.close(serverSocket);

        // Kick all clients
        for (Client client : clients) {
            Packet kickPacket = new KickPacket(SERVER_CLOSED);
            executor.execute(new SendPacketTask(client, kickPacket));
        }

        // Wait for clients to get kicked
        executor.shutdown();
        try {
            executor.awaitTermination(SHUTDOWN_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Kill all client connections
        for (Client client : clients) {
            client.kill();
        }
    }

    public void addClient(Client client) {
        synchronized (clients) {
            System.out.println("Client connected from " + client.getAddress());
            Packet packet = new LoginSuccessPacketBuilder().build();
            executor.execute(new SendPacketTask(client, packet));
            clients.add(client);
        }
    }

    public void run() throws IOException {

        start();

        while (!exiting) {
            long before = System.currentTimeMillis();

            tick();

            int elapsed = (int) (System.currentTimeMillis() - before);
            int sleepTime = MS_PER_FRAME - elapsed;

            if (sleepTime < 1) {
                sleepTime = 1;
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void start() throws IOException {
        // Listen for new clients in a separate thread
        AcceptThread acceptThread = new AcceptThread(this, serverSocket);
        executor.execute(acceptThread);
    }

    private void tick() {
        removeDeadClients();
    }

    private void removeDeadClients() {
         synchronized (clients) {
             clients = clients
                     .stream()
                     .filter(c -> !c.isDead())
                     .collect(Collectors.toList());
         }
    }

}
