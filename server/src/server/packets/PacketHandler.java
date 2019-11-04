package server.packets;

public abstract class PacketHandler {

    public abstract void apply(Packet packetBeforeCast, PacketContext context);

}
