package org.moshang.fantasystructure.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ControllerPacket(BlockPos pos, ResourceLocation id, boolean isFormed) {
    public ControllerPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readResourceLocation(), buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeResourceLocation(id);
        buf.writeBoolean(isFormed);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}
