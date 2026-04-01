package org.moshang.fantasystructure.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ShaderLoader {
    private static final Map<ResourceLocation, ShaderInstance> shaders = new HashMap<>();

    private ShaderLoader() {}

    @Nullable
    public static ShaderInstance getShader(String name) {
        return shaders.get(FantasyStructure.id(name));
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void loadShaders(ResourceLocation shaderFile, VertexFormat format, ResourceProvider provider) {
        try {
            shaders.put(shaderFile, new ShaderInstance(provider, shaderFile, format));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadShaders(String shaderFile, ResourceProvider provider) {
        loadShaders(
                FantasyStructure.id(shaderFile),
                DefaultVertexFormat.POSITION_COLOR_TEX,
                provider
        );
    }

    public void close() {
        if(!shaders.isEmpty()) {
            shaders.values().forEach(ShaderInstance::close);
        }
    }
}
