package dev.ryanhcode.sable.neoforge.mixin.compatibility.flywheel;

import dev.engine_room.flywheel.backend.engine.LightDataCollector;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightStorage.class)
public interface LightStorageAccessor {

    @Accessor(remap = false)
    LightDataCollector getCollector();

    @Accessor(remap = false)
    void setNeedsLutRebuild(boolean needsLutRebuild);

}
