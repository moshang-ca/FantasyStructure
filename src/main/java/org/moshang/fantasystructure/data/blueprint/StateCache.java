package org.moshang.fantasystructure.data.blueprint;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("removal")
public class StateCache {
    private static final Map<String, BlockState> GLOBAL_BLOCK_STATES = new ConcurrentHashMap<>(1024);

    private static final Logger LOGGER = LogUtils.getLogger();

    private StateCache() {}

    public static BlockState parse(String stateString) {
        return GLOBAL_BLOCK_STATES.computeIfAbsent(stateString, StateCache::parseUncached);
    }

    private static BlockState parseUncached(String stateString) {
        try {
            String blockId;
            Map<String, String> properties = new HashMap<>();

            blockId = stateString.substring(
                    stateString.indexOf('{') + 1,
                    stateString.indexOf('}')
            );
            if (stateString.contains("[")) {
                String propString = stateString.substring(
                        stateString.indexOf('[') + 1, stateString.lastIndexOf(']')
                );

                for (String propPair : propString.split(",")) {
                    String[] kv = propPair.split("=");
                    if (kv.length == 2) {
                        properties.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
            if (block == null) {
                return Blocks.AIR.defaultBlockState();
            }

            BlockState state = block.defaultBlockState();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                Property<?> prop = block.getStateDefinition().getProperty(entry.getKey());
                if (prop != null) {
                    state = setPropertyValue(state, prop, entry.getValue());
                }
            }

            return state;
        } catch (Exception e) {
            LOGGER.error("Failed to parse block state: {}", stateString, e);
            return Blocks.AIR.defaultBlockState();
        }
    }

    private static <T extends Comparable<T>> BlockState setPropertyValue
            (BlockState state, Property<T> property, String valueName) {
        return property.getValue(valueName)
                .map(value -> state.setValue(property, value))
                .orElse(state);
    }
}