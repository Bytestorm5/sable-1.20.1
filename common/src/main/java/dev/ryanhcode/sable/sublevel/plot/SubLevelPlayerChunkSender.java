package dev.ryanhcode.sable.sublevel.plot;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.function.Consumer;

public class SubLevelPlayerChunkSender {

    /**
     * A version of the chunk sending in {@code ChunkMap#playerLoadedChunk} that uses the plots light engine.
     * On 1.20.1 chunks are sent directly as {@link ClientboundLevelChunkWithLightPacket}s, without chunk batching.
     */
    public static void sendChunk(final Consumer<Packet<? super ClientGamePacketListener>> listener, final LevelLightEngine lightEngine, final LevelChunk chunk) {
        listener.accept(new ClientboundLevelChunkWithLightPacket(chunk, lightEngine, null, null));
    }

    /**
     * Sends the debug POI data of a plot chunk, as {@code ChunkMap#playerLoadedChunk} does for normal chunks.
     */
    public static void sendChunkPoiData(final ServerLevel level, final LevelChunk chunk) {
        final ChunkPos chunkPos = chunk.getPos();
        DebugPackets.sendPoiPacketsForChunk(level, chunkPos);
    }

}
