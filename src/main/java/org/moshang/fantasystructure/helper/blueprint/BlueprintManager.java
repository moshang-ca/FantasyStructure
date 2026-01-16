package org.moshang.fantasystructure.helper.blueprint;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.moshang.fantasystructure.Config;
import org.moshang.fantasystructure.FantasyStructure;
import org.moshang.fantasystructure.helper.StructurePattern;
import org.moshang.fantasystructure.developed.TestBlueprint;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SuppressWarnings("removal")
public class BlueprintManager {
    private static final Map<ResourceLocation, Blueprint> REGISTRY = new ConcurrentHashMap<>();

    private static ExecutorService LOADING_THREAD_POOL;
    private static volatile boolean initialized = false;

    private static int totalFiles = 0;
    private static int loadedCounts = 0;
    private static int skippedCounts = 0;

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init(Path configDir) {
        if(LOADING_THREAD_POOL != null) return;
        int threadCount = Math.min(Config.MAX_PROCESSOR.get(), Runtime.getRuntime().availableProcessors());
        LOADING_THREAD_POOL = Executors.newFixedThreadPool(Math.max(1, threadCount));

        if(initialized) return;

        Path blueprintDir = configDir.resolve("fantasystructure/blueprints");
        try {
            Files.createDirectories(blueprintDir);

            //This block is a test block
            if (!containsBlueprintFiles(blueprintDir)) {
                LOGGER.info("No blueprint files found, creating test blueprints...");
                TestBlueprint.createAllTestBlueprints(blueprintDir);
            }

            List<Path> files = new ArrayList<>();
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(blueprintDir, "*.fspb")) {
                stream.forEach(files::add);
            }

            totalFiles = files.size();

            List<Future<LoadResult>> futures = new ArrayList<>();
            for(Path file : files) {
                futures.add(LOADING_THREAD_POOL.submit(() -> loadBlueprint(file)));
            }

            for(Future<LoadResult> future : futures) {
                try {
                    LoadResult result = future.get();
                    if(result.success) {
                        REGISTRY.put(result.id, result.blueprint);
                        loadedCounts++;
                    } else {
                        skippedCounts++;
                    }
                } catch (Exception e) {
                    skippedCounts++;
                }
            }

            initialized = true;
            LOGGER.info("initialized blueprint manager with {} threads free", threadCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean containsBlueprintFiles(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return false;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.fspb")) {
            return stream.iterator().hasNext();
        }
    }

    private static LoadResult loadBlueprint(Path file) {
        try {
            String name = file.getFileName().toString().replace(".fspb", "");
            ResourceLocation id = new ResourceLocation(FantasyStructure.MODID, name);

            Blueprint blueprint = Blueprint.fromBinary(id, file);
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

    public static StructurePattern getPattern(ResourceLocation id) {
        return get(id).map(Blueprint::toStructurePattern).orElse(null);
    }

    public static Collection<Blueprint> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static List<ResourceLocation> getAvailableBlueprintIds() {
        return new ArrayList<>(REGISTRY.keySet());
    }

    public static int getLoadedCounts() {
        return loadedCounts;
    }
    public static int getSkippedCounts() {
        return skippedCounts;
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
}
