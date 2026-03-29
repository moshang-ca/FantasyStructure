package org.moshang.fantasystructure.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.moshang.fantasystructure.FantasyStructure;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public class ShaderLoader {
    private static ShaderLoader INSTANCE;

    @Nullable @Getter
    private ShaderInstance shader = null;

    private ShaderLoader() {}

    public static ShaderLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderLoader();
        }
        return INSTANCE;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void loadShaders(ResourceLocation shaderFile, VertexFormat format, ResourceProvider provider) {
        try {
            shader = new ShaderInstance(provider, shaderFile, format);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadShaders(String shaderFile, ResourceProvider provider) {
        loadShaders(
                FantasyStructure.id(shaderFile),
                DefaultVertexFormat.POSITION_COLOR_TEX,
                provider
        );
    }

    public void close() {
        if(shader != null) {
            shader.close();
        }
    }
}
