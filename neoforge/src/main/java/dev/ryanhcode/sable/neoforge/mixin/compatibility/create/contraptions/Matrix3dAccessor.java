package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.contraptions;

import com.simibubi.create.foundation.collision.Matrix3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Matrix3d.class)
public interface Matrix3dAccessor {
    @Accessor(value = "m00", remap = false)
    double getM00();

    @Accessor(value = "m00", remap = false)
    void setM00(double value);

    @Accessor(value = "m01", remap = false)
    double getM01();

    @Accessor(value = "m01", remap = false)
    void setM01(double value);

    @Accessor(value = "m02", remap = false)
    double getM02();

    @Accessor(value = "m02", remap = false)
    void setM02(double value);

    @Accessor(value = "m10", remap = false)
    double getM10();

    @Accessor(value = "m10", remap = false)
    void setM10(double value);

    @Accessor(value = "m11", remap = false)
    double getM11();

    @Accessor(value = "m11", remap = false)
    void setM11(double value);

    @Accessor(value = "m12", remap = false)
    double getM12();

    @Accessor(value = "m12", remap = false)
    void setM12(double value);

    @Accessor(value = "m20", remap = false)
    double getM20();

    @Accessor(value = "m20", remap = false)
    void setM20(double value);

    @Accessor(value = "m21", remap = false)
    double getM21();

    @Accessor(value = "m21", remap = false)
    void setM21(double value);

    @Accessor(value = "m22", remap = false)
    double getM22();

    @Accessor(value = "m22", remap = false)
    void setM22(double value);

}
