package org.firstinspires.ftc.teamcode.config;

import com.pedropathing.math.Pose;

/**
 * Official Pedro field frame: bottom-left = (0, 0), +X right, +Y up in the
 * field diagram. Field center = (72, 72) inches. Headings are counterclockwise
 * from +X (right). Positions/heights are inches; start headings are degrees.
 * https://pedropathing.com/docs/pathing/reference/coordinates
 */
public final class FieldConfig {
    public static final double METERS_PER_INCH = 0.0254;
    private static final double UNMEASURED = Double.NaN;

    private FieldConfig() { }

    public enum Alliance {
        // TODO: Enter each fixed scoring aim point as (x inches, y inches, height inches).
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
        // Arguments: label, alliance, x inches, y inches, heading degrees.
        // Left/right are named from that alliance's view into the field.
        RED_RIGHT("Red Right", Alliance.RED, UNMEASURED, UNMEASURED, UNMEASURED),
        RED_LEFT("Red Left", Alliance.RED, UNMEASURED, UNMEASURED, UNMEASURED),
        BLUE_RIGHT("Blue Right", Alliance.BLUE, UNMEASURED, UNMEASURED, UNMEASURED),
        BLUE_LEFT("Blue Left", Alliance.BLUE, UNMEASURED, UNMEASURED, UNMEASURED);

        public final String label;
        public final Alliance alliance;
        public final double xInches;
        public final double yInches;
        public final double headingDegrees;

        StartPosition(String label, Alliance alliance, double xInches, double yInches,
                      double headingDegrees) {
            this.label = label;
            this.alliance = alliance;
            this.xInches = xInches;
            this.yInches = yInches;
            this.headingDegrees = headingDegrees;
        }

        public boolean isMeasured() {
            return Double.isFinite(xInches) && Double.isFinite(yInches)
                    && Double.isFinite(headingDegrees);
        }

        public Pose toPedroPose() {
            return new Pose(xInches, yInches, Math.toRadians(headingDegrees));
        }
    }

    public static final class Target {
        public final double xInches;
        public final double yInches;
        public final double heightInches;

        public Target(double xInches, double yInches, double heightInches) {
            this.xInches = xInches;
            this.yInches = yInches;
            this.heightInches = heightInches;
        }

        public boolean isMeasured() {
            return Double.isFinite(xInches) && Double.isFinite(yInches)
                    && Double.isFinite(heightInches) && heightInches >= 0;
        }
    }
}
