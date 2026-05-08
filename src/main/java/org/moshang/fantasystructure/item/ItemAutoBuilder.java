package org.moshang.fantasystructure.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.blockentity.BlockEntityAbstractController;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO: Open screen when use shift + right-click
// TODO: Remove clear position storage function when use shift + right-click
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemAutoBuilder extends Item {
    private static final String BUILDER_DATA = FantasyStructure.MODID + "_builder_data";

    public ItemAutoBuilder(int stackTo) {
        super(new Item.Properties().stacksTo(stackTo));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));

        ItemStack stack = player.getItemInHand(hand);

        if(player.isShiftKeyDown()) {
            clearContainerPositions(stack);
            player.displayClientMessage(Component.translatable("cleared"), true);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if(level.isClientSide) return InteractionResult.PASS;

        if(player != null && player.isShiftKeyDown()) return InteractionResult.PASS;

        BlockPos useOnPos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(useOnPos);

        if (blockEntity instanceof BlockEntityAbstractController controller) {
            controller.autoBuild(stack, context.getPlayer().isCreative());
            return InteractionResult.SUCCESS;
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
                    context.getPlayer()
                            .displayClientMessage(Component.translatable("already_saved"), true);
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

    public static ItemStack shrinkMaterials(Level level, ItemStack stack, BlockState blockState) {
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

                for(int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stackInSlot = itemHandler.getStackInSlot(i);
                    if(!stackInSlot.isEmpty()) {
                        Item item = stackInSlot.getItem();
                        if(item instanceof BlockItem blockItem) {
                            if(blockState.is(blockItem.getBlock())) {
                                return itemHandler.extractItem(i, 1, false);
                            }
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static Fluid shrinkMaterials(Level level, ItemStack stack, FluidState fluidState) {
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

                for(int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stackInSlot = itemHandler.getStackInSlot(i);
                    LazyOptional<IFluidHandlerItem> fluidCapability = stackInSlot.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
                    if(fluidCapability.isPresent()) {
                        var handler = fluidCapability.orElseThrow(() -> new IllegalStateException("Fluid handler capacity is present but empty!"));
                        var toDrain = new FluidStack(fluidState.getType(), 1000);
                        var drained = handler.drain(toDrain, IFluidHandler.FluidAction.SIMULATE);
                        if (!drained.isEmpty() && drained.getAmount() >= 1000) {
                            handler.drain(drained, IFluidHandler.FluidAction.EXECUTE);
                            if(FluidUtil.getFluidContained(stackInSlot).map(fluidStack -> !fluidStack.isEmpty())
                                    .orElse(false)) {
                                itemHandler.extractItem(i, 1, false);
                                itemHandler.insertItem(i, handler.getContainer(), false);
                            }
                            return fluidState.getType();
                        }
                    }
                }
            }
        }
        return Fluids.EMPTY;
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
