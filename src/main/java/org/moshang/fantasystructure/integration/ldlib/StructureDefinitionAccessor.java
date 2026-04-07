package org.moshang.fantasystructure.integration.ldlib;


import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.accessor.CustomObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.payload.FriendlyBufPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;

public class StructureDefinitionAccessor extends CustomObjectAccessor<FSStructureDefinitions.StructureDefinition> {
    public StructureDefinitionAccessor() {
        super(FSStructureDefinitions.StructureDefinition.class, true);
    }

    @Override
    public FSStructureDefinitions.StructureDefinition deserialize(AccessorOp op, ITypedPayload<?> payload) {
        if(payload instanceof FriendlyBufPayload buffer) {
            return FSStructureDefinitions.StructureDefinition.fromNetwork(buffer);
        }
        return null;
    }

    @Override
    public ITypedPayload<?> serialize(AccessorOp op, FSStructureDefinitions.StructureDefinition value) {
        FriendlyByteBuf holder = new FriendlyByteBuf(Unpooled.buffer());
        value.toNetwork(holder);
        return FriendlyBufPayload.of(holder);
    }
}
