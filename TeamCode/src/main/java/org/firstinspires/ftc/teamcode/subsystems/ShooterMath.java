package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.config.ShooterConfig;
import org.firstinspires.ftc.teamcode.config.FieldConfig;

// Equations from SillyServosShooterMath. Robot pose/config use SI; field targets use inches.
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

    // Returns null for an invalid or unreachable shot, including an excessive RPM.
    public static Shot solve(double robotX, double robotY, double heading, ShooterConfig c,
                             FieldConfig.Target target) {
        if (!validConfig(c) || !target.isMeasured()) return null;

        // Equations 27-28: rotate each launch point's forward/left offsets into the field.
        double launchX = robotX + c.forwardOffset * Math.cos(heading)
                - c.leftOffset * Math.sin(heading);
        double launchY = robotY + c.forwardOffset * Math.sin(heading)
                + c.leftOffset * Math.cos(heading);
        // FieldConfig uses Pedro inches; projectile motion uses meters throughout.
        double dx = target.xInches * FieldConfig.METERS_PER_INCH - launchX;
        double dy = target.yInches * FieldConfig.METERS_PER_INCH - launchY;
        double distance = Math.hypot(dx, dy);
        double height = target.heightInches * FieldConfig.METERS_PER_INCH - c.launchHeight;
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

    // Chassis heading that points the offset launch point at the target from a fixed robot XY.
    public static double aimHeading(double robotX, double robotY, ShooterConfig c, FieldConfig.Target target) {
        double dx = target.xInches * FieldConfig.METERS_PER_INCH - robotX;
        double dy = target.yInches * FieldConfig.METERS_PER_INCH - robotY;
        double distance = Math.hypot(dx, dy);
        // Mount offset expressed along/across the shooter's horizontal firing direction.
        double along = c.forwardOffset * Math.cos(c.headingOffset) + c.leftOffset * Math.sin(c.headingOffset);
        double across = -c.forwardOffset * Math.sin(c.headingOffset) + c.leftOffset * Math.cos(c.headingOffset);
        if (!Double.isFinite(distance) || !Double.isFinite(along) || !Double.isFinite(across)
                || distance < 1e-6 || Math.abs(across) >= distance
                || Math.sqrt(distance * distance - across * across) - along <= 1e-6) {
            return Double.NaN;
        }
        // distance*sin(targetBearing - chassisHeading - mountingAngle) = across.
        return wrapToPi(Math.atan2(dy, dx) - c.headingOffset - Math.asin(across / distance));
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
