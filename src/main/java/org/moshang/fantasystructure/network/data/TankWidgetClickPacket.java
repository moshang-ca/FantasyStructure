package org.moshang.fantasystructure.network.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.moshang.fantasystructure.blockentity.container.BEFluidBus;

import java.util.function.Supplier;

public record TankWidgetClickPacket(BlockPos pos, int tank, boolean isFill) {
    // private final boolean isShiftDown;

    public static void encode(TankWidgetClickPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeInt(packet.tank);
        buf.writeBoolean(packet.isFill);
    }

    public static TankWidgetClickPacket decode(FriendlyByteBuf buf) {
        return new TankWidgetClickPacket(buf.readBlockPos(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(TankWidgetClickPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            handleServer(player, packet);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleServer(ServerPlayer player, TankWidgetClickPacket packet) {
        Level level = player.level();
        if (level.getBlockEntity(packet.pos) instanceof BEFluidBus be) {
            ItemStack heldItem = player.containerMenu.getCarried();
            if (packet.isFill) {
                be.fillTank(player, packet.tank, heldItem);
            } else {
                be.drainTank(player, packet.tank, heldItem);
            }
            level.sendBlockUpdated(player.blockPosition(), be.getBlockState(), be.getBlockState(), 3);
            be.setChanged();
        }
    }
}
