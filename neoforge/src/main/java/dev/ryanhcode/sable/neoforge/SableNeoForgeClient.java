package dev.ryanhcode.sable.neoforge;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.FlywheelCompatNeoForge;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Client-side initialization, called from {@link SableNeoForge} on the physical client.
 */
public final class SableNeoForgeClient {

    private SableNeoForgeClient() {
    }

    static void init(final IEventBus modBus, final ModLoadingContext modContext) {
        final IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        SableClient.init();

        modContext.registerConfig(ModConfig.Type.CLIENT, SableClientConfig.SPEC);
        modBus.<ModConfigEvent.Loading>addListener(event -> {
            if (event.getConfig().getSpec() == SableClientConfig.SPEC) {
                SableClientConfig.onUpdate(false);
            }
        });
        modBus.<ModConfigEvent.Reloading>addListener(event -> {
            if (event.getConfig().getSpec() == SableClientConfig.SPEC) {
                SableClientConfig.onUpdate(true);
            }
        });
        forgeBus.<ClientPlayerNetworkEvent.LoggingOut>addListener(event -> {
            if (event.getPlayer() != null) { // LoggingOut may fire when logging in
                FloatingBlockMaterialDataHandler.clearMaterials();
            }
        });
        modBus.<RegisterClientReloadListenersEvent>addListener(event -> event.registerReloadListener((arg, arg2, arg3, arg4, executor, executor2) -> SubLevelRenderDispatcher.get().reload(arg, arg2, arg3, arg4, executor, executor2)));

        if (FlywheelCompatNeoForge.FLYWHEEL_LOADED) {
            Sable.LOGGER.warn("NOTE: Sable is loaded with Flywheel. Sable contains extensive shader overrides and a full light-storage replacement. Expect this to cause compatibility issues. If issues arise, please report them to the Sable issue tracker ({}) instead of the Flywheel issue tracker.", Sable.ISSUE_TRACKER_URL);
        }
    }
}
