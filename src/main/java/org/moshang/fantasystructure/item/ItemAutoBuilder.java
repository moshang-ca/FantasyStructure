package org.moshang.fantasystructure.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.blockentity.BlockEntityControllerBase;

import java.util.ArrayList;
import java.util.List;

public class ItemAutoBuilder extends Item {
    private static final String BUILDER_DATA = FantasyStructure.MODID + "_builder_data";

    public ItemAutoBuilder(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if(player.isShiftKeyDown()) {
            clearContainerPositions(stack);
            if(level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("cleared"),
                        false
                );
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos useOnPos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(useOnPos);

        if(!level.isClientSide) {
            if (blockEntity instanceof BlockEntityControllerBase controller) {
                controller.autoBuild(stack, context.getPlayer().isCreative());
                return InteractionResult.SUCCESS;
            }
        }

        if(isContainer(context.getLevel(), useOnPos)) {
            List<BlockPos> containerPositions = loadTag(stack);
            if(!containerPositions.contains(useOnPos)) {
                if(containerPositions.size() < 4) {
                    containerPositions.add(useOnPos);
                } else {
                    containerPositions.remove(0);
                    containerPositions.add(useOnPos);
                }
                saveTag(stack, containerPositions);
                return InteractionResult.SUCCESS;
            } else {
                if (context.getPlayer() != null) {
                    if(level.isClientSide) {
                        context.getPlayer().displayClientMessage(
                                Component.translatable("already_saved"),
                                false
                        );
                    }
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    private boolean isContainer(Level level, BlockPos pos) {
        if(level == null || !level.isLoaded(pos)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if(blockEntity != null) {
            return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
        }
        return false;
    }

    public static @Nullable ItemStack shrinkMaterials(Level level, ItemStack stack, ResourceLocation blockId) {
        List<BlockPos> containerPositions = loadTag(stack);
        for(BlockPos pos : containerPositions) {
            if(!level.isLoaded(pos)) continue;

            BlockEntity be = level.getBlockEntity(pos);
            if(be == null) continue;

            LazyOptional<IItemHandler> capability = be.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if(capability.isPresent()) {
                IItemHandler itemHandler = capability.orElseThrow(
                        () -> new IllegalStateException("Item handler capacity is present but empty!")
                );

                for(int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    ItemStack stackInSlot = itemHandler.getStackInSlot(slot);
                    if(!stackInSlot.isEmpty()) {
                        Item item = stackInSlot.getItem();
                        if(item instanceof BlockItem blockItem) {
                            ResourceLocation slotBlockId = ForgeRegistries.BLOCKS.getKey(blockItem.getBlock());
                            if(blockId.equals(slotBlockId)) {
                                stackInSlot.shrink(1);
                                return stackInSlot;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void clearContainerPositions(ItemStack stack) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.put("containerPositions", new ListTag());
        stack.getOrCreateTag().put(BUILDER_DATA, tag);
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag root = stack.getOrCreateTag();

        if(!root.contains(BUILDER_DATA, CompoundTag.TAG_COMPOUND)) {
            CompoundTag tag = new CompoundTag();
            root.put(BUILDER_DATA, tag);
        }

        return root.getCompound(BUILDER_DATA);
    }

    private static void saveTag(ItemStack stack, List<BlockPos> containerPositions) {
        CompoundTag tag = getOrCreateTag(stack);
        ListTag listTag = new ListTag();

        for(BlockPos pos : containerPositions) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            listTag.add(posTag);
        }
        tag.put("containerPositions", listTag);
        stack.getOrCreateTag().put(BUILDER_DATA, tag);
    }

    private static List<BlockPos> loadTag (ItemStack stack) {
        CompoundTag tag = getOrCreateTag(stack);
        List<BlockPos> containerPositions = new ArrayList<>();
        ListTag listTag = tag.getList("containerPositions", 10);
        for(int i = 0; i < listTag.size(); ++i) {
            CompoundTag posTag = listTag.getCompound(i);
            containerPositions.add(
                    new BlockPos(
                            posTag.getInt("x"),
                            posTag.getInt("y"),
                            posTag.getInt("z")
                    )
            );
        }
        return containerPositions;
    }
}
