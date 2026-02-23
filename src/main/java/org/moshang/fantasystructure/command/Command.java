package org.moshang.fantasystructure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.moshang.fantasystructure.blockentity.container.BEEnergyBus;
import org.moshang.fantasystructure.helper.blueprint.BlueprintEditor;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Command {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("fantasystructure")
                        .then(Commands.literal("export")
                                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                    .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                        .then(Commands.argument("filename", StringArgumentType.string())
                                                .executes(Command::executeExport)))))
        );
        dispatcher.register(
                Commands.literal("fantasystructure")
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amounts", IntegerArgumentType.integer())
                                                .executes(commandContext -> {
                                                    BlockEntity be = commandContext.getSource().getLevel().getBlockEntity(BlockPosArgument.getLoadedBlockPos(commandContext, "pos1"));
                                                    if(be instanceof BEEnergyBus energyBusBase) {
                                                        energyBusBase.setEnergyStorageDebug(IntegerArgumentType.getInteger(commandContext, "amounts"));
                                                        return SINGLE_SUCCESS;
                                                    }
                                                    return 0;
                                                }))))
        );
    }

    private static int executeExport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Level level = source.getLevel();

        BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(context, "pos1");
        BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(context, "pos2");
        String filename = StringArgumentType.getString(context, "filename");
        if(filename.isEmpty()) {
            source.sendFailure(Component.translatable("invalid filename"));
            return 0;
        }

        if(!filename.endsWith(".fspb")) {
            filename += ".fspb";
        }

        Path configDir = source.getServer().getServerDirectory().toPath()
                .resolve("config")
                .resolve("fantasystructure")
                .resolve("blueprints");

        try {
            Files.createDirectories(configDir);

            Path outputFile = configDir.resolve(filename);

            if(Files.exists(outputFile)) {
                source.sendFailure(Component.translatable("file exists"));
                return 0;
            }

            int minX = Math.min(pos1.getX(), pos2.getX());
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());

            int sizeX = (maxX - minX) + 1;
            int sizeY = (maxY - minY) + 1;
            int sizeZ = (maxZ - minZ) + 1;

            if(sizeX > 256 || sizeY > 256 || sizeZ > 256) {
                source.sendFailure(Component.translatable("size too large, must smaller than 256"));
            }
            if(sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                source.sendFailure(Component.translatable("invalid size"));
            }

            int volume = sizeX * sizeY * sizeZ;

            source.sendSuccess(() -> Component.translatable("exporting..."), false);
            source.sendSuccess(() -> Component.literal(String.format(
                    "region: (%d,%d,%d) - (%d,%d,%d)",
                    minX, minY, minZ, maxX, maxY, maxZ
            )), false);
            source.sendSuccess(() -> Component.literal(String.format(
                    "size: %dx%dx%d (total: %d)",
                    sizeX, sizeY, sizeZ, volume
            )), false);

            boolean success = BlueprintEditor.exportRegionToBlueprint(
                    level, pos1, pos2, filename, outputFile
            );

            if(success) {
                source.sendSuccess(() -> Component.translatable("exported"), false);
                source.sendSuccess(() -> Component.translatable("file in:" + outputFile.toString()), false);

                return SINGLE_SUCCESS;
            } else {
                source.sendFailure(Component.translatable("export failed.may be no controller in region"));
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            source.sendFailure(Component.translatable("export error"));
            return 0;
        }
    }
}
