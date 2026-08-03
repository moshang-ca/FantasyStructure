package org.moshang.fantasystructure.integration.kubejs.event;

import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.block.BlockRecipeControllerKjs;
import org.moshang.fantasystructure.api.blockentity.BERecipeControllerKjs;

import java.util.Collections;
import java.util.Set;

public class FSControllerRegistryEventJS {

    @SuppressWarnings({"LombokGetterMayBeUsed", "unused"})
    public static class ControllerBuilder extends BuilderBase<Block> {
        private int baseParallel = 1;
        private int parallelLimit = -1;
        private int maxThreads = 1;
        private float hardness = 1.5f;

        public ControllerBuilder(ResourceLocation i) {
            super(i);
        }

        // We need use standard JavaBean property accessors for KubeJS to work
        public ControllerBuilder baseParallel(int baseParallel) {
            this.baseParallel = baseParallel;
            return this;
        }

        public ControllerBuilder parallelLimit(int parallelLimit) {
            this.parallelLimit = parallelLimit;
            return this;
        }

        public ControllerBuilder maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }

        public ControllerBuilder hardness(float hardness) {
            this.hardness = hardness;
            return this;
        }

        public int getBaseParallel() {
            return baseParallel;
        }

        public int getParallelLimit() {
            return parallelLimit;
        }

        public int getMaxThreads() {
            return maxThreads;
        }

        public float getHardness() {
            return hardness;
        }

        @Override
        public RegistryInfo<Block> getRegistryType() {
            return RegistryInfo.BLOCK;
        }

        @Override
        public Block createObject() {
            return new BlockRecipeControllerKjs(
                    (int) this.hardness,
                    () -> {
                        @SuppressWarnings("unchecked")
                        BlockEntityType<BERecipeControllerKjs> type = (BlockEntityType<BERecipeControllerKjs>)
                                RegistryInfo.BLOCK_ENTITY_TYPE.getValue(this.id);
                        if (type == null) {
                            FantasyStructure.LOGGER.warn(
                                    "BlockEntityType not found for controller '{}'", this.id);
                        }
                        return type;
                    },
                    this.id,
                    this.baseParallel,
                    this.parallelLimit,
                    this.maxThreads
            );
        }

        @Override
        public void createAdditionalObjects() {
            var self = this;
            RegistryInfo.ITEM.addBuilder(new BuilderBase<>(this.id) {
                @Override
                public RegistryInfo<Item> getRegistryType() {
                    return RegistryInfo.ITEM;
                }

                @Override
                public Item createObject() {
                    return new BlockItem(self.get(), new Item.Properties());
                }
            });

            RegistryInfo.BLOCK_ENTITY_TYPE.addBuilder(new ControllerBlockEntityBuilder(this.id, this));
        }

        @SuppressWarnings({"rawtypes"})
        private static class ControllerBlockEntityBuilder extends BuilderBase<BlockEntityType> {
            private final ControllerBuilder blockBuilder;

            public ControllerBlockEntityBuilder(ResourceLocation id, ControllerBuilder blockBuilder) {
                super(id);
                this.blockBuilder = blockBuilder;
            }

            @Override
            public RegistryInfo<BlockEntityType> getRegistryType() {
                return RegistryInfo.BLOCK_ENTITY_TYPE;
            }

            @Override
            @SuppressWarnings("DataFlowIssue")
            public BlockEntityType createObject() {
                Block block = blockBuilder.get();
                Set<Block> validBlocks = Collections.singleton(block);
                BlockEntityType.BlockEntitySupplier<BERecipeControllerKjs> supplier = BERecipeControllerKjs::new;
                return new BlockEntityType<>(supplier, validBlocks, null);
            }
        }
    }
}
