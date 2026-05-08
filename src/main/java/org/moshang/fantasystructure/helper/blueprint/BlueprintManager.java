package org.moshang.fantasystructure.helper.blueprint;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.moshang.fantasystructure.Config;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

@SuppressWarnings({"removal", "CallToPrintStackTrace"})
public class BlueprintManager {
    private static final Map<ResourceLocation, Blueprint> REGISTRY = new ConcurrentHashMap<>();

    private static ExecutorService LOADING_THREAD_POOL;
    private static volatile boolean initialized = false;

    private static int totalFiles = 0;
    @Getter
    private static int loadedCounts = 0;
    @Getter
    private static int skippedCounts = 0;

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init(Path configDir) {
        if(LOADING_THREAD_POOL != null) return;
        int threadCount = Math.min(Config.MAX_PROCESSOR.get(), Runtime.getRuntime().availableProcessors());
        LOADING_THREAD_POOL = Executors.newFixedThreadPool(Math.max(1, threadCount));

        if(initialized) return;

        try {
            var stat = loadBlueprints(configDir, REGISTRY);
            totalFiles = stat.totalFiles;
            loadedCounts = stat.loaded;
            skippedCounts = stat.skipped;
            LOGGER.info("Loaded {} blueprints, skipped {} blueprints, total {} blueprints", loadedCounts, skippedCounts, totalFiles);
        } catch (Exception ex) {
            LOGGER.error("Failed to load blueprints", ex);
        }
        initialized = true;
    }

    public static void reload(Path configDir, BiConsumer<Boolean, String> callback) {
        LOADING_THREAD_POOL.submit(() -> {
            try {
                Map<ResourceLocation, Blueprint> newRegistry = new ConcurrentHashMap<>();
                var result = loadBlueprints(configDir, newRegistry);

                synchronized (BlueprintManager.class) {
                    REGISTRY.clear();
                    REGISTRY.putAll(newRegistry);
                    totalFiles = result.totalFiles;
                    loadedCounts = result.loaded;
                    skippedCounts = result.skipped;
                }
                callback.accept(true, null);
            } catch (IOException e) {
                callback.accept(false, e.getMessage());
            }
        });
    }

    private static LoadStat loadBlueprints(Path configDir, Map<ResourceLocation, Blueprint> registry) throws IOException {
        int newLoaded = 0;
        int newSkipped = 0;
        int newTotal = 0;

        Path blueprintDir = configDir.resolve("fantasystructure/blueprints");
        try {
            Files.createDirectories(blueprintDir);

            List<Path> files = new ArrayList<>();
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(blueprintDir, "*.json")) {
                stream.forEach(files::add);
            }
            newTotal = files.size();

            ExecutorCompletionService<LoadResult> completionService = new ExecutorCompletionService<>(LOADING_THREAD_POOL);
            for(Path file : files) {
                completionService.submit(() -> loadBlueprintInternal(file));
            }

            for(int i = 0; i < newTotal; ++i) {
                try {
                    LoadResult result = completionService.take().get();
                    if(result.success) {
                        registry.put(result.id, result.blueprint);
                        newLoaded++;
                    } else {
                        newSkipped++;
                    }
                } catch (Exception e) {
                    newSkipped++;
                    LOGGER.error("Blueprint loading execution error", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new LoadStat(newTotal, newLoaded, newSkipped);
    }


    private static LoadResult loadBlueprintInternal(Path file) {
        try {
            String name = file.getFileName().toString().replace(".json", "");
            ResourceLocation id = new ResourceLocation(FantasyStructure.MODID, name);

            Blueprint blueprint = Blueprint.fromJson(file);
            return new LoadResult(id, blueprint, null);
        } catch (Blueprint.BlueprintLoadException e) {
            e.printStackTrace();
            return new LoadResult(null, null, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new LoadResult(null, null, "IO Error: " + e.getMessage());
        }
    }

    public static Optional<Blueprint> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static StructurePattern getPattern(ResourceLocation id, Direction facing) {
        return get(id).map(blueprint -> blueprint.toStructurePattern(facing)).orElse(null);
    }

    public static StructurePattern getPattern(ResourceLocation id) {
        return getPattern(id, Direction.NORTH);
    }

    public static Map<List<Item>, Integer> getMaterial(ResourceLocation id) {
        return get(id).map(Blueprint::getMaterialMap).orElse(null);
    }

    public static Collection<Blueprint> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
    public static List<ResourceLocation> getAvailableBlueprintIds() {
        return new ArrayList<>(REGISTRY.keySet());
    }

    private static class LoadResult {
        final ResourceLocation id;
        final Blueprint blueprint;
        final String error;
        final boolean success;

        LoadResult(ResourceLocation id, Blueprint blueprint, String error) {
            this.id = id;
            this.blueprint = blueprint;
            this.error = error;
            this.success = blueprint != null;
        }
    }

    private record LoadStat(int totalFiles, int loaded, int skipped) { }
}
