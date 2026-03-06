package org.moshang.fantasystructure.util;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.scene.ParticleManager;
import com.lowdragmc.lowdraglib.core.mixins.accessor.EntityAccessor;
import com.lowdragmc.lowdraglib.utils.DummyWorld;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector3f;
import org.moshang.fantasystructure.data.BlockInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings({"rawtypes", "unchecked", "ConstantValue"})
public class PreviewDummyWorld extends DummyWorld {
    @Setter
    private Predicate<BlockPos> renderFilter;
    public final WeakReference<Level> proxyWorld;
    @Getter
    public final Map<BlockPos, BlockInfo> renderedBlocks = new HashMap<>();
    public final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    public final Map<Integer, Entity> entities = new Int2ObjectArrayMap<>();

    @Getter
    public final Vector3f minPos = new Vector3f(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    @Getter
    public final Vector3f maxPos = new Vector3f(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    public PreviewDummyWorld() {
        super(Minecraft.getInstance().level);
        proxyWorld = new WeakReference<>(null);
    }

    public PreviewDummyWorld(Level world) {
        super(world);
        proxyWorld = new WeakReference<>(world);
    }

    public void clear() {
        renderedBlocks.clear();
        blockEntities.clear();
        entities.clear();
    }

    public void addBlocks(Long2ObjectOpenHashMap<BlockInfo> renderedBlocks) {
        renderedBlocks.forEach((k, v) -> addBlock(BlockPos.of(k), v));
    }

    public void addBlock(BlockPos pos, BlockInfo blockInfo) {
        if (blockInfo.getExpectedState().isAir())
            return;
        this.renderedBlocks.put(pos, blockInfo);
        this.blockEntities.remove(pos);
        minPos.x = (Math.min(minPos.x, pos.getX()));
        minPos.y = (Math.min(minPos.y, pos.getY()));
        minPos.z = (Math.min(minPos.z, pos.getZ()));
        maxPos.x = (Math.max(maxPos.x, pos.getX()));
        maxPos.y = (Math.max(maxPos.y, pos.getY()));
        maxPos.z = (Math.max(maxPos.z, pos.getZ()));
    }

    public BlockInfo removeBlock(BlockPos pos) {
        this.blockEntities.remove(pos);
        return this.renderedBlocks.remove(pos);
    }

    // wth? mcp issue
    public void setInnerBlockEntity(BlockEntity pBlockEntity) {
        blockEntities.put(pBlockEntity.getBlockPos(), pBlockEntity);
    }

    @Override
    public void setBlockEntity(BlockEntity pBlockEntity) {
        blockEntities.put(pBlockEntity.getBlockPos(), pBlockEntity);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int a, int b) {
        this.renderedBlocks.put(pos, BlockInfo.fromBlockState(state));
        this.blockEntities.remove(pos);
        return true;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        if (renderFilter != null && !renderFilter.test(pos))
            return null;
        Level proxy = proxyWorld.get();
        return proxy != null ? proxy.getBlockEntity(pos) : blockEntities.computeIfAbsent(pos, p -> renderedBlocks.getOrDefault(p, BlockInfo.EMPTY).getBlockEntity(this, p));
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (renderFilter != null && !renderFilter.test(pos))
            return Blocks.AIR.defaultBlockState(); //return air if not rendering this
        Level proxy = proxyWorld.get();
        return proxy != null ? proxy.getBlockState(pos) : renderedBlocks.getOrDefault(pos, BlockInfo.EMPTY).getExpectedState();
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        ((EntityAccessor) entity).invokeSetLevel(this);
        if (entity instanceof ItemFrame itemFrame)
            itemFrame.setItem(withUnsafeNBTDiscarded(itemFrame.getItem()));
        if (entity instanceof ArmorStand armorStand)
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values())
                armorStand.setItemSlot(equipmentSlot,
                        withUnsafeNBTDiscarded(armorStand.getItemBySlot(equipmentSlot)));
        entities.put(entity.getId(), entity);
        return true;
    }

    public static ItemStack withUnsafeNBTDiscarded(ItemStack stack) {
        if (stack.getTag() == null)
            return stack;
        ItemStack copy = stack.copy();
        stack.getTag()
                .getAllKeys()
                .stream()
                .filter(TrackedDummyWorld::isUnsafeItemNBTKey)
                .forEach(copy::removeTagKey);
        if (copy.getTag().isEmpty())
            copy.setTag(null);
        return copy;
    }

    public static boolean isUnsafeItemNBTKey(String name) {
        if (name.equals(EnchantedBookItem.TAG_STORED_ENCHANTMENTS))
            return false;
        if (name.equals("Enchantments"))
            return false;
        if (name.contains("Potion"))
            return false;
        if (name.contains("Damage"))
            return false;
        return !name.equals("display");
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return super.getEntities();
    }

    @Override
    public Entity getEntity(int id) {
        for (Entity entity : entities.values()) {
            if (entity.getId() == id && entity.isAlive())
                return entity;
        }
        return super.getEntity(id);
    }

    public Vector3f getSize() {
        return new Vector3f(maxPos.x - minPos.x + 1, maxPos.y - minPos.y + 1, maxPos.z - minPos.z + 1);
    }

    @Override
    public ChunkSource getChunkSource() {
        Level proxy = proxyWorld.get();
        return proxy == null ? super.getChunkSource() : proxy.getChunkSource();
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        Level proxy = proxyWorld.get();
        return proxy == null ? super.getFluidState(pPos) : proxy.getFluidState(pPos);
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        Level proxy = proxyWorld.get();
        return proxy == null ? super.getBlockTint(blockPos, colorResolver) : proxy.getBlockTint(blockPos, colorResolver);
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pos) {
        Level proxy = proxyWorld.get();
        return proxy == null ? super.getBiome(pos) : proxy.getBiome(pos);
    }

    @Override
    public void setParticleManager(ParticleManager particleManager) {
        super.setParticleManager(particleManager);
        if (proxyWorld.get() instanceof DummyWorld dummyWorld) {
            dummyWorld.setParticleManager(particleManager);
        }
    }

    @Override
    public ParticleManager getParticleManager() {
        ParticleManager particleManager = super.getParticleManager();
        if (particleManager == null && proxyWorld.get() instanceof DummyWorld dummyWorld) {
            return dummyWorld.getParticleManager();
        }
        return particleManager;
    }

    public void tickWorld() {
        var iter = entities.values().iterator();
        while (iter.hasNext()) {
            var entity = iter.next();
            entity.tickCount++;
            entity.setOldPosAndRot();
            entity.tick();

            if (entity.getY() <= -.5f)
                entity.discard();

            if (!entity.isAlive())
                iter.remove();
        }

        for (var entry : renderedBlocks.entrySet()) {
            var blockState = entry.getValue().getExpectedState();
            var blockEntity = getBlockEntity(entry.getKey());
            if (blockEntity != null && blockEntity.getType().isValid(blockState)) {
                try {
                    BlockEntityTicker ticker = blockState.getTicker(this, blockEntity.getType());
                    if (ticker != null) {
                        ticker.tick(this, entry.getKey(), blockState, blockEntity);
                    }
                } catch (Exception e) {
                    LDLib.LOGGER.error("error while update DummyWorld tick, pos {} type {}", entry.getKey(), blockEntity.getType(), e);
                }
            }
        }
    }

    public List<Entity> getAllEntities() {
        var entities = new ArrayList<>(this.entities.values());
        if (proxyWorld.get() instanceof TrackedDummyWorld trackedDummyWorld)
            entities.addAll(trackedDummyWorld.getAllEntities());
        return entities;
    }
}
