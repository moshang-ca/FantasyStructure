package org.moshang.fantasystructure.data.blueprint;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.moshang.fantasystructure.util.StringUtil;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class TagCache {
    private static final Map<String, TagKey<Block>> GLOBAL_TAGS = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    private TagCache() {}

    public static TagKey<Block> parse(String tagString) {
        return GLOBAL_TAGS.computeIfAbsent(tagString, TagCache::parseUncached);
    }

    public static TagKey<Block> parseUncached(String tagString) {
        try {
            String tagContent = StringUtil.parseStringByChar(tagString, '{', '}').trim();

            if(!tagContent.isEmpty()) {
                ResourceLocation tagId = ResourceLocation.tryParse(tagContent);
                if(tagId != null) {
                    return TagKey.create(Registries.BLOCK, tagId);
                }
            }
            return null;
        } catch (Exception e) {
            LOGGER.error("Error while parsing tag string.", e);
            return null;
        }
    }
}
