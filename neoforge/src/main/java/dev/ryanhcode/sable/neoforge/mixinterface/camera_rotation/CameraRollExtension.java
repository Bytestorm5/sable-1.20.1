package dev.ryanhcode.sable.neoforge.mixinterface.camera_rotation;

/**
 * On Forge 1.20.1 the view matrix is built from the camera's yaw, pitch and the {@code ComputeCameraAngles} roll rather
 * than from its rotation quaternion, so the roll of a sub-level the camera entity is riding is kept here.
 */
public interface CameraRollExtension {

    /**
     * @return the roll in degrees from the sub-level the camera entity is riding, or 0
     */
    float sable$getSubLevelRoll();
}
