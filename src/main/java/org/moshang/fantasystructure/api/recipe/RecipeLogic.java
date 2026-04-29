package org.moshang.fantasystructure.api.recipe;

import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.IRecipeMachine;
import org.moshang.fantasystructure.api.capability.recipe.IO;

public class RecipeLogic implements IEnhancedManaged {
    public enum Status {
        IDLE, WORKING, BLOCKED, SUSPEND
    }

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(RecipeLogic.class);

    @Getter @Persisted @DescSynced @RequireRerender
    private Status status = Status.IDLE;

    @Getter
    private final IRecipeMachine machine;

    @Setter @Getter
    private int parallel = 0;
    private final MultiRecipeThread parent;
    @Getter
    private final String name;

    @Nullable @Getter @Setter @Persisted
    protected FSRecipe lastRecipe;
    /**
     * Similar to {@code lastRecipe}, but have not been modified, which can be found from {@link net.minecraft.world.item.crafting.RecipeManager}
     */
    @Nullable @Getter @Persisted
    protected FSRecipe lastOriginalRecipe;
    @Persisted @Getter @Setter
    protected int progress;
    @Getter @Setter @Persisted
    protected int duration;
    @Getter
    protected boolean recipeDirty;

    public RecipeLogic(IRecipeMachine machine, MultiRecipeThread parent, String name) {
        this.machine = machine;
        setStatus(true);
        this.parent = parent;
        this.name = name;
    }

    public void resetRecipeLogic() {
        recipeDirty = false;
        lastRecipe = null;
        lastOriginalRecipe = null;
        progress = 0;
        duration = 0;
        setStatus(Status.IDLE);
    }

    public double getProgressPercent() {
        return duration == 0 ? .0d : progress / (duration * 1.0);
    }

    @Override
    public void scheduleRenderUpdate() {
        machine.scheduleRenderUpdate();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        machine.markDirty();
    }

    public void serverTick() {
        if(!isSuspend()) {
            if (!isIdle() && lastRecipe != null) {
                if (progress < duration) {
                    handleRecipeWorking();
                }
                if (isIdle() || duration == 0) {
                } else if (isBlocked() || progress >= duration) {
                    onRecipeFinish();
                }
            }
        }
    }

    public void handleRecipeWorking() {
        assert lastRecipe != null;
        var result = handleTickRecipe(lastRecipe);
        if(result.isSuccess()) {
            setStatus(Status.WORKING);
            progress++;
        }
    }

    public FSRecipe.ActionResult handleTickRecipe(FSRecipe recipe) {
        if(recipe.hasTick()) {
            var result = recipe.matchTickRecipe(machine);
            if(result.isSuccess()) {
                recipe.handleTickRecipeIO(IO.IN, machine);
                recipe.handleTickRecipeIO(IO.OUT, machine);
            } else {
                return result;
            }
        }
        return FSRecipe.ActionResult.SUCCESS;
    }

    public boolean setupRecipe(Pair<FSRecipe, Integer> recipeData) {
        var recipe = recipeData.getFirst();
        if(recipe.handleRecipeIO(IO.IN, machine)) {
            recipeDirty = false;
            lastRecipe = recipe;
            setStatus(Status.WORKING);
            progress = 0;
            duration = recipe.duration();
            parallel = recipeData.getSecond();
            return true;
        }
        return false;
    }

    public void interruptRecipe() {
        if(lastRecipe != null) {
            setStatus(Status.IDLE);
            progress = 0;
            duration = 0;
        }
    }

    private void onRecipeFinish() {
        if(lastRecipe != null) {
            if(lastRecipe.handleRecipeIO(IO.OUT, machine)) {
                lastRecipe = null;
                progress = 0;
                duration = 0;
                if (parent != null) {
                    parent.onLogicComplete(this, lastOriginalRecipe);
                }
            } else {
                setStatus(Status.BLOCKED);
            }
        }
    }

    public void setStatus(Status status) {
        if (this.status != status) {
            this.status = status;
        }
    }

    public void setStatus(boolean formed) {
        setStatus(formed ? Status.IDLE : Status.SUSPEND);
    }

    public void setWorkingEnabled(boolean isWorkingAllowed) {
        if (!isWorkingAllowed) {
            setStatus(Status.SUSPEND);
        } else {
            if (lastRecipe != null && duration > 0) {
                setStatus(Status.WORKING);
            } else {
                setStatus(Status.IDLE);
            }
        }
    }

    public boolean isWorking() {
        return status == Status.WORKING;
    }

    public boolean isIdle() {
        return status == Status.IDLE;
    }

    public boolean isSuspend() {
        return status == Status.SUSPEND;
    }

    public boolean isBlocked() {
        return status == Status.BLOCKED;
    }
}
