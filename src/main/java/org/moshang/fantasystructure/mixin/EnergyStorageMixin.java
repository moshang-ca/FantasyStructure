package org.moshang.fantasystructure.mixin;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import net.minecraft.nbt.Tag;
import net.minecraftforge.energy.EnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnergyStorage.class, remap = false)
public abstract class EnergyStorageMixin implements ITagSerializable<Tag>, IContentChangeAware {
    @Shadow public abstract Tag serializeNBT();
    @Shadow public abstract void deserializeNBT(Tag tag);

    @Unique Runnable fantasystructure$onContentsChanged = () -> {};

    @Override
    public void setOnContentsChanged(Runnable onContentChanged) {
        this.fantasystructure$onContentsChanged = onContentChanged;
    }

    @Override
    public Runnable getOnContentsChanged() {
        return fantasystructure$onContentsChanged;
    }

    @Inject(method = "receiveEnergy", at = @At("RETURN"))
    public void onReceiveEnergy(int maxReceive, boolean simulate, CallbackInfoReturnable<Integer> cir) {
        if(!simulate && cir.getReturnValue() > 0) {
            fantasystructure$onContentsChanged.run();
        }
    }

    @Inject(method = "extractEnergy", at = @At("RETURN"))
    public void onExtractEnergy(int maxExtract, boolean simulate, CallbackInfoReturnable<Integer> cir) {
        if(!simulate && cir.getReturnValue() > 0) {
            fantasystructure$onContentsChanged.run();
        }
    }
}
