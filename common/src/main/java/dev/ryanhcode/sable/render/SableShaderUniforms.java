package dev.ryanhcode.sable.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;

/**
 * Shader uniform helpers that 1.20.1 is missing.
 */
@ApiStatus.Internal
public final class SableShaderUniforms {

    private SableShaderUniforms() {
    }

    /**
     * Uploads the default vanilla uniforms to the specified shader. This is a backport of 1.21's
     * {@code ShaderInstance#setDefaultUniforms}, matching the uniform setup 1.20.1 does inline in
     * {@code LevelRenderer#renderChunkLayer} and {@code VertexBuffer#drawWithShader}.
     *
     * @param shader     The shader to set the uniforms of
     * @param mode       The vertex mode about to be drawn
     * @param modelView  The model view matrix
     * @param projection The projection matrix
     * @param window     The game window
     */
    public static void setDefaultUniforms(final ShaderInstance shader, final VertexFormat.Mode mode, final Matrix4f modelView, final Matrix4f projection, final Window window) {
        for (int i = 0; i < 12; i++) {
            final int texture = RenderSystem.getShaderTexture(i);
            shader.setSampler("Sampler" + i, texture);
        }

        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(modelView);
        }

        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(projection);
        }

        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (shader.GLINT_ALPHA != null) {
            shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        }

        if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }

        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }

        if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }

        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        if (shader.SCREEN_SIZE != null) {
            shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }

        if (shader.LINE_WIDTH != null && (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP)) {
            shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }

        RenderSystem.setupShaderLights(shader);
    }
}
