package dev.ryanhcode.sable.render.dynamic_shade;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;

/**
 * Make all shade-less things have a normal pointing straight up for dynamic shading!
 */
public class SubLevelVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private boolean verticalNormal;

    public SubLevelVertexConsumer(final VertexConsumer delegate) {
        this.delegate = delegate;
    }


    @Override
    public VertexConsumer vertex(final double x, final double y, final double z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(final int i, final int j, final int k, final int l) {
        this.delegate.color(i, j, k, l);
        return this;
    }

    @Override
    public VertexConsumer uv(final float f, final float g) {
        this.delegate.uv(f, g);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(final int i, final int j) {
        this.delegate.overlayCoords(i, j);
        return this;
    }

    @Override
    public VertexConsumer uv2(final int i, final int j) {
        this.delegate.uv2(i, j);
        return this;
    }

    @Override
    public VertexConsumer normal(final float pX, final float pY, final float pZ) {
        if (this.verticalNormal) {
            this.delegate.normal(0f, 1f, 0f);
        } else {
            this.delegate.normal(pX, pY, pZ);
        }
        return this;
    }

    @Override
    public void endVertex() {
        this.delegate.endVertex();
    }

    @Override
    public void defaultColor(final int r, final int g, final int b, final int a) {
        this.delegate.defaultColor(r, g, b, a);
    }

    @Override
    public void unsetDefaultColor() {
        this.delegate.unsetDefaultColor();
    }

    @Override
    public void putBulkData(final PoseStack.Pose pose, final BakedQuad bakedQuad, final float[] fs, final float f, final float g, final float h, final int[] is, final int j, final boolean bl) {
        this.verticalNormal = !bakedQuad.isShade();
        VertexConsumer.super.putBulkData(pose, bakedQuad, fs, f, g, h, is, j, bl);
        this.verticalNormal = false;
    }

}
