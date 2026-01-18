package org.moshang.fantasystructure;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FantasyStructure.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static ForgeConfigSpec.IntValue MAX_PROCESSOR;

    static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("FantasyStructure");

        builder.push("common");

        builder.comment("This defines how many processors will be used in loading and exporting task");
        builder.comment("Too large number won't take effect if it exceeds the maximum number of processors");
        builder.comment("e.g. 4 processors means 4 threads will be used in exporting task");
        builder.comment("and 6 threads in loading task");
        MAX_PROCESSOR = builder.defineInRange("maxProcessor", 2, 1, 64);

        SPEC = builder.build();
    }


}
