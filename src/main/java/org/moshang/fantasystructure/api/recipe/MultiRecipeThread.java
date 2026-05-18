package org.moshang.fantasystructure.api.recipe;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.api.blockentity.IRecipeMachine;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MultiRecipeThread implements IEnhancedManaged {
    private static final int MAX_FAILURE = 40;

    private static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MultiRecipeThread.class);
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

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

    @Getter @Persisted
    private final RecipeLogic[] threads;
    @Getter
    private final IRecipeMachine machine;

    @Getter
    private final int maxThreads;
    @Getter @DescSynced
    private int freeParallels;
    @Getter @Persisted
    private int availableThread;
    @Nullable
    protected CompletableFuture<List<FSRecipe>> completedFuture = null;

    private final List<Runnable> wakeupListener = new ArrayList<>();
    private int failures = 0;
    private boolean isSleep = false;

    public MultiRecipeThread(IRecipeMachine machine, int maxThreads) {
        this.machine = machine;
        this.freeParallels = machine.getMaxParallel();
        this.maxThreads = maxThreads;
        this.availableThread = maxThreads;
        threads = new RecipeLogic[maxThreads];
        for(int i = 0; i < maxThreads; ++i) {
            threads[i] = new RecipeLogic(machine, this, "thread_%s".formatted(i));
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void serverTick() {
        if(!isSleep && canAssignRecipe() && getMachine().getOffsetTimer() % 10 == 0) {
            if(completedFuture == null) {
                completedFuture = supplyAsyncSearchTask();
            } else if(completedFuture.isDone()) {
                var lastFuture = completedFuture;
                completedFuture = null;
                boolean assignedAny = false;
                if(!lastFuture.isCancelled()) {
                    try {
                        var matches = lastFuture.join().stream().toList();
                        if(!matches.isEmpty()) {
                            for(var r : matches) {
                                for(var t : threads) {
                                    if(t.isWorking()) continue;
                                    assignedAny = assignRecipe(r, t);
                                    break;
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        completedFuture = supplyAsyncSearchTask();
                    }
                } else {
                    System.out.println("search recipe sync");
                    for(var r : searchRecipe()) {
                        for(var t : threads) {
                            if(t.isWorking()) continue;
                            assignedAny = assignRecipe(r, t);
                            break;
                        }
                    }
                }
                if(!assignedAny) {
                    failure();
                } else {
                    success();
                }
            }
        }
        for(var t : threads)
            t.serverTick();
    }

    protected void onLogicComplete(RecipeLogic logic, FSRecipe originalRecipe) {
        freeParallels += logic.getParallel();
        availableThread++;
        if(!assignRecipe(originalRecipe, logic)) {
            logic.setParallel(0);
            logic.setStatus(RecipeLogic.Status.IDLE);
        }
    }

    private boolean assignRecipe(FSRecipe recipe, RecipeLogic logic) {
        if(recipe != null && recipe.matchRecipe(machine).isSuccess()
                        && recipe.matchTickRecipe(machine).isSuccess()) {
            var modified = machine.doModifyRecipe(recipe, freeParallels);
            if(modified != null) {
                if(modified.getFirst().matchRecipe(machine).isSuccess() &&
                    modified.getFirst().matchTickRecipe(machine).isSuccess()) {
                    logic.setupRecipe(modified);
                }
                if(logic.lastRecipe != null && logic.isWorking()) {
                    logic.lastOriginalRecipe = recipe;
                    freeParallels -= modified.getSecond();
                    availableThread--;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canAssignRecipe() {
        return availableThread > 0 && freeParallels > 0;
    }

    private List<FSRecipe> searchRecipe() {
        return machine.getRecipeType().searchRecipes(getRecipeManager(), machine);
    }

    private CompletableFuture<List<FSRecipe>> supplyAsyncSearchTask() {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("search recipe", this::searchRecipe), Util.backgroundExecutor());
    }

    @SuppressWarnings("DataFlowIssue")
    public RecipeManager getRecipeManager() {
        return Platform.getMinecraftServer().getRecipeManager();
    }

    public void setLogicsStatus(boolean formed) {
        for(var t : threads) t.setStatus(formed);
    }

    public ISubscription addWakeupListener(Runnable runnable) {
        wakeupListener.add(runnable);
        return () -> wakeupListener.remove(runnable);
    }

    public void wakeUp() {
        if(isSleep) {
            isSleep = false;
            failures = 0;
            wakeupListener.forEach(Runnable::run);
        }
    }

    private void failure() {
        failures++;
        if(failures >= MAX_FAILURE) {
            sleep();
        }
    }

    private void success() {
        failures = 0;
        if(isSleep) {
            isSleep = false;
        }
    }

    private void sleep() {
        isSleep = true;
    }
}
