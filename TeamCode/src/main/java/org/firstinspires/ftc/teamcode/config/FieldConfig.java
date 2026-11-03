package org.firstinspires.ftc.teamcode.config;

import com.pedropathing.geometry.Pose;

/**
 * One field frame for every start and hive: center = (0, 0), +X toward the blue
 * alliance wall, +Y left when facing +X. Headings are counterclockwise from +X.
 * Positions/heights are meters; start headings below are entered in degrees.
 */
public final class FieldConfig {
    public static final double METERS_PER_INCH = 0.0254;
    private static final double UNMEASURED = Double.NaN;

    private FieldConfig() { }

    public enum Alliance {
        // TODO: Enter each fixed scoring aim point as (x meters, y meters, height meters).
        // Pollen first, nectar second. If their aim points coincide, use the same values.
        RED(new Target(UNMEASURED, UNMEASURED, UNMEASURED),
                new Target(UNMEASURED, UNMEASURED, UNMEASURED)),
        BLUE(new Target(UNMEASURED, UNMEASURED, UNMEASURED),
                new Target(UNMEASURED, UNMEASURED, UNMEASURED));

        public final Target pollenHive;
        public final Target nectarHive;

        Alliance(Target pollenHive, Target nectarHive) {
            this.pollenHive = pollenHive;
            this.nectarHive = nectarHive;
        }
    }

    public enum StartPosition {
        // TODO: Measure the odometry reference point, not the robot's edge.
        // Arguments: label, alliance, x meters, y meters, heading degrees.
        // Left/right are named from that alliance's view into the field.
        RED_RIGHT("Red Right", Alliance.RED, UNMEASURED, UNMEASURED, UNMEASURED),
        RED_LEFT("Red Left", Alliance.RED, UNMEASURED, UNMEASURED, UNMEASURED),
        BLUE_RIGHT("Blue Right", Alliance.BLUE, UNMEASURED, UNMEASURED, UNMEASURED),
        BLUE_LEFT("Blue Left", Alliance.BLUE, UNMEASURED, UNMEASURED, UNMEASURED);

        public final String label;
        public final Alliance alliance;
        public final double xMeters;
        public final double yMeters;
        public final double headingDegrees;

        StartPosition(String label, Alliance alliance, double xMeters, double yMeters,
                      double headingDegrees) {
            this.label = label;
            this.alliance = alliance;
            this.xMeters = xMeters;
            this.yMeters = yMeters;
            this.headingDegrees = headingDegrees;
        }

        public boolean isMeasured() {
            return Double.isFinite(xMeters) && Double.isFinite(yMeters)
                    && Double.isFinite(headingDegrees);
        }

        public Pose toPedroPose() {
            return new Pose(xMeters / METERS_PER_INCH, yMeters / METERS_PER_INCH,
                    Math.toRadians(headingDegrees));
        }
    }

    public static final class Target {
        public final double xMeters;
        public final double yMeters;
        public final double heightMeters;

        public Target(double xMeters, double yMeters, double heightMeters) {
            this.xMeters = xMeters;
            this.yMeters = yMeters;
            this.heightMeters = heightMeters;
        }

        public boolean isMeasured() {
            return Double.isFinite(xMeters) && Double.isFinite(yMeters)
                    && Double.isFinite(heightMeters) && heightMeters >= 0;
        }
    }
}
