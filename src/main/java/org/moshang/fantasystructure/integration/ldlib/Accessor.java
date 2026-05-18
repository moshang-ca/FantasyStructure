package org.moshang.fantasystructure.integration.ldlib;


import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.accessor.CustomObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.payload.FriendlyBufPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.moshang.fantasystructure.registry.FSStructureDefinitions;

import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class Accessor<T> extends CustomObjectAccessor<T> {
    public Accessor(Class<T> type, boolean includesChildren) {
        super(type, includesChildren);
    }

    public static final CustomObjectAccessor<FSStructureDefinitions.StructureDefinition> STRUCTURE_DEFINITION =
            create(FSStructureDefinitions.StructureDefinition.class,
                    FSStructureDefinitions.StructureDefinition::toNetwork,
                    FSStructureDefinitions.StructureDefinition::fromNetwork);

    public static <T> CustomObjectAccessor<T> create(Class<T> type,
                                                     BiConsumer<T, FriendlyByteBuf> serializer,
                                                     Function<FriendlyByteBuf, T> deserializer) {
        return new CustomObjectAccessor<>(type, true) {
            @Override
            public ITypedPayload<?> serialize(AccessorOp op, T value) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                serializer.accept(value, buf);
                return FriendlyBufPayload.of(buf);
            }

            @Override
            public T deserialize(AccessorOp op, ITypedPayload<?> payload) {
                if (payload instanceof FriendlyBufPayload buffer) {
                    return deserializer.apply(buffer.getPayload());
                }
                return null;
            }
        };
    }
}
