package tigase.http.modules;

import tigase.server.Packet;

import java.util.concurrent.CompletableFuture;

public interface PacketSender {

	boolean sendPacket(Packet packet);

	default CompletableFuture<Packet> sendPacketAndWait(Packet packet) {
		return sendPacketAndWait(packet, null);
	}

	CompletableFuture<Packet> sendPacketAndWait(Packet packet, Integer timeout);
	
}
