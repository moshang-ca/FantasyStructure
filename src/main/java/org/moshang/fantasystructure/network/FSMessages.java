package org.moshang.fantasystructure.network;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.network.data.FilterTankWidgetClickPacket;
import org.moshang.fantasystructure.network.data.TankWidgetClickPacket;
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

        INSTANCE.registerMessage(id(), TankWidgetClickPacket.class,
                TankWidgetClickPacket::encode,
                TankWidgetClickPacket::decode,
                TankWidgetClickPacket::handle);
        INSTANCE.registerMessage(id(), FilterTankWidgetClickPacket.class,
                FilterTankWidgetClickPacket::encode,
                FilterTankWidgetClickPacket::decode,
                FilterTankWidgetClickPacket::handle);

        LOGGER.info("FSMessage init complete!");
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToClient(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
