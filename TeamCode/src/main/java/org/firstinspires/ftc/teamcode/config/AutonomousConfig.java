package org.firstinspires.ftc.teamcode.config;

// Shooting Auto settings. Times are seconds and Pedro distances are inches. Tune on the robot.
public final class AutonomousConfig {
    private AutonomousConfig() { }

    public static final double MAX_PATH_SPEED_INCHES_PER_SECOND = 24;
    public static final double DRIVE_TIMEOUT_SECONDS = 12;
    public static final double SHOOTER_TIMEOUT_SECONDS = 8; // Per shooter, including settling/recovery.
    public static final double TOTAL_TIMEOUT_SECONDS = 28;

    // Actual time feeding; there is no ball-count sensor. Adjust to the loaded balls/mechanism.
    public static final double POLLEN_FEED_SECONDS = 2;
    public static final double NECTAR_FEED_SECONDS = 2;
    public static final double READY_SETTLE_SECONDS = 0.5;
    public static final double GATE_OPEN_SECONDS = 0.25; // Wait for the gate before starting the feeders.

    public static final double POSITION_TOLERANCE_INCHES = 1;
    public static final double AIM_TOLERANCE_RADIANS = Math.toRadians(2);
    public static final double MAX_SHOOT_SPEED_INCHES_PER_SECOND = 1;
    public static final double MAX_SHOOT_TURN_RATE_RADIANS_PER_SECOND = Math.toRadians(5);
    public static final double RPM_TOLERANCE_FRACTION = 0.05;
    public static final double RPM_TOLERANCE_MIN = 75;

    public static void validate() {
        for (double value : new double[]{MAX_PATH_SPEED_INCHES_PER_SECOND, DRIVE_TIMEOUT_SECONDS,
                SHOOTER_TIMEOUT_SECONDS, TOTAL_TIMEOUT_SECONDS, POLLEN_FEED_SECONDS, NECTAR_FEED_SECONDS,
                READY_SETTLE_SECONDS, POSITION_TOLERANCE_INCHES, AIM_TOLERANCE_RADIANS,
                MAX_SHOOT_SPEED_INCHES_PER_SECOND, MAX_SHOOT_TURN_RATE_RADIANS_PER_SECOND,
                RPM_TOLERANCE_FRACTION, RPM_TOLERANCE_MIN}) {
            if (!Double.isFinite(value) || value <= 0) {
                throw new IllegalArgumentException("AutonomousConfig limits/times must be finite and positive.");
            }
        }
        if (!Double.isFinite(GATE_OPEN_SECONDS) || GATE_OPEN_SECONDS < 0) {
            throw new IllegalArgumentException("AutonomousConfig gate delay must be finite and nonnegative.");
        }
    }
}
