package org.moshang.fantasystructure.integration.ae2.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class StorageData implements MEStorage {
    private final UUID id;
    private final Map<AEKeyType, TypeChannel> channels = new HashMap<>();

    @Setter
    private long maxTypes = 128L;
    @Setter
    private long maxBytes = (1 << 12) * maxTypes;

    @Setter
    private boolean dirty = false;

    public StorageData() {
        this(UUID.randomUUID());
    }

    public StorageData(UUID id) {
        this.id = id;
    }

    public void addCapacity(long additionalTypes, long additionalBytes) {
        this.maxTypes += additionalTypes;
        this.maxBytes += additionalBytes;
    }

    @Override
    public Component getDescription() {
        return Component.empty();
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        AEKeyType keyType = what.getType();
        TypeChannel channel = channels.get(keyType);
        if(channel == null) return 0L;

        long extracted = channel.extract(what, amount, mode);
        if(extracted > 0 && mode == Actionable.SIMULATE) {
            setDirty(true);
        }
        return extracted;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        AEKeyType keyType = what.getType();
        TypeChannel channel = getOrCreate(keyType);

        long inserted = channel.insert(what, amount, maxTypes, maxBytes, mode);
        if(inserted > 0 && mode == Actionable.MODULATE) {
            setDirty(true);
        }
        return inserted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for(var channel : channels.values()) {
            for(var entry : Object2LongMaps.fastIterable(channel.amounts)) {
                out.add(entry.getKey(), entry.getLongValue());
            }
        }
    }

    public void restoreItem(AEKey what, long amount) {
        AEKeyType type = what.getType();
        TypeChannel channel = getOrCreate(type);

        long currentAmount = channel.amounts.getLong(what);
        channel.amounts.put(what, amount + currentAmount);
        if(currentAmount <= 0) channel.types++;
        channel.count += amount;
        channel.updateUsedBytes();
    }

    public long getUsedBytes() {
        return channels.values().stream().mapToLong(TypeChannel::getUsedBytes).sum();
    }

    public long getUsedTypes() {
        return channels.values().stream().mapToLong(TypeChannel::getTypes).sum();
    }

    private TypeChannel getOrCreate(AEKeyType keyType) {
        return channels.computeIfAbsent(keyType, TypeChannel::new);
    }

    protected static class TypeChannel {
        private static final int BYTES_PER_TYPE = 8;
        private final AEKeyType keyType;
        @Getter
        private final Object2LongMap<AEKey> amounts = new Object2LongOpenHashMap<>();
        @Getter
        private long types = 0L;
        @Getter
        private long count = 0L;
        @Getter
        private long usedBytes = 0L;

        public TypeChannel(AEKeyType keyType) {
            this.keyType = keyType;
        }

        private void updateUsedBytes() {
            long bytesForItemCount = (count + getUnusedItemCount()) / keyType.getAmountPerByte();
            this.usedBytes = types * BYTES_PER_TYPE + bytesForItemCount;
        }

        private int getUnusedItemCount() {
            int div = (int) (count % keyType.getAmountPerByte());
            return div == 0 ? div : keyType.getAmountPerByte() - div;
        }

        private long getRemainingItemCount(long maxBytes) {
            long remainingBytes = maxBytes - usedBytes;
            return remainingBytes <= 0 ? 0 : remainingBytes * keyType.getAmountPerByte() + getUnusedItemCount();
        }

        private boolean canHoldNewType(long maxTypes, long maxBytes) {
            long bytesFree = maxBytes - usedBytes;
            return (bytesFree > BYTES_PER_TYPE || (bytesFree == BYTES_PER_TYPE && getUnusedItemCount() > 0))
                    && types < maxTypes;
        }

        private long insert(AEKey what, long amount, long maxTypes, long maxBytes, Actionable mode) {
            if(amount <= 0) return 0L;

            long currentAmount = amounts.getLong(what);
            boolean isNewType = currentAmount <= 0;
            if(isNewType && !canHoldNewType(maxTypes, maxBytes)) return 0L;

            long remainingCount = getRemainingItemCount(maxBytes);
            if(isNewType) {
                remainingCount -= (long) BYTES_PER_TYPE * keyType.getAmountPerByte();
                if(remainingCount <= 0) return 0L;
            }

            long inserted = Math.min(amount, remainingCount);
            if(mode == Actionable.MODULATE && inserted > 0) {
                amounts.put(what, currentAmount + inserted);
                if(isNewType) types++;
                count += inserted;
                updateUsedBytes();
            }
            return inserted;
        }

        private long extract(AEKey what, long amount, Actionable mode) {
            long currentAmount = amounts.getLong(what);
            if(currentAmount <= 0) return 0L;

            long extracted = Math.min(amount, currentAmount);
            if(mode == Actionable.MODULATE && extracted > 0) {
                long newAmount = currentAmount - extracted;
                if(newAmount <= 0) {
                    amounts.removeLong(what);
                    types--;
                } else {
                    amounts.put(what, newAmount);
                }
                count -= extracted;
                updateUsedBytes();
            }
            return extracted;
        }
    }
}
