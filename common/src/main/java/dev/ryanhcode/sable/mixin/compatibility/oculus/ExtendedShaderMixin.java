package dev.ryanhcode.sable.mixin.compatibility.oculus;

import com.mojang.blaze3d.shaders.Uniform;
import dev.ryanhcode.sable.mixinterface.compatibility.iris.ExtendedShaderExtension;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Iris compatibility. On Forge 1.20.1 Iris is provided by Oculus (mod id {@code oculus}, still in the
 * {@code net.irisshaders.iris} package), so this lives in {@code compatibility.oculus} for the mixin plugin's mod id check.
 */
@Mixin(value = ExtendedShader.class, remap = false)
public class ExtendedShaderMixin implements ExtendedShaderExtension {

    @Shadow
    @Final
    private Uniform modelViewInverse;

    @Shadow
    @Final
    private Uniform normalMatrix;

    // Not final in Oculus 1.8.0
    @Shadow
    private Matrix4f tempMatrix4f;

    @Shadow
    private Matrix3f tempMatrix3f;

    @Shadow
    private float[] tempFloats;

    @Shadow
    private float[] tempFloats2;

    @Unique
    @Override
    public void sable$refreshModelMatrices() {
        final var modelView = ((ShaderInstance) (Object) this).MODEL_VIEW_MATRIX;

        if (modelView != null) {
            if (this.modelViewInverse != null) {
                this.modelViewInverse.set(this.tempMatrix4f.set(modelView.getFloatBuffer()).invert().get(this.tempFloats));
                this.modelViewInverse.upload();
            }

            if (this.normalMatrix != null) {
                this.normalMatrix.set(this.tempMatrix3f.set(this.tempMatrix4f.set(modelView.getFloatBuffer())).invert().transpose().get(this.tempFloats2));
                this.normalMatrix.upload();
            }
        }
    }
}
