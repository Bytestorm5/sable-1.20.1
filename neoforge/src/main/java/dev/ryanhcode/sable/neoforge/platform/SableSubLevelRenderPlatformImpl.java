package dev.ryanhcode.sable.neoforge.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.FlywheelCompatNeoForge;
import dev.ryanhcode.sable.platform.SableSubLevelRenderPlatform;
import dev.ryanhcode.sable.sublevel.render.vanilla.SingleBlockSubLevelWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelDataManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@ApiStatus.Internal
public class SableSubLevelRenderPlatformImpl implements SableSubLevelRenderPlatform {

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void tesselateBlock(final SingleBlockSubLevelWrapper blockAndTintGetter, final BakedModel bakedModel, final BlockState blockState, final BlockPos pos, final PoseStack poseStack, final VertexConsumer vertexConsumer, final RandomSource randomSource, final long seed, final int packedOverlay, final @Nullable RenderType renderType) {
        Minecraft.getInstance().getBlockRenderer().modelRenderer.tesselateWithoutAO(blockAndTintGetter, bakedModel, blockState, pos, poseStack, vertexConsumer, true, randomSource, seed, packedOverlay, getModelData(blockAndTintGetter.getLevel(), pos), renderType);
    }

    @Override
    public List<RenderType> getRenderLayers(final SingleBlockSubLevelWrapper blockAndTintGetter, final BakedModel bakedModel, final BlockState blockState, final BlockPos pos, final RandomSource randomSource) {
        return bakedModel.getRenderTypes(blockState, randomSource, getModelData(blockAndTintGetter.getLevel(), pos)).asList();
    }

    /**
     * Forge 1.20.1 has no {@code BlockAndTintGetter#getModelData}; model data is looked up through the level's
     * {@link ModelDataManager} instead.
     */
    private static ModelData getModelData(final @Nullable ClientLevel level, final BlockPos pos) {
        final ModelDataManager manager = level != null ? level.getModelDataManager() : null;
        final ModelData data = manager != null ? manager.getAt(pos) : null;
        return data != null ? data : ModelData.EMPTY;
    }

    @Override
    public void tryAddFlywheelVisual(final BlockEntity blockEntity) {
        if (FlywheelCompatNeoForge.FLYWHEEL_LOADED) {
            FlywheelCompatNeoForge.tryAddVisual(blockEntity);
        }
    }
}
