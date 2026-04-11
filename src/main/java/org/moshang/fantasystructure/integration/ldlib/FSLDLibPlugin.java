package org.moshang.fantasystructure.integration.ldlib;

import com.lowdragmc.lowdraglib.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.payload.FriendlyBufPayload;

@LDLibPlugin
@SuppressWarnings("unused")
public class FSLDLibPlugin implements ILDLibPlugin {
    @Override
    public void onLoad() {
        TypedPayloadRegistries.register(FriendlyBufPayload.class, FriendlyBufPayload::new, Accessor.STRUCTURE_DEFINITION, 1000);
        TypedPayloadRegistries.register(FriendlyBufPayload.class, FriendlyBufPayload::new, Accessor.STORAGE_DATA, 1000);
    }
}
