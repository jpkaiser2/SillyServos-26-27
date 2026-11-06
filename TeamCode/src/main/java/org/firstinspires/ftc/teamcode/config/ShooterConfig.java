package org.firstinspires.ftc.teamcode.config;

import com.qualcomm.robotcore.hardware.DcMotorSimple;

// Measurements are meters/radians, except the explicitly named RPM and encoder fields.
public final class ShooterConfig {
    // TODO: Replace these EXAMPLE measurements for EACH shooter before shooting.
    // not measurements of our robot. k starts at 1; measure it separately for each ball.
    public static final ShooterConfig POLLEN = new ShooterConfig(
            HardwareNames.FLYWHEEL_P, HardwareNames.HOOD_P, HardwareNames.GATE_P);
    public static final ShooterConfig NECTAR = new ShooterConfig(
            HardwareNames.FLYWHEEL_N, HardwareNames.HOOD_N, HardwareNames.GATE_N);

    static {
        // Replace/add measurements here independently for each shooter.
        // Same as k value in SillyServosShooterMath, but measured on the robot for each ball.
        POLLEN.rpmCorrection = 1.0;
        NECTAR.rpmCorrection = 1.0;
    }

    public final String flywheelName;
    public final String hoodName;
    public final String gateName;
    // EXAMPLE servo positions (0-1). Calibrate independently for pollen and nectar.
    // Closed blocks balls; open rotates the blocker clear of the ball path.
    public double gateClosedPosition = 0.0;
    public double gateOpenPosition = 0.5;
    public DcMotorSimple.Direction motorDirection = DcMotorSimple.Direction.FORWARD;
    // Fixed hive coordinates belong in FieldConfig; this class describes the hardware.
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

    public ShooterConfig(String flywheelName, String hoodName, String gateName) {
        this.flywheelName = flywheelName;
        this.hoodName = hoodName;
        this.gateName = gateName;
    }

    // Each OpMode gets its own settings; tuning must not mutate the shared defaults.
    public ShooterConfig copy() {
        ShooterConfig copy = new ShooterConfig(flywheelName, hoodName, gateName);
        copy.gateClosedPosition = gateClosedPosition;
        copy.gateOpenPosition = gateOpenPosition;
        copy.motorDirection = motorDirection;
        copy.launchHeight = launchHeight;
        copy.forwardOffset = forwardOffset;
        copy.leftOffset = leftOffset;
        copy.headingOffset = headingOffset;
        copy.wheelDiameter = wheelDiameter;
        copy.rpmCorrection = rpmCorrection;
        copy.wheelToMotorRatio = wheelToMotorRatio;
        copy.ticksPerMotorRevolution = ticksPerMotorRevolution;
        copy.maxWheelRpm = maxWheelRpm;
        copy.minAngle = minAngle;
        copy.maxAngle = maxAngle;
        copy.servoAtMinAngle = servoAtMinAngle;
        copy.servoAtMaxAngle = servoAtMaxAngle;
        return copy;
    }
}
