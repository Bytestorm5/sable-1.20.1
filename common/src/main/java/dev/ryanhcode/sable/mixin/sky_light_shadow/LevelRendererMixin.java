package dev.ryanhcode.sable.mixin.sky_light_shadow;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // TODO: neo dies
    // (Disabled upstream as well. The targets below are the 1.20.1 equivalents of the 1.21 ones: renderSectionLayer is
    // renderChunkLayer, SectionRenderDispatcher$CompiledSection is ChunkRenderDispatcher$CompiledChunk and Forge's
    // ParticleEngine#render takes the pose stack, buffer source and frustum.)
/*
    @Inject(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V", shift = At.Shift.AFTER))
    private void sable$onRenderSectionLayer(final RenderType renderType, final PoseStack poseStack, final double d, final double e, final double f, final Matrix4f projection, final CallbackInfo ci, @Local final ShaderInstance shader) {
        SableSkyLightShadows.bindShadowMapTexture(shader);
    }

    @WrapOperation(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$CompiledChunk;isEmpty(Lnet/minecraft/client/renderer/RenderType;)Z"))
    private boolean sable$wrapRenderSectionLayer(final ChunkRenderDispatcher.CompiledChunk instance, final RenderType renderType, final Operation<Boolean> original) {
        return SableSkyLightShadows.renderingShadowMap() || original.call(instance, renderType);
    }

    *//**
     * Don't render entities if we're rendering the shadow map
     *//*
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    private boolean sable$wrapRenderLevel(final EntityRenderDispatcher instance, final Entity entity, final Frustum frustum, final double d, final double e, final double f, final Operation<Boolean> original) {
        return !SableSkyLightShadows.renderingShadowMap() && original.call(instance, entity, frustum, d, e, f);
    }

    @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"))
    private boolean sable$wrapRenderParticles(final ParticleEngine instance, final PoseStack poseStack, final MultiBufferSource.BufferSource bufferSource, final LightTexture lightTexture, final Camera camera, final float f, final Frustum frustum) {
        return !SableSkyLightShadows.renderingShadowMap();
    }*/
}
