package org.moshang.fantasystructure.network.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.blockentity.container.BEFluidBus;

import java.util.function.Supplier;

public record FilterTankWidgetClickPacket(BlockPos pos, int tank, @Nullable Fluid fluid) {
    public static void encode(FilterTankWidgetClickPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeInt(packet.tank);
        buf.writeResourceLocation(ForgeRegistries.FLUIDS.getKey(packet.fluid));
    }

    public static FilterTankWidgetClickPacket decode(FriendlyByteBuf buf) {
        return new FilterTankWidgetClickPacket(buf.readBlockPos(), buf.readInt(),
                ForgeRegistries.FLUIDS.getValue(buf.readResourceLocation()));
    }

    public static void handle(FilterTankWidgetClickPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if(player == null) return;
            if(player.level().getBlockEntity(packet.pos()) instanceof BEFluidBus be) {
                be.setValidatorInTank(packet.tank(), packet.fluid());
                player.level().sendBlockUpdated(packet.pos(), be.getBlockState(), be.getBlockState(), 3);
                be.setChanged();
            }
        });
    }
}
