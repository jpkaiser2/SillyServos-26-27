package org.firstinspires.ftc.teamcode.config;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

//Tunable constants shared across subsystems and opmodes.

public final class RobotConstants {
    private RobotConstants() {
    }

    public static final double MAX_DRIVE_POWER = 1.0;
    public static final double MAX_TURN_POWER = 1.0;

    public static final boolean FIELD_CENTRIC_TELEOP = true;

    // TODO: Measure pod offsets (inches) and verify pod type/directions on the robot.
    public static final double FORWARD_POD_Y_INCHES = 0.0;
    public static final double STRAFE_POD_X_INCHES = 0.0;
    public static final GoBildaPinpointDriver.GoBildaOdometryPods ODOMETRY_PODS =
            GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD;
    public static final GoBildaPinpointDriver.EncoderDirection FORWARD_ENCODER_DIRECTION =
            GoBildaPinpointDriver.EncoderDirection.REVERSED;
    public static final GoBildaPinpointDriver.EncoderDirection STRAFE_ENCODER_DIRECTION =
            GoBildaPinpointDriver.EncoderDirection.FORWARD;
    public static final DcMotorSimple.Direction LEFT_DRIVE_DIRECTION = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction RIGHT_DRIVE_DIRECTION = DcMotorSimple.Direction.FORWARD;

    // Starting pose must use the same field frame as the targets in ShooterConfig.
    public static final double START_X_INCHES = 0.0;
    public static final double START_Y_INCHES = 0.0;
    public static final double START_HEADING_RADIANS = 0.0;
    public static final double AIM_KP = 1.0; // Turn power per radian; tune on the robot.
    public static final double MAX_AIM_POWER = 0.35;
    public static final double AIM_TOLERANCE_RADIANS = Math.toRadians(2);

    public static final double AUTO_FORWARD_POWER = 0.35;
    public static final double AUTO_FORWARD_SECONDS = 1.5;
}
