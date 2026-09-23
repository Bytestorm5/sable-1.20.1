package dev.ryanhcode.sable.backport.client;

/**
 * Frame timing, in the shape of 1.21's {@code DeltaTracker}. On 1.20.1 the partial tick is passed around as a float,
 * so this wraps that value.
 */
public interface DeltaTracker {

    float getGameTimeDeltaTicks();

    float getGameTimeDeltaPartialTick(boolean runsNormally);

    float getRealtimeDeltaTicks();

    /**
     * @param partialTick the partial tick 1.20.1 passes to the render method
     */
    static DeltaTracker of(final float partialTick) {
        return new DeltaTracker() {
            @Override
            public float getGameTimeDeltaTicks() {
                return net.minecraft.client.Minecraft.getInstance().getDeltaFrameTime();
            }

            @Override
            public float getGameTimeDeltaPartialTick(final boolean runsNormally) {
                return partialTick;
            }

            @Override
            public float getRealtimeDeltaTicks() {
                return net.minecraft.client.Minecraft.getInstance().getDeltaFrameTime();
            }
        };
    }
}
