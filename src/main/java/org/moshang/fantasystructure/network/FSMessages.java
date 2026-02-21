package org.moshang.fantasystructure.network;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.network.packet.ControllerPacket;
import org.slf4j.Logger;

@SuppressWarnings("removal")
public class FSMessages {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(FantasyStructure.MODID, "messages"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        INSTANCE.registerMessage(id(), ControllerPacket.class, ControllerPacket::toBytes, ControllerPacket::new, ControllerPacket::handle);

        LOGGER.info("init complete!");
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToClient(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
