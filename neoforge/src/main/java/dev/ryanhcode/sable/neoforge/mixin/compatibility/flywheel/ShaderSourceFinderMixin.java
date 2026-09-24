package dev.ryanhcode.sable.neoforge.mixin.compatibility.flywheel;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.SableFlywheelShaderOverrides;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

/**
 * Loads Sable's Flywheel shader overrides consistently, see {@link SableFlywheelShaderOverrides}.
 */
@Mixin(targets = "dev.engine_room.flywheel.backend.glsl.ShaderSources$SourceFinder", remap = false)
public class ShaderSourceFinderMixin {

    @ModifyVariable(method = "rootLoad", at = @At("HEAD"), argsOnly = true)
    private Resource sable$preferRootOverride(final Resource resource, final ResourceLocation location, final Resource originalResource) {
        return SableFlywheelShaderOverrides.prefer(location, resource);
    }

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ResourceManager;getResource(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", remap = true))
    private Optional<Resource> sable$preferIncludeOverride(final ResourceManager manager, final ResourceLocation location, final Operation<Optional<Resource>> original) {
        return original.call(manager, location).map(resource -> SableFlywheelShaderOverrides.prefer(location, resource));
    }
}
