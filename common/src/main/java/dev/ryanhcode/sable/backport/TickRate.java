package dev.ryanhcode.sable.backport;

/**
 * Stand-in for 1.20.3+'s {@code TickRateManager} / {@code ServerTickRateManager} ({@code /tick} command).
 * <p>
 * Minecraft 1.20.1 has no tick-rate manager: the game always runs at 20 ticks per second, is never frozen and
 * never steps. Code that queried {@code level.tickRateManager()} or {@code server.tickRateManager()} on 1.21 calls
 * these helpers instead, which report that fixed 1.20.1 behaviour.
 */
public final class TickRate {

    /**
     * The fixed tick rate of 1.20.1, in ticks per second
     */
    public static final float TICK_RATE = 20.0f;

    /**
     * The fixed length of a tick on 1.20.1, in milliseconds
     */
    public static final long MILLISECONDS_PER_TICK = 50L;

    private TickRate() {
    }

    /**
     * @return the tick rate in ticks per second (equivalent of {@code TickRateManager#tickrate()})
     */
    public static float tickrate() {
        return TICK_RATE;
    }

    /**
     * @return the length of a tick in milliseconds (equivalent of {@code TickRateManager#millisecondsPerTick()})
     */
    public static long millisecondsPerTick() {
        return MILLISECONDS_PER_TICK;
    }

    /**
     * @return if the game ticks normally (equivalent of {@code TickRateManager#runsNormally()}); always true on 1.20.1
     */
    public static boolean runsNormally() {
        return true;
    }

    /**
     * @return if the game is frozen (equivalent of {@code TickRateManager#isFrozen()}); always false on 1.20.1
     */
    public static boolean isFrozen() {
        return false;
    }

    /**
     * @return if the game is stepping while frozen (equivalent of {@code TickRateManager#isSteppingForward()}); always false on 1.20.1
     */
    public static boolean isSteppingForward() {
        return false;
    }
}
