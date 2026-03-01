package org.moshang.fantasystructure.api.recipe;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.IMachine;
import org.moshang.fantasystructure.api.capability.recipe.IO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("CallToPrintStackTrace")
public class RecipeLogic implements IEnhancedManaged {
    public enum Status {
        IDLE, WORKING, SUSPEND
    }

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(RecipeLogic.class);

    @Getter @Persisted @DescSynced @RequireRerender
    private Status status = Status.IDLE;

    @Getter
    private final IMachine machine;
    private List<FSRecipe> failedMatches;
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
    @Nullable
    protected CompletableFuture<List<FSRecipe>> completableFuture = null;

    public RecipeLogic(IMachine machine, boolean formed) {
        this.machine = machine;
        setStatus(formed);
    }

    public void resetRecipeLogic() {
        recipeDirty = false;
        lastRecipe = null;
        lastOriginalRecipe = null;
        progress = 0;
        duration = 0;
        failedMatches = null;
        setStatus(Status.IDLE);
    }

    public double getProgressPercent() {
        return duration == 0 ? .0d : progress / (duration * 1.0);
    }

    public RecipeManager getRecipeManager() {
        return Platform.getMinecraftServer().getRecipeManager();
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
                } else if (progress >= duration) {
                    onRecipeFinish();
                }
            } else if (lastRecipe != null) {
                findAndHandleRecipe();
            } else if (getMachine().getOffsetTimer() % 5 == 0) {
                findAndHandleRecipe();
                if (failedMatches != null) {
                    for (var recipe : failedMatches) {
                        if (checkMatchedRecipeAvailable(recipe)) break;
                    }
                }
            }
        } else {
            if(completableFuture != null) {
                completableFuture.cancel(true);
                completableFuture = null;
            }
        }
    }

    private boolean checkMatchedRecipeAvailable(FSRecipe recipe) {
        var modified = machine.doModifyRecipe(recipe);
        if(modified != null) {
            if(modified.matchRecipe(machine).isSuccess() && modified.matchTickRecipe(machine).isSuccess()) {
                setupRecipe(modified);
            }
            if(lastRecipe != null && getStatus() == Status.WORKING) {
                lastOriginalRecipe = recipe;
                failedMatches = null;
                return true;
            }
        }
        return false;
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

    private List<FSRecipe> searchRecipe() {
        return machine.getRecipeType().searchRecipes(getRecipeManager(), machine);
    }

    public void findAndHandleRecipe() {
        failedMatches = null;
        if(!recipeDirty && lastRecipe != null &&
                lastRecipe.matchRecipe(machine).isSuccess() &&
                lastRecipe.matchTickRecipe(machine).isSuccess()) {         // Try last success recipe first.
            FSRecipe recipe = lastRecipe;
            lastRecipe = null;
            lastOriginalRecipe = null;
            setupRecipe(recipe);
        } else {
            lastRecipe = null;
            lastOriginalRecipe = null;
            if(completableFuture == null) {
                completableFuture = supplyAsyncSearchTask();
            } else if(completableFuture.isDone()) {
                var lastFuture = completableFuture;
                completableFuture = null;
                if(!lastFuture.isCancelled()) {
                    try {
                        var matches = lastFuture.join().stream()
                                .toList();
                        matches = matches.stream().filter(match -> match.matchRecipe(machine).isSuccess()).toList();
                        if(!matches.isEmpty()) {
                            handleSearchingRecipe(matches);
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        completableFuture = supplyAsyncSearchTask();
                    }
                } else {
                    handleSearchingRecipe(searchRecipe());
                }
            }
            recipeDirty = false;
        }
    }

    private CompletableFuture<List<FSRecipe>> supplyAsyncSearchTask() {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("search recipe", this::searchRecipe), Util.backgroundExecutor());
    }

    private void handleSearchingRecipe(List<FSRecipe> recipes) {
        for(var recipe : recipes) {
            if(checkMatchedRecipeAvailable(recipe)) break;
            if(failedMatches == null) {
                failedMatches = new ArrayList<>();
            }
            failedMatches.add(recipe);
        }
    }

    public void setupRecipe(FSRecipe recipe) {
        if(recipe.handleRecipeIO(IO.IN, machine)) {
            recipeDirty = false;
            lastRecipe = recipe;
            setStatus(Status.WORKING);
            progress = 0;
            duration = recipe.duration();
        }
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
            lastRecipe.handleRecipeIO(IO.OUT, machine);
            if(!recipeDirty) {
                if(lastOriginalRecipe != null) {
                    var modified = machine.doModifyRecipe(lastOriginalRecipe);
                    if(modified != null) {
                        lastRecipe = modified;
                    } else {
                        markLastRecipeDirty();
                    }
                } else {
                    markLastRecipeDirty();
                }
            }
            if(!recipeDirty &&
                    lastRecipe.matchRecipe(machine).isSuccess() &&
                    lastRecipe.matchTickRecipe(machine).isSuccess()) {
                setupRecipe(lastRecipe);
            } else {
                setStatus(Status.IDLE);
                progress = 0;
                duration = 0;
            }
        }
    }

    public void markLastRecipeDirty() {
        recipeDirty = true;
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

    public boolean isActive() {
        return isWorking() || (isSuspend() && lastRecipe != null && duration > 0);
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
}
