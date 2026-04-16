package org.moshang.fantasystructure.api.recipe.ingredient;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.crafting.CraftingHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FluidIngredient implements Predicate<FluidStack> {
    public static final FluidIngredient EMPTY = new FluidIngredient(Stream.empty(), 0, null);

    @Nullable
    private FluidStack[] fluidStacks;
    public Value[] values;

    @Getter
    private long amount;
    @Getter
    @Nullable
    private CompoundTag nbt;
    private boolean changed = true;

    private FluidIngredient(Stream<? extends Value> empty, long amount, @Nullable CompoundTag nbt) {
        this.values = empty.toArray(Value[]::new);
        this.amount = amount;
        this.nbt = nbt;
    }

    public static FluidIngredient fromValues(Stream<? extends Value> empty, long amount, @Nullable CompoundTag nbt) {
        FluidIngredient ingredient = new FluidIngredient(empty, amount, nbt);
        return ingredient.isEmpty() ? EMPTY : ingredient;
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeCollection(Arrays.asList(this.getStacks()), (buf1, stack) -> stack.writeToBuf(buf1));
        buf.writeLong(this.amount);
        buf.writeNbt(this.nbt);
    }

    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("amount", amount * FluidHelper.getBucket() / 1000);
        if(this.nbt != null) {
            obj.addProperty("nbt", this.nbt.getAsString());
        }
        if(this.values.length == 1) {
            obj.add("values", this.values[0].serialize());
        }
        JsonArray array = new JsonArray();
        for (Value value : this.values) {
            array.add(value.serialize());
        }
        obj.add("value", array);
        return obj;
    }

    public FluidIngredient copy() {
        return new FluidIngredient(Arrays.stream(this.values).map(Value::copy), this.amount, this.nbt == null ? null : this.nbt.copy());
    }

    public FluidIngredient copy(long amount) {
        return new FluidIngredient(Arrays.stream(this.values).map(Value::copy), amount, this.nbt == null ? null : this.nbt.copy());
    }

    @Override
    public boolean test(FluidStack fluidStack) {
        if(fluidStack == null) {
            return false;
        }
        if(this.isEmpty()) {
            return fluidStack.isEmpty();
        }
        if(this.nbt != null && !this.nbt.equals(fluidStack.getTag())) {
            return false;
        }
        for (FluidStack stack : this.getStacks()) {
            if(!fluidStack.isFluidEqual(stack)) continue;
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return this.values.length == 0;
    }

    public FluidStack[] getStacks() {
        if(changed || this.fluidStacks == null) {
            this.fluidStacks = Arrays.stream(this.values).flatMap(entry -> entry.getStacks().stream()).distinct().map(fluid -> FluidStack.create(fluid, amount, nbt)).toArray(FluidStack[]::new);
            this.changed = false;
        }
        return this.fluidStacks;
    }

    public void setAmount(long amount) {
        this.amount = amount;
        this.changed = true;
    }

    public void setNbt(@Nullable CompoundTag nbt) {
        this.nbt = nbt;
        this.changed = true;
    }

    public static FluidIngredient of() {
        return EMPTY;
    }

    public static FluidIngredient of(long amount, Fluid... items) {
        return of(Arrays.stream(items), amount, null);
    }

    public static FluidIngredient of(FluidStack... stacks) {
        return of(Arrays.stream(stacks).map(FluidStack::getFluid), stacks.length == 0 ? 0 : stacks[0].getAmount(), stacks.length == 0 ? null : stacks[0].getTag());
    }

    public static FluidIngredient of(Stream<Fluid> stacks, long amount, CompoundTag nbt) {
        return fromValues(stacks.filter(stack -> stack != null && !stack.isSame(Fluids.EMPTY)).map(FluidValue::new), amount, nbt);
    }

    public static FluidIngredient of(TagKey<Fluid> tag, long amount) {
        return fromValues(Stream.of(new TagValue(tag)), amount, null);
    }

    public static FluidIngredient of(TagKey<Fluid> tag, long amount, CompoundTag nbt) {
        return fromValues(Stream.of(new TagValue(tag)), amount, nbt);
    }

    public static FluidIngredient fromJson(JsonElement json) {
        return fromJson(json, true);
    }

    public static FluidIngredient fromJson(JsonElement json, boolean allowAir) {
        if(json != null && !json.isJsonNull()) {
            if(!json.isJsonObject()) throw new JsonParseException("Expected JSON object");
            JsonObject obj = GsonHelper.convertToJsonObject(json, "ingredient");
            long amount = GsonHelper.getAsLong(obj, "amount");
            CompoundTag nbt = obj.has("nbt") ? CraftingHelper.getNBT(obj.get("nbt")) : null;
            if(GsonHelper.isObjectNode(obj, "value")) {
                return fromValues(Stream.of(valueFromJson(GsonHelper.getAsJsonObject(obj, "value"))), amount, nbt);
            } else if(GsonHelper.isArrayNode(obj, "value")) {
                JsonArray array = GsonHelper.getAsJsonArray(obj, "value");
                if(array.isEmpty() && !allowAir) {
                    throw new JsonParseException("Fluid array can't be empty, at least one value is required");
                }
                return fromValues(StreamSupport.stream(array.spliterator(), false).map(jsonElement -> valueFromJson(GsonHelper.convertToJsonObject(jsonElement, "fluid"))), amount, nbt);
            } else {
                throw new JsonParseException("Expect value to be either an array or an object");
            }
        } else {
            throw new JsonParseException("Cannot parse null or empty json object");
        }
    }

    @SuppressWarnings("removal")
    public static Value valueFromJson(JsonObject json) {
        if(json.has("fluid") && json.has("tag")) {
            throw new JsonParseException("A fluid ingredient can't be both fluid and tag. ");
        }
        if(json.has("fluid")) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(GsonHelper.getAsString(json, "fluid")));
            return new FluidValue(fluid);
        }
        if(json.has("tag")) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, resourceLocation);
            return new TagValue(tagKey);
        }
        throw new JsonParseException("A fluid ingredient must be either fluid or tag");
    }

    public static FluidIngredient fromNetwork(FriendlyByteBuf buf) {
        return FluidIngredient.fromValues(buf.readList(FluidStack::readFromBuf).stream().map(fluidStack -> new FluidValue(fluidStack.getFluid())), buf.readVarLong(), buf.readNbt());
    }

    public interface Value {
        Collection<Fluid> getStacks();
        JsonObject serialize();
        Value copy();
    }

    public static class FluidValue implements Value {
        @Getter @Setter
        private Fluid fluid;

        public FluidValue(Fluid fluid) {
            this.fluid = fluid;
        }

        @Override
        public Collection<Fluid> getStacks() {
            return Collections.singleton(fluid);
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("fluid", ForgeRegistries.FLUIDS.getKey(fluid).toString());
            return jsonObject;
        }

        @Override
        public Value copy() {
            return new FluidValue(fluid);
        }
    }

    public static class TagValue implements Value {
        @Getter @Setter
        private TagKey<Fluid> tag;

        public TagValue(TagKey<Fluid> tag) {
            this.tag = tag;
        }

        @Override
        public Collection<Fluid> getStacks() {
            List<Fluid> list = Lists.newArrayList();
            var tagCollection = ForgeRegistries.FLUIDS.tags();
            if(tagCollection != null) {
                for(var fluid : tagCollection.getTag(this.tag)) {
                    list.add(fluid);
                }
            }
            return list;
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("tag", tag.location().toString());
            return jsonObject;
        }

        @Override
        public Value copy() {
            return new TagValue(this.tag);
        }
    }
}
