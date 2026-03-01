package org.moshang.fantasystructure.data.blueprint;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.moshang.fantasystructure.util.StringUtil;
import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TagCache {
    private static final Cache<String, TagKey<Block>> GLOBAL_TAGS = CacheBuilder.newBuilder().maximumSize(512).expireAfterAccess(5, TimeUnit.MINUTES).softValues().build();
    private static final Logger LOGGER = LogUtils.getLogger();

    private TagCache() {}

    public static TagKey<Block> parse(String tagString) {
        try {
            return GLOBAL_TAGS.get(tagString, () -> parseUncached(tagString));
        } catch (ExecutionException e) {
            LOGGER.error("Failed to parse tag string: {}", tagString, e);
            return null;
        }
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
