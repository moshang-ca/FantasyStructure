package org.moshang.fantasystructure.capability.handler;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.capability.recipe.IO;
import org.moshang.fantasystructure.api.recipe.FSRecipe;
import org.moshang.fantasystructure.api.recipe.ingredient.FluidIngredient;
import org.moshang.fantasystructure.capability.recipe.FluidRecipeCapability;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class FluidRecipeHandler extends RecipeHandler<FluidIngredient> {
    private final IFluidTransfer fluidHandler;

    public FluidRecipeHandler(IO io, IFluidTransfer fluidHandler) {
        super(io, FluidRecipeCapability.INSTANCE);
        this.fluidHandler = fluidHandler;
    }

    @Override
    public List<FluidIngredient> handleRecipeInner(IO io, FSRecipe recipe, List<FluidIngredient> left, @Nullable String slotName, boolean simulate) {
        if(left == null || left.isEmpty()) return null;

        IFluidTransfer handler = simulate ? (IFluidTransfer) fluidHandler.createSnapshot() : fluidHandler;
        List<FluidIngredient> remaining = new ArrayList<>(left);

        if(io == IO.IN) {
            return handleInput(handler, remaining, slotName);
        } else {
            return handleOutput(handler, remaining, slotName, simulate);
        }
    }

    private List<FluidIngredient> handleInput(IFluidTransfer fluidHandler, List<FluidIngredient> left, @Nullable String slotName) {
        Iterator<FluidIngredient> iter = left.iterator();

        while(iter.hasNext()) {
            FluidIngredient ingredient = iter.next();
            SLOT_SEARCH:
            for(int i = 0; i < fluidHandler.getTanks(); i++) {
                var fluidStack = fluidHandler.getFluidInTank(i);
                if(ingredient.test(fluidStack)) {
                    FluidStack[] stacks = ingredient.getStacks();
                    for(FluidStack stack : stacks) {
                        if(stack.isFluidEqual(fluidStack)) {
                            var extracted = fluidHandler.drain(i, stack, false, true);
                            stack.setAmount(stack.getAmount() - extracted.getAmount());
                            if(stack.isEmpty()) {
                                iter.remove();
                                break SLOT_SEARCH;
                            }
                        }
                    }
                }
            }
        }
        return left.isEmpty() ? null : left;
    }

    private List<FluidIngredient> handleOutput(IFluidTransfer fluidHandler, List<FluidIngredient> left, @Nullable String slotName, boolean simulate) {
        Iterator<FluidIngredient> iter = left.iterator();

        while(iter.hasNext()) {
            FluidIngredient ingredient = iter.next();
            var fluids = ingredient.getStacks();
            if(fluids.length == 0) {
                iter.remove();
                continue;
            }
            if(fluids.length == 1) {
                var output = fluids[0];
                if(!output.isEmpty()) {
                    for(int i = 0; i < fluidHandler.getTanks(); i++) {
                        long filled = fluidHandler.fill(i, output, false, true);
                        output.setAmount(output.getAmount() - filled);
                        if(output.isEmpty()) break;
                    }
                }
                if(output.isEmpty()) iter.remove();
            } else {
                var shuffled = Arrays.asList(Arrays.copyOf(fluids, fluids.length));
                assert getMachine() != null;
                random.setSeed(getMachine().getOffsetTimer());
                Collections.shuffle(shuffled, random);
                int index = -1;
                for(int i = 0; i< shuffled.size(); i++) {
                    var output = shuffled.get(i).copy();
                    if(!output.isEmpty()) {
                        for(int j = 0; j < fluidHandler.getTanks(); j++) {
                            long filled = fluidHandler.fill(i, output, true, false);
                            output.setAmount(filled);
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
                        if(!output.isEmpty()) {
                            for(int j = 0; j < fluidHandler.getTanks(); j++) {
                                long filled = fluidHandler.fill(j, output, true, false);
                                if(filled < output.getAmount()) {
                                    filled = fluidHandler.fill(j, output, false, true);
                                    output.setAmount(filled);
                                    if(output.isEmpty()) break;
                                }
                            }
                        }
                    }
                    iter.remove();
                }
            }
        }
        return left.isEmpty() ? null : left;
    }
}
