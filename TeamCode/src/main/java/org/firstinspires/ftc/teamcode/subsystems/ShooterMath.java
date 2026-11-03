package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.config.ShooterConfig;

/** Equations from SillyServosShooterMath. All inputs use SI units. */
public final class ShooterMath {
    private static final double GRAVITY = 9.81;

    private ShooterMath() { }

    public static final class Shot {
        public final double distance;
        public final double headingError;
        public final double angle;
        public final double speed;
        public final double wheelRpm;

        private Shot(double distance, double headingError, double angle,
                     double speed, double wheelRpm) {
            this.distance = distance;
            this.headingError = headingError;
            this.angle = angle;
            this.speed = speed;
            this.wheelRpm = wheelRpm;
        }
    }

    /** Returns null for an invalid or unreachable shot, including an excessive RPM. */
    public static Shot solve(double robotX, double robotY, double heading, ShooterConfig c) {
        if (!validConfig(c)) return null;

        // Equations 27-28: rotate each launch point's forward/left offsets into the field.
        double launchX = robotX + c.forwardOffset * Math.cos(heading)
                - c.leftOffset * Math.sin(heading);
        double launchY = robotY + c.forwardOffset * Math.sin(heading)
                + c.leftOffset * Math.cos(heading);
        double dx = c.targetX - launchX;
        double dy = c.targetY - launchY;
        double distance = Math.hypot(dx, dy);
        double height = c.targetHeight - c.launchHeight;
        if (!Double.isFinite(distance) || distance < 1e-6 || !Double.isFinite(height)) {
            return null;
        }

        // Equation 21, limited to the actual hood travel.
        double optimalAngle = Math.PI / 4 + Math.atan2(height, distance) / 2;
        double angle = Math.max(c.minAngle, Math.min(c.maxAngle, optimalAngle));
        // Equation 13 also works when the optimal angle is outside the hood's limits.
        double denominator = 2 * Math.pow(Math.cos(angle), 2)
                * (distance * Math.tan(angle) - height);
        if (!(denominator > 0)) return null;
        double speed = Math.sqrt(GRAVITY * distance * distance / denominator);
        double rpm = 60 * c.rpmCorrection * speed / (Math.PI * c.wheelDiameter);
        double error = wrapToPi(Math.atan2(dy, dx) - heading - c.headingOffset);
        if (!Double.isFinite(rpm) || rpm > c.maxWheelRpm || !Double.isFinite(error)) {
            return null;
        }
        return new Shot(distance, error, angle, speed, rpm);
    }

    public static double wrapToPi(double angle) {
        double wrapped = (angle + Math.PI) % (2 * Math.PI);
        if (wrapped < 0) wrapped += 2 * Math.PI;
        return wrapped - Math.PI;
    }

    public static double motorTicksPerSecond(double wheelRpm, ShooterConfig c) {
        return wheelRpm / c.wheelToMotorRatio * c.ticksPerMotorRevolution / 60;
    }

    public static double wheelRpm(double motorTicksPerSecond, ShooterConfig c) {
        return motorTicksPerSecond * 60 / c.ticksPerMotorRevolution * c.wheelToMotorRatio;
    }

    public static double hoodPosition(double angle, ShooterConfig c) {
        double fraction = (angle - c.minAngle) / (c.maxAngle - c.minAngle);
        fraction = Math.max(0, Math.min(1, fraction));
        return c.servoAtMinAngle + fraction * (c.servoAtMaxAngle - c.servoAtMinAngle);
    }

    private static boolean validConfig(ShooterConfig c) {
        return positive(c.wheelDiameter) && positive(c.rpmCorrection)
                && positive(c.wheelToMotorRatio) && positive(c.ticksPerMotorRevolution)
                && positive(c.maxWheelRpm)
                && c.minAngle > -Math.PI / 2 && c.maxAngle < Math.PI / 2
                && c.minAngle < c.maxAngle
                && c.servoAtMinAngle >= 0 && c.servoAtMinAngle <= 1
                && c.servoAtMaxAngle >= 0 && c.servoAtMaxAngle <= 1;
    }

    private static boolean positive(double value) {
        return Double.isFinite(value) && value > 0;
    }
}
