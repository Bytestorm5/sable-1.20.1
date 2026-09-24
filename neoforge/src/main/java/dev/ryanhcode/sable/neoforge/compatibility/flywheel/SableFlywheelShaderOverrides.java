package dev.ryanhcode.sable.neoforge.compatibility.flywheel;

import dev.ryanhcode.sable.Sable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Makes Flywheel load Sable's shader overrides (see {@code assets/flywheel/explanation.md}) instead of its own copies.
 * <p>
 * On Forge 1.20.1 all mod jars share one {@code mod_resources} pack, which answers a single-file lookup with the first
 * mod that has the file but a listing with the last one. Flywheel's source finder mixes the two: root shaders come from
 * the listing (Sable's copy), while {@code #include}d files that are reached first are looked up directly (Flywheel's
 * copy). Sable's overridden {@code .glsl} includes were therefore silently replaced by Flywheel's.
 */
public final class SableFlywheelShaderOverrides {

    private static final String MOD_RESOURCES_PACK_ID = "mod_resources";

    private SableFlywheelShaderOverrides() {
    }

    /**
     * @param location the full resource location of the shader, e.g. {@code flywheel:flywheel/internal/api_impl.glsl}
     * @param resource the resource the resource manager resolved
     * @return Sable's copy if the resource came from a mod jar and Sable ships an override for it, otherwise {@code resource}
     */
    public static Resource prefer(final ResourceLocation location, final Resource resource) {
        // Leave resource packs alone, they are allowed to override Sable too
        if (!MOD_RESOURCES_PACK_ID.equals(resource.sourcePackId())) {
            return resource;
        }

        final IModFileInfo sableFile = ModList.get().getModFileById(Sable.MOD_ID);
        if (sableFile == null) {
            return resource;
        }

        final Path path = sableFile.getFile().findResource("assets", location.getNamespace(), location.getPath());
        if (!Files.isRegularFile(path)) {
            return resource;
        }

        return new Resource(resource.source(), () -> Files.newInputStream(path));
    }
}
