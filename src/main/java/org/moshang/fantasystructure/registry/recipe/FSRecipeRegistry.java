package org.moshang.fantasystructure.registry.recipe;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.moshang.fantasystructure.FantasyStructure;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class FSRecipeRegistry<K, V> implements Iterable<V> {
    protected final BiMap<K, V> registry;
    @Getter
    protected final ResourceLocation registryName;
    @Getter
    protected boolean frozen = true;

    public FSRecipeRegistry(ResourceLocation registryName) {
        this.registry = HashBiMap.create();
        this.registryName = registryName;
    }

    public boolean containsKey(K key) {
        return registry.containsKey(key);
    }

    public boolean containValue(V value) {
        return registry.containsValue(value);
    }

    public void register(K key, V value) {
        if(frozen) {
            throw new IllegalStateException("Already frozen");
        }
        if(containsKey(key)) {
            throw new IllegalStateException("Duplicate key");
        }
        registry.put(key, value);
    }

    public void freeze() {
        frozen = true;
    }

    public void unfreeze() {
        frozen = false;
    }
    
    public Set<Map.Entry<K,V>> entrySet() {
        return registry.entrySet();
    }

    public Set<V> values() {
        return registry.values();
    }

    public Set<K> keys() {
        return registry.keySet();
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return registry.values().iterator();
    }

    @Nullable
    public V get(K key) {
        return registry.get(key);
    }

    public V getOrDefault(K key, V defaultValue) {
        return registry.getOrDefault(key, defaultValue);
    }

    public K getKey(V value) {
        return registry.inverse().get(value);
    }

    public K getOrDefaultKey(V key, K defaultKey) {
        return registry.inverse().getOrDefault(key, defaultKey);
    }

    public boolean remove(K name) {
        return registry.remove(name) != null;
    }

    public static class String<V> extends FSRecipeRegistry<java.lang.String, V> {
        public String(ResourceLocation registryName) {
            super(registryName);
        }
    }

    public static class RL<V> extends FSRecipeRegistry<ResourceLocation, V> {
        public RL(ResourceLocation registryName) {
            super(registryName);
        }

        public V get(java.lang.String id) {
            return this.registry.get(FantasyStructure.id(id));
        }
    }
}
