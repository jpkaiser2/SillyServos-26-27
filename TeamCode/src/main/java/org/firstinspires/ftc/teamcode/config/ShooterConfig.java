package org.firstinspires.ftc.teamcode.config;

import com.qualcomm.robotcore.hardware.DcMotorSimple;

/** Measurements are meters/radians, except the explicitly named RPM and encoder fields. */
public final class ShooterConfig {
    // TODO: Replace these EXAMPLE measurements for EACH shooter before shooting.
    // not measurements of our robot. k starts at 1; measure it separately for each ball.
    public static final ShooterConfig POLLEN = new ShooterConfig(
            HardwareNames.FLYWHEEL_P, HardwareNames.HOOD_P);
    public static final ShooterConfig NECTAR = new ShooterConfig(
            HardwareNames.FLYWHEEL_N, HardwareNames.HOOD_N);

    static {
        // Replace/add measurements here independently for each shooter.
        // Same as k value in SillyServosShooterMath, but measured on the robot for each ball.
        POLLEN.rpmCorrection = 1.0;
        NECTAR.rpmCorrection = 1.0;
    }

    public final String flywheelName;
    public final String hoodName;
    public DcMotorSimple.Direction motorDirection = DcMotorSimple.Direction.FORWARD;
    public double targetX = 2.0;
    public double targetY = 0.0;
    public double targetHeight = 1.0;
    public double launchHeight = 0.5;
    public double forwardOffset = 0.0;
    public double leftOffset = 0.0;
    public double headingOffset = 0.0;
    public double wheelDiameter = 0.096;
    public double rpmCorrection = 1.0;
    // Wheel RPM / motor output-shaft RPM. 2 means the wheel turns twice per motor turn.
    public double wheelToMotorRatio = 1.0;
    // Encoder ticks per MOTOR OUTPUT SHAFT revolution, including any internal gearbox.
    public double ticksPerMotorRevolution = 28.0;
    public double maxWheelRpm = 5000.0;

    // Calibrate two actual launch angles and their servo positions
    public double minAngle = Math.toRadians(20);
    public double maxAngle = Math.toRadians(70);
    public double servoAtMinAngle = 0.2;
    public double servoAtMaxAngle = 0.8;

    public ShooterConfig(String flywheelName, String hoodName) {
        this.flywheelName = flywheelName;
        this.hoodName = hoodName;
    }
}
