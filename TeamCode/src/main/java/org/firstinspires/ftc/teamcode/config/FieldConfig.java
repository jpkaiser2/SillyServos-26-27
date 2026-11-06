package org.firstinspires.ftc.teamcode.config;

import com.pedropathing.math.Pose;

/**
 * Official Pedro field frame: bottom-left = (0, 0), +X right, +Y up in the
 * BIOBUZZ field diagram. Field center = (70.75, 70.75) inches. Headings are counterclockwise
 * from +X (right). Positions/heights are inches; start headings are degrees.
 * https://pedropathing.com/docs/pathing/reference/coordinates
 */
public final class FieldConfig {
    public static final double METERS_PER_INCH = 0.0254;
    // The BIOBUZZ diagram is 141.5 inches wide; the docs' generic +72 example is nominal.
    public static final double FIELD_SIZE_INCHES = 141.5;
    public static final double FIELD_CENTER_INCHES = FIELD_SIZE_INCHES / 2;
    private static final double UNMEASURED = Double.NaN;

    // BIOBUZZ TU07, pp. 68-70, Figures 9-8 through 9-11. Nominal opening-center aim
    // points, with the hives in the match-start orientation (Figure 10-2, p. 81).
    // Both game pieces score in the SAME upward-facing cell for their alliance.
    private static final double HIVE_X_OFFSET_INCHES = 25.5 / 2;
    private static final double HIVE_UP_AIM_HEIGHT_INCHES = (53.5 + 65.6) / 2;
    private static final double HIVE_TILT_RADIANS = Math.toRadians(30);
    // Undo the tilt at the opening plane: r = y*cos(tilt) + (height-pivot)*sin(tilt).
    // r is half the level hive's 42.91-inch outside span, NOT half the 18.84-inch gap.
    private static final double HIVE_UP_Y_OFFSET_INCHES = (42.91 / 2
            - (HIVE_UP_AIM_HEIGHT_INCHES - 43.95) * Math.sin(HIVE_TILT_RADIANS))
            / Math.cos(HIVE_TILT_RADIANS);

    private FieldConfig() { }

    public enum Alliance {
        // TODO: Measure the robot's ShootingPosition (x inches, y inches) for each alliance.
        // Red's starting UP cell is toward the audience (-Y); blue's is away (+Y).
        // Targets stay fixed: this configuration does not track subsequent hive tips.
        RED(new ShootingPosition(UNMEASURED, UNMEASURED),
                new Target(FIELD_CENTER_INCHES - HIVE_X_OFFSET_INCHES,
                        FIELD_CENTER_INCHES - HIVE_UP_Y_OFFSET_INCHES,
                        HIVE_UP_AIM_HEIGHT_INCHES)),
        BLUE(new ShootingPosition(UNMEASURED, UNMEASURED),
                new Target(FIELD_CENTER_INCHES + HIVE_X_OFFSET_INCHES,
                        FIELD_CENTER_INCHES + HIVE_UP_Y_OFFSET_INCHES,
                        HIVE_UP_AIM_HEIGHT_INCHES));

        public final ShootingPosition shootingPosition;
        public final Target pollenHive;
        public final Target nectarHive;

        Alliance(ShootingPosition shootingPosition, Target upwardCell) {
            this.shootingPosition = shootingPosition;
            this.pollenHive = upwardCell;
            this.nectarHive = upwardCell;
        }
    }

    // Robot's odometry reference location, not the hive location. Heading is computed per shooter.
    public static final class ShootingPosition {
        public final double xInches;
        public final double yInches;

        public ShootingPosition(double xInches, double yInches) {
            this.xInches = xInches;
            this.yInches = yInches;
        }

        public boolean isMeasured() {
            return Double.isFinite(xInches) && Double.isFinite(yInches);
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
