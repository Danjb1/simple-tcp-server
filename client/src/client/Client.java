package client;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import client.net.Connection;
import client.net.SocketUtils;
import client.packets.Packet;
import client.packets.PacketContext;
import client.packets.PacketHandler;
import client.packets.PacketRegistry;

/**
 *
 * @author Dan Bryce
 */
public class Client implements PacketContext {

    private static final int MS_PER_FRAME = 16; // 60 fps

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 7780;

    private ExecutorService executor;
    private Connection connection;

    private boolean exiting;

    public void run() {

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

    private void start() {
        connect(SERVER_ADDRESS, SERVER_PORT);
        executor = Executors.newCachedThreadPool();
        executor.execute(connection.getPacketReaderThread());
    }

    private void connect(String address, int port) {
        Socket socket = null;
        try {
            socket = new Socket(address, port);
            connection = new Connection(socket);
        } catch (IOException e) {
            SocketUtils.close(socket);
            e.printStackTrace();
        }
    }

    private void tick() {
        handlePackets();
    }

    private void handlePackets() {
        for (Packet p : connection.getPacketsReceived()) {
            PacketHandler handler = PacketRegistry.getPacketHandler(p.id);
            if (handler != null) {
                handler.apply(p, this);
            }
        }
    }

    @Override
    public void loggedIn() {
        System.out.println("Logged in!");
    }

}
