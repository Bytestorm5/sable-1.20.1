package dev.ryanhcode.sable.neoforge;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableCommonEvents;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.SableServerConfig;
import dev.ryanhcode.sable.command.SableCommand;
import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifiers;
import dev.ryanhcode.sable.index.SableAttributes;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinitionLoader;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.CrashReportCallables;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod(Sable.MOD_ID)
public final class SableNeoForge {
    public SableNeoForge() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        final ModLoadingContext modContext = ModLoadingContext.get();

        Sable.init();

        final IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::registerCommand);
        forgeBus.addListener(this::registerReloadListeners);
        forgeBus.addListener(this::syncDataPack);
        modBus.addListener(this::addPlayerAttributes);

        SubLevelSelectorModifiers.registerModifiers();

        final DeferredRegister<Attribute> attributes = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Sable.MOD_ID);
        SableAttributes.PUNCH_STRENGTH = attributes.register(SableAttributes.PUNCH_STRENGTH_NAME, () -> SableAttributes.PUNCH_STRENGTH_ATTRIBUTE);
        SableAttributes.PUNCH_COOLDOWN = attributes.register(SableAttributes.PUNCH_COOLDOWN_NAME, () -> SableAttributes.PUNCH_COOLDOWN_ATTRIBUTE);
        attributes.register(modBus);

        modContext.registerConfig(ModConfig.Type.COMMON, SableConfig.SPEC);
        modContext.registerConfig(ModConfig.Type.SERVER, SableServerConfig.SPEC);

        CrashReportCallables.registerCrashCallable("Sable", Sable::getCrashHeader);

        // Forge 1.20.1 has no client-only @Mod entrypoints
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SableNeoForgeClient.init(modBus, modContext);
        }
    }

    public void registerReloadListeners(final AddReloadListenerEvent event) {
        event.addListener(PhysicsBlockPropertiesDefinitionLoader.INSTANCE);
        event.addListener(DimensionPhysicsData.ReloadListener.INSTANCE);
        event.addListener(FloatingBlockMaterialDataHandler.ReloadListener.INSTANCE);
    }

    /**
     * Forge's way of adding attributes to an existing entity type (NeoForge Sable patched the default supplier in
     * common setup instead).
     */
    private void addPlayerAttributes(final EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, SableAttributes.PUNCH_STRENGTH.get());
        event.add(EntityType.PLAYER, SableAttributes.PUNCH_COOLDOWN.get());
    }

    private void registerCommand(final RegisterCommandsEvent event) {
        SableCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    private void syncDataPack(final OnDatapackSyncEvent event) {
        final List<ServerPlayer> players = event.getPlayer() != null ? List.of(event.getPlayer()) : event.getPlayerList().getPlayers();
        SableCommonEvents.syncDataPacket(packet -> players.forEach(player -> player.connection.send(packet)));
    }
}
