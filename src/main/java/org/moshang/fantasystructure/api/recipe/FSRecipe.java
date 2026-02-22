package org.moshang.fantasystructure.api.recipe;

import com.google.common.collect.Table;
import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeCapabilityHolder;
import org.moshang.fantasystructure.api.capability.recipe.IRecipeHandler;
import org.moshang.fantasystructure.api.capability.recipe.RecipeCapability;
import org.moshang.fantasystructure.api.recipe.content.Content;
import org.moshang.fantasystructure.api.recipe.content.ContentModifier;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Getter
@Accessors(fluent = true)
public class FSRecipe implements Recipe<Container> {
    @Setter
    private FSRecipeType recipeType;
    private final ResourceLocation id;
    private final Map<RecipeCapability<?>, List<Content>> inputs;
    private final Map<RecipeCapability<?>, List<Content>> outputs;
    private int duration;
    private int priority;
    @Nullable
    private CompoundTag data;
    private Boolean hasTick;

    public FSRecipe(FSRecipeType recipeType, ResourceLocation id, Map<RecipeCapability<?>,
            List<Content>> inputs, Map<RecipeCapability<?>, List<Content>> outputs, int duration, int priority,@Nullable CompoundTag data) {
        this.recipeType = recipeType;
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
        this.duration = duration;
        this.priority = priority;
        this.data = data;
    }

    public Map<RecipeCapability<?>, List<Content>> copyContents(Map<RecipeCapability<?>, List<Content>> contents, @Nullable ContentModifier modifier) {
        Map<RecipeCapability<?>, List<Content>> copy = new HashMap<>();
        for(var entry : contents.entrySet()) {
            var contentList = entry.getValue();
            var cap = entry.getKey();
            if(contentList != null && !contentList.isEmpty()) {
                List<Content> contentListCopy = new ArrayList<>();
                for(var content : contentList) {
                    contentListCopy.add(content.copy(cap, modifier));
                }
                copy.put(cap, contentListCopy);
            }
        }
        return copy;
    }

    public FSRecipe copy(ResourceLocation id) {
        return new FSRecipe(recipeType, id, copyContents(inputs, null), copyContents(outputs, null), duration, priority, data);
    }

    public FSRecipe copy() {
        return copy(id);
    }

    public FSRecipe copy(ContentModifier modifier, boolean modifyDuration) {
        var copy = new FSRecipe(recipeType, id,
                copyContents(inputs, modifier),
                copyContents(outputs, modifier),
                duration, priority, data);
        if(modifyDuration) {
            copy.duration = modifier.apply(this.duration).intValue();
        }
        return copy;
    }

    @Override
    public boolean matches(Container pContainer, Level pLevel) {
        return false;
    }

    @Override
    public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public RecipeType<?> getType() {
        return recipeType;
    }

    public ActionResult matchRecipe(IRecipeCapabilityHolder holder) {
        if(!holder.hasProxy()) return ActionResult.FAILURE_NO_REASON;
        var result = matchRecipe(false, IO.IN, holder, inputs);
        if(!result.isSuccess()) return result;
        result = matchRecipe(false, IO.OUT, holder, outputs);
        if(!result.isSuccess()) return result;
        return ActionResult.SUCCESS;
    }

    public ActionResult matchTickRecipe(IRecipeCapabilityHolder holder) {
        if(hasTick()) {
            if(!holder.hasProxy()) return ActionResult.FAILURE_NO_REASON;
            var result = matchRecipe(true, IO.IN, holder, inputs);
            if(!result.isSuccess()) return result;
            result = matchRecipe(true, IO.OUT, holder, outputs);
            if(!result.isSuccess()) return result;
        }
        return ActionResult.SUCCESS;
    }

    private ActionResult matchRecipe(boolean perTick, IO io, IRecipeCapabilityHolder holder, Map<RecipeCapability<?>, List<Content>> contents) {
        Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> capabilityProxies = holder.getRecipeCapabilitiesProxy();

        for(var entry : contents.entrySet()) {
            Set<IRecipeHandler<?>> used = new HashSet<>();
            List content = new ArrayList<>();
            Map<String, List> contentSlot = new HashMap<>();
            for(var cont : entry.getValue()) {
                if(cont.isPerTick() != perTick) continue;
                if(cont.getSlotName().isEmpty()) {
                    content.add(cont.getContent());
                } else {
                    contentSlot.computeIfAbsent(cont.getSlotName(), s -> new ArrayList<>()).add(cont.getContent());
                }
            }
            RecipeCapability<?> capability = entry.getKey();
            content = content.stream().map(capability::copyContent).toList();
            if(content.isEmpty() && contentSlot.isEmpty()) continue;
            if(content.isEmpty()) content = null;

            var result = handlerContentsInternal(io, io, capabilityProxies, capability, used, content,
                    contentSlot, content, contentSlot, true);
            if(result.getA() != null || !result.getB().isEmpty()) {
                return ActionResult.FAILURE_NO_REASON;
            }
        }
        return ActionResult.SUCCESS;
    }

    public boolean handleTickRecipeIO(IO io, IRecipeCapabilityHolder holder) {
        if(!holder.hasProxy()) return false;
        return handleRecipe(true, io, holder, io == IO.IN ? inputs : outputs);
    }

    public boolean handleRecipeIO(IO io, IRecipeCapabilityHolder holder) {
        if(!holder.hasProxy()) return false;
        return handleRecipe(false, io, holder, io == IO.IN ? inputs : outputs);
    }

    /**
     * @param perTick should produce or consume every tick
     * @param io the handle direction
     * @param holder the recipe capability holder, usually is a controller entity
     * @param contents the content of inputs or outputs
     * @return whether the handler can handle the contents
    * */
    public boolean handleRecipe(boolean perTick, IO io, IRecipeCapabilityHolder holder, Map<RecipeCapability<?>, List<Content>> contents) {
        Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> capabilityProxies = holder.getRecipeCapabilitiesProxy();
        for(var entry : contents.entrySet()) {
            Set<IRecipeHandler<?>> used = new HashSet<>();
            List content = new ArrayList<>();
            Map<String, List> contentSlot = new HashMap<>();
            List contentSearch  = new ArrayList();
            Map<String, List> contentSlotSearch = new HashMap<>();
            for(var cont : entry.getValue()) {
                if(cont.isPerTick() != perTick) continue;
                if(cont.getSlotName().isEmpty()) {
                    contentSearch.add(cont.getContent());
                } else {
                    contentSlotSearch.computeIfAbsent(cont.getSlotName(), s -> new ArrayList<>()).add(cont.getContent());
                }
                if(cont.getChance() >= 1 || FantasyStructure.RND.nextFloat() < cont.getChance()) {
                    if(cont.getSlotName().isEmpty()) {
                        content.add(cont.getContent());
                    } else {
                        contentSlot.computeIfAbsent(cont.getSlotName(), s -> new ArrayList<>()).add(cont.getContent());
                    }
                }
            }
            RecipeCapability<?> capability = entry.getKey();
            content = content.stream().map(capability::copyContent).toList();
            if(content.isEmpty() && contentSlot.isEmpty()) continue;
            if(content.isEmpty()) content = null;

            var result = handlerContentsInternal(io, io, capabilityProxies, capability, used, content, contentSlot, contentSearch, contentSlotSearch, false);
            if(result.getA() != null || !result.getB().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * This will handle the content, will be called in {@code matchRecipe()} and {@code handleRecipe()}
     * @param capIO the handler's IO
     * @param io the demanded IO, usually same as the capIO.
     * @param capabilityProxies all capability handlers carried by a holder
     * @param capability capability that handle the content needed
     * @param used all handler that failed to handle the contents
     * @param content all need to be handled contents
     * @param contentSlot all need to be handled contents, which should be put in exactly slot
     * @param contentSearch the full list of contents, this won't be affected by the chance.
     * @param contentSlotSearch the full list of contents with exactly slot name, this won't be affected by the chance.
     * @param simulate should be executed in real environment
     * */
    private Tuple<List, Map<String, List>> handlerContentsInternal(
            IO capIO, IO io, Table<IO, RecipeCapability<?>, List<IRecipeHandler<?>>> capabilityProxies,
            RecipeCapability<?> capability, Set<IRecipeHandler<?>> used,
            List content, Map<String, List> contentSlot,
            List contentSearch, Map<String, List> contentSlotSearch,
            boolean simulate) {
        if (capabilityProxies.contains(capIO, capability)) {
            var handlers = capabilityProxies.get(capIO, capability);
            if (handlers != null) {
                for (IRecipeHandler<?> handler : handlers) {
                    if (!handler.isDistinct()) continue;
                    var slotNames = handler.getSlotNames();
                    var result = handler.handleRecipe(io, this, contentSearch, null, true);
                    if (result == null) {
                        if (slotNames.containsAll(contentSlotSearch.keySet())) {
                            boolean success = true;
                            for (var entry : contentSlotSearch.entrySet()) {
                                List<?> left = handler.handleRecipe(io, this, entry.getValue(), entry.getKey(), true);
                                if (left != null) {
                                    success = false;
                                    break;
                                }
                            }
                            if (success) {
                                if (!simulate) {
                                    for (var entry : contentSlot.entrySet()) {
                                        handler.handleRecipe(io, this, entry.getValue(), entry.getKey(), false);
                                    }
                                }
                                contentSlot.clear();
                            }
                        }
                        if (contentSlot.isEmpty()) {
                            if (!simulate) {
                                if (content != null) {
                                    handler.handleRecipe(io, this, content, null, false);
                                }
                            }
                            content = null;
                        }
                    }
                    if (content == null && contentSlot.isEmpty()) {
                        break;
                    }
                }
            }
            if (content != null || !contentSlot.isEmpty()) {
                if (handlers != null) {
                    for (IRecipeHandler<?> proxy : handlers) {
                        if (used.contains(proxy) || proxy.isDistinct()) continue;
                        used.add(proxy);
                        if (content != null) {
                            content = proxy.handleRecipe(io, this, content, null, simulate);
                        }
                        var slotNames = proxy.getSlotNames();
                        if (!slotNames.isEmpty()) {
                            Iterator<String> iterator = contentSlot.keySet().iterator();
                            while (iterator.hasNext()) {
                                String key = iterator.next();
                                if (slotNames.contains(key)) {
                                    List<?> left = proxy.handleRecipe(io, this, contentSlot.get(key), key, simulate);
                                    if (left == null) iterator.remove();
                                }
                            }
                        }
                        if (content == null && contentSlot.isEmpty()) break;
                    }
                }
            }
        }
        return new Tuple<>(content, contentSlot);
    }

    public boolean hasTick() {
        if(hasTick == null) {
            for(List<Content> contents : inputs.values()) {
                for(Content content : contents) {
                    if(content.isPerTick()) {
                        hasTick = true;
                        return true;
                    }
                }
            }
            for(List<Content> contents : outputs.values()) {
                for(Content content : contents) {
                    if(content.isPerTick()) {
                        hasTick = true;
                        return true;
                    }
                }
            }
            hasTick = false;
        }
        return hasTick;
    }

    public static Pair<FSRecipe, Integer> calculateParallel(IRecipeCapabilityHolder holder, FSRecipe recipe, int maxParallel) {
        if(maxParallel == 1) {
            return Pair.of(recipe, 1);
        }
        var parallel = tryParallel(holder, recipe, 1, maxParallel);
        return parallel != null ? parallel : Pair.of(recipe, 1);
    }

    @Nullable
    public static Pair<FSRecipe, Integer> tryParallel(IRecipeCapabilityHolder holder, FSRecipe recipe, int min, int max) {
        if(min > max) return null;

        int mid = (min + max) / 2;
        var copied = recipe.copy(ContentModifier.multiplier(mid), false);
        if(!copied.matchRecipe(holder).isSuccess() || !copied.matchTickRecipe(holder).isSuccess()) {
            return tryParallel(holder, recipe, min, mid - 1);
        } else {
            if(mid == max) return Pair.of(copied, mid);
            var more = tryParallel(holder, recipe, mid + 1, max);
            return more != null ? more : Pair.of(copied, mid);
        }
    }

    public record ActionResult(boolean isSuccess, @Nullable Supplier<Component> reason) {
        public static final ActionResult SUCCESS = new ActionResult(true, null);
        public static final ActionResult FAILURE_NO_REASON = new ActionResult(false, null);

        public static ActionResult fail(@Nullable Supplier<Component> reason) {
            return new ActionResult(false, reason);
        }
    }
}
