package org.moshang.fantasystructure.capability.handler;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.capability.recipe.ItemRecipeCapability;

import java.util.*;

public class ItemSlotRecipeHandler extends RecipeHandler<Ingredient> {
    private final IItemHandler inventory;

    public ItemSlotRecipeHandler(IO io, IItemHandler inventory) {
        super(io, ItemRecipeCapability.INSTANCE);
        this.inventory = inventory;
    }

    @Override
    public List<Ingredient> handleRecipeInner(IO io, FSRecipe recipe, List<Ingredient> left, @Nullable String slotName, boolean simulate) {
        if(left == null || left.isEmpty()) return null;

        IItemHandler targetInv = simulate ? createSnapshot() : inventory;
        List<Ingredient> remaining = new ArrayList<>(left);

        if(io == IO.IN) {
            return handleInput(targetInv, remaining, slotName);
        } else {
            return handleOutput(targetInv, remaining, slotName, simulate);
        }
    }

    private List<Ingredient> handleInput(IItemHandler inv, List<Ingredient> left, @Nullable String slotName) {
        Iterator<Ingredient> it = left.iterator();

        while (it.hasNext()) {
            Ingredient ing = it.next();
            SLOT_SEARCH:
            for(int i = 0; i < inv.getSlots(); i++) {
                ItemStack itemStack = inv.getStackInSlot(i);
                if(ing.test(itemStack)) {
                    ItemStack[] stacks = ing.getItems();
                    for(ItemStack stack : stacks) {
                        if(stack.is(itemStack.getItem())) {
                            ItemStack extracted = inv.extractItem(i, stack.getCount(), false);
                            stack.setCount(stack.getCount() - extracted.getCount());
                            if(stack.isEmpty()) {
                                it.remove();
                                break SLOT_SEARCH;
                            }
                        }
                    }
                }
            }
        }
        return left.isEmpty() ? null : left;
    }

    private List<Ingredient> handleOutput(IItemHandler inv, List<Ingredient> left, @Nullable String slotName, boolean simulate) {
        Iterator<Ingredient> it = left.iterator();
        while (it.hasNext()) {
            Ingredient ing = it.next();
            var items = ing.getItems();
            if(items.length == 0) {
                it.remove();
                continue;
            }
            if(items.length == 1) {
                ItemStack output = items[0];
                if(!output.isEmpty()) {
                    for(int i = 0; i < inv.getSlots(); i++) {
                        ItemStack leftStack = inv.insertItem(i, output.copy(), false);
                        output.setCount(leftStack.getCount());
                        if(output.isEmpty()) break;
                    }
                }
                if(output.isEmpty()) it.remove();
            } else {
                var shuffled = Arrays.asList(Arrays.copyOf(items, items.length));
                assert getMachine() != null;
                random.setSeed(getMachine().getOffsetTimer());
                Collections.shuffle(shuffled, random);
                int index = -1;
                for(int i = 0; i < shuffled.size(); i++) {
                    var output = shuffled.get(i).copy();
                    if(!output.isEmpty()) {
                        for(int slot = 0; slot < inv.getSlots(); slot++) {
                            var leftStack = inv.insertItem(slot, output.copy(), true);
                            output.setCount(leftStack.getCount());
                            if(output.isEmpty()) break;
                        }
                    }
                    if(output.isEmpty()) {
                        index = i;
                        break;
                    }
                }
                if(index != -1) {
                    if(!simulate) {
                        var output = shuffled.get(index);
                        for(int i = 0; i < inv.getSlots(); i++) {
                            var leftStack = inv.insertItem(i, output, true);
                            if(leftStack.getCount() < output.getCount()) {
                                leftStack = inv.insertItem(i, output, false);
                                output.setCount(leftStack.getCount());
                                if(output.isEmpty()) break;
                            }
                        }
                    }
                    it.remove();
                }
            }
        }
        return left.isEmpty() ? null : left;
    }

    private IItemHandler createSnapshot() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        for(int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            stacks.add(stack);
        }
        return new ItemStackHandler(stacks);
    }
}
