package dev.ryanhcode.sable;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SableServerConfig {

    public static final ForgeConfigSpec SPEC;

    public static final int SUB_LEVEL_SUBSTEPS_PER_TICK_MIN = 1;
    public static final int SUB_LEVEL_SUBSTEPS_PER_TICK_MAX = 10;
    public static final ForgeConfigSpec.IntValue SUB_LEVEL_SUBSTEPS_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue AERODYNAMIC_REFERENCE_SPEED;
    public static final ForgeConfigSpec.BooleanValue QUADRATIC_AERODYNAMICS;
    public static final ForgeConfigSpec.BooleanValue SUB_LEVEL_STORAGE_PRUNING;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        SUB_LEVEL_SUBSTEPS_PER_TICK = builder
                .comment("How many times the physics simulation is stepped in every second. Higher values will be significantly more performance intensive, but will have higher accuracy.")
                .defineInRange("sub_level_substeps_per_tick", 2, SUB_LEVEL_SUBSTEPS_PER_TICK_MIN, SUB_LEVEL_SUBSTEPS_PER_TICK_MAX);

        AERODYNAMIC_REFERENCE_SPEED = builder
                .comment("Airspeed at which sail lift/drag equal the old linear model, in m/s. Forces scale with (airspeed / this)². 1.0 treats the sail coefficients as physical ½·ρ·C·S values.")
                .defineInRange("aerodynamic_reference_speed", 1.0, 0.01, 1000.0);

        QUADRATIC_AERODYNAMICS = builder
                .comment("Whether sail lift and drag scale with the square of airspeed (see aerodynamic_reference_speed). false restores the previous linear model exactly.")
                .define("quadratic_aerodynamics", true);

        SUB_LEVEL_STORAGE_PRUNING = builder
                .comment("Prunes and deletes empty sub-level storage files. Experimental.")
                .define("sub_level_storage_pruning", false);

        SPEC = builder.build();
    }
}
