package org.moshang.fantasystructure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import org.moshang.fantasystructure.api.blockentity.BlockEntityAbstractController;
import org.moshang.fantasystructure.data.save.StructureWorldSavedData;
import org.moshang.fantasystructure.helper.blueprint.BlueprintEditor;
import org.moshang.fantasystructure.helper.blueprint.BlueprintManager;
import org.moshang.fantasystructure.registry.recipe.FSRecipes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Command {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fantasystructure")
                .then(Commands.literal("export")
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                .then(Commands.argument("registryName", StringArgumentType.string())
                                        .executes(ctx -> executeExport(ctx, StringArgumentType.getString(ctx, "registryName"))))))));

        dispatcher.register(Commands.literal("fantasystructure")
                .then(Commands.literal("reload")
                        .executes(Command::executeReload)));

        // Just for debug
        dispatcher.register(Commands.literal("fantasystructure")
                .then(Commands.literal("getRecipeType")
                        .executes(ctx -> {
                            var source = ctx.getSource();
                            for(var entry : FSRecipes.RECIPE_TYPES.entrySet()) {
                                source.sendSuccess(() -> Component.literal("recipe type: " + entry.getValue().toString()), false);
                            }
                            return 1;
                        })));
    }

    private static int executeExport(CommandContext<CommandSourceStack> ctx, String registryName)
            throws CommandSyntaxException {
        var source = ctx.getSource();

        BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(ctx, "pos1");
        BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(ctx, "pos2");

        Path blueprintDir = FMLPaths.CONFIGDIR.get()
                .resolve("fantasystructure")
                .resolve("blueprints");

        try {
            Files.createDirectories(blueprintDir);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to create blueprint directory: " + e.getMessage()));
            return 0;
        }

        Path outputFile = blueprintDir.resolve(registryName + ".json");
        BlueprintEditor.export(
                source.getLevel(), pos1, pos2,
                registryName, registryName, outputFile,
                (success, message) -> source.getServer().execute(() -> {
                    if(success) {
                        source.sendSuccess(() -> Component.literal(
                                "§aComplete exporting！\n" +
                                        "§7Registry Name: §f" + registryName + "\n" +
                                        "§7Saved Position: §f" + outputFile.toAbsolutePath()
                        ), false);
                    } else {
                        source.sendFailure(Component.literal("§cExport failed: " + message));
                    }
                })
        );
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        BlueprintManager.reload(FMLPaths.CONFIGDIR.get(), (success, message) -> source.getServer().execute(() -> {
            if(success) {
                source.sendSuccess(() -> Component.literal("§aReload Success"), false);
                var serverLevel = source.getLevel();
                var controllers = StructureWorldSavedData.getOrCreate(serverLevel).getControllers();
                if(!controllers.isEmpty()) {
                    for(var pos : controllers) {
                        if(serverLevel.isLoaded(pos)) {
                            var be = serverLevel.getBlockEntity(pos);
                            if(be instanceof BlockEntityAbstractController controller) {
                                controller.reload();
                            }
                        }
                    }
                }
            } else {
                source.sendFailure(Component.literal("Reload failed: " + message));
            }
        }));
        return 1;
    }
}
