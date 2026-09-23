package dev.ryanhcode.sable.neoforge.platform;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.platform.SablePlotPlatform;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkDataEvent;
import org.slf4j.Logger;

public class SablePlotPlatformImpl implements SablePlotPlatform {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * The key Forge's {@code ChunkSerializer} stores chunk capabilities under
     */
    private static final String FORGE_CAPS_NBT_KEY = "ForgeCaps";

    @Override
    public void readLightData(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        // Forge 1.20.1 has no auxiliary (per-chunk) light data; NeoForge's LevelChunkAuxiliaryLightManager does not exist yet
    }

    @Override
    @SuppressWarnings("deprecation")
    public void readChunkAttachments(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        // Forge 1.20.1 equivalent of NeoForge data attachments: chunk capabilities
        if (tag.contains(FORGE_CAPS_NBT_KEY, Tag.TAG_COMPOUND)) {
            chunk.readCapsFromNBT(tag.getCompound(FORGE_CAPS_NBT_KEY));
        }
    }

    @Override
    public void postLoad(final CompoundTag tag, final LevelChunk chunk) {
        MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk, tag, ChunkStatus.ChunkType.LEVELCHUNK));
    }

    @Override
    public void writeLightData(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        // Forge 1.20.1 has no auxiliary (per-chunk) light data; NeoForge's LevelChunkAuxiliaryLightManager does not exist yet
    }

    @Override
    @SuppressWarnings("deprecation")
    public void writeChunkAttachments(final CompoundTag tag, final RegistryAccess registryAccess, final LevelChunk chunk) {
        try {
            final CompoundTag capTag = chunk.writeCapsToNBT();
            if (capTag != null) {
                tag.put(FORGE_CAPS_NBT_KEY, capTag);
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to write chunk capabilities. A capability provider has likely thrown an exception trying to write state. It will not persist. Report this to the mod author", e);
        }
    }
}
