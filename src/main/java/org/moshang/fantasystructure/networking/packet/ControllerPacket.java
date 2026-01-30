package org.moshang.fantasystructure.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.moshang.fantasystructure.api.blockentity.BlockEntityControllerBase;
import org.moshang.fantasystructure.screen.ControllerScreen;

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

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if(ctx.get().getDirection().getReceptionSide().isClient()) {
                Minecraft minecraft = Minecraft.getInstance();
                ClientLevel level = minecraft.level;

                if(level != null) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if(be instanceof BlockEntityControllerBase controller) {
                        controller.updateClientData(isFormed, id);

                        if(minecraft.screen instanceof ControllerScreen scr) {
                            
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
