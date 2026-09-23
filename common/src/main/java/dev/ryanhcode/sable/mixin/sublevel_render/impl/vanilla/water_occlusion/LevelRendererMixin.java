package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla.water_occlusion;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.SableClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;crumblingBufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;", ordinal = 2, shift = At.Shift.AFTER))
    public void sable$preRenderSectionLayers(final PoseStack poseStack, final float partialTick, final long finishNanoTime, final boolean renderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f projectionMatrix, final CallbackInfo ci) {
        // On 1.20.1 the view rotation lives in the level pose stack; the RenderSystem model view matrix is still identity here
        SableClient.WATER_OCCLUSION_RENDERER.preRenderTranslucent(new Matrix4f(poseStack.last().pose()), RenderSystem.getProjectionMatrix());
    }

    @Inject(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V", shift = At.Shift.BEFORE))
    private void sable$onRenderSectionLayer(final RenderType renderType, final PoseStack poseStack, final double d, final double e, final double f, final Matrix4f matrix4f, final CallbackInfo ci, @Local final ShaderInstance shader) {
        if (renderType == RenderType.translucent()) {
            SableClient.WATER_OCCLUSION_RENDERER.setupTranslucentShader(shader);
        }
    }

}
