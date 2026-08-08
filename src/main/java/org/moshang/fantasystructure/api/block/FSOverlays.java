package org.moshang.fantasystructure.api.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class FSOverlays {
    private static final Map<Block, ResourceLocation> OVERLAY_LOCATIONS = new IdentityHashMap<>();
    private static final Map<Block, ResourceLocation> OVERLAY_FORMED_LOCATIONS = new IdentityHashMap<>();

    private FSOverlays() {
    }

    public static void register(Block block, ResourceLocation overlayLocation) {
        OVERLAY_LOCATIONS.put(block, overlayLocation);
    }

    public static void registerFormed(Block block, ResourceLocation overlayFormedLocation) {
        OVERLAY_FORMED_LOCATIONS.put(block, overlayFormedLocation);
    }

    public static ResourceLocation get(Block block) {
        return OVERLAY_LOCATIONS.get(block);
    }

    public static ResourceLocation getFormed(Block block) {
        return OVERLAY_FORMED_LOCATIONS.get(block);
    }

    public static Map<Block, ResourceLocation> all() {
        return Collections.unmodifiableMap(OVERLAY_LOCATIONS);
    }

    public static Map<Block, ResourceLocation> allFormed() {
        return Collections.unmodifiableMap(OVERLAY_FORMED_LOCATIONS);
    }
}
