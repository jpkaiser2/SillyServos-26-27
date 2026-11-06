package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.config.AutonomousConfig;
import org.firstinspires.ftc.teamcode.config.FieldConfig;
import org.firstinspires.ftc.teamcode.config.ShooterCalibrationStore;
import org.firstinspires.ftc.teamcode.config.ShooterConfig;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.subsystems.IntakeTransfer;
import org.firstinspires.ftc.teamcode.subsystems.ShooterMath;
import org.firstinspires.ftc.teamcode.subsystems.TimedShot;
import org.firstinspires.ftc.teamcode.subsystems.Turret;

import static com.pedropathing.api.Paths.line;

// Four selectable starts -> one alliance shooting location -> pollen -> nectar -> stop.
@Autonomous(name = "Shooting Auto", group = "Silly Servos")
public final class ShootingAuto extends OpMode {
    private enum State { INIT, DRIVE, POLLEN, NECTAR, DONE, ABORTED }

    private final FieldConfig.StartPosition[] starts = FieldConfig.StartPosition.values();
    private final ElapsedTime clock = new ElapsedTime();
    private final Turret[] turrets = new Turret[2];
    private final ShooterConfig[] settings = new ShooterConfig[2];
    private final String[] calibration = new String[2];
    private final Pose[] aimPoses = new Pose[2];
    private Follower follower;
    private IntakeTransfer intakeTransfer;
    private FieldConfig.Alliance alliance;
    private State state = State.INIT;
    private TimedShot shotTimer;
    private int startIndex;
    private boolean previousLeft, previousRight;
    private double phaseStarted;
    private String message = "Select the actual starting position before Play.";

    @Override
    public void init() {
        AutonomousConfig.validate();
        intakeTransfer = new IntakeTransfer(hardwareMap);
        ShooterConfig[] defaults = {ShooterConfig.POLLEN, ShooterConfig.NECTAR};
        for (int i = 0; i < turrets.length; i++) {
            ShooterCalibrationStore.Loaded loaded = ShooterCalibrationStore.load(defaults[i]);
            settings[i] = loaded.config;
            calibration[i] = loaded.message;
            turrets[i] = new Turret(hardwareMap, loaded.config);
        }
        // Allow INIT telemetry/start selection even before the team has filled in Foresight.
        if (Constants.foresightConfig != null) follower = Constants.create(hardwareMap);
        showSelection();
    }

    @Override
    public void init_loop() {
        if (gamepad1.dpad_right && !previousRight) startIndex = (startIndex + 1) % starts.length;
        if (gamepad1.dpad_left && !previousLeft) startIndex = (startIndex + starts.length - 1) % starts.length;
        previousRight = gamepad1.dpad_right;
        previousLeft = gamepad1.dpad_left;
        showSelection();
    }

    private void showSelection() {
        FieldConfig.StartPosition start = starts[startIndex];
        telemetry.addLine("G1 D-pad left/right: select start. At Play: drive, shoot POLLEN, then NECTAR.");
        telemetry.addData("Start / alliance", "%s / %s", start.label, start.alliance);
        telemetry.addData("Start measured", start.isMeasured());
        telemetry.addData("Alliance shooting position measured", start.alliance.shootingPosition.isMeasured());
        telemetry.addData("Pollen / nectar hive measured", "%s / %s",
                start.alliance.pollenHive.isMeasured(), start.alliance.nectarHive.isMeasured());
        telemetry.addData("Shooting robot XY (in)", "%.1f, %.1f",
                start.alliance.shootingPosition.xInches, start.alliance.shootingPosition.yInches);
        telemetry.addData("Foresight configured", Constants.foresightConfig != null);
        telemetry.addData("Pollen calibration", calibration[0]);
        telemetry.addData("Nectar calibration", calibration[1]);
        telemetry.addData("Feed seconds: pollen / nectar", "%.1f / %.1f",
                AutonomousConfig.POLLEN_FEED_SECONDS, AutonomousConfig.NECTAR_FEED_SECONDS);
        String problem = configurationProblem();
        telemetry.addData("Setup", problem == null ? "Ready: verify the straight route is clear." : problem);
        telemetry.addLine("Keep still during Pinpoint initialization; place robot at the selected start/heading.");
        telemetry.update();
    }

    private String configurationProblem() {
        FieldConfig.StartPosition start = starts[startIndex];
        if (!start.isMeasured()) return "Enter the selected start in FieldConfig.";
        if (!start.alliance.shootingPosition.isMeasured()) return "Enter this alliance's ShootingPosition in FieldConfig.";
        if (!start.alliance.pollenHive.isMeasured() || !start.alliance.nectarHive.isMeasured()) {
            return "Enter both fixed hive aim points for this alliance in FieldConfig.";
        }
        if (follower == null) return "Tune Foresight and set Constants.foresightConfig, then re-INIT.";
        for (int i = 0; i < turrets.length; i++) {
            Pose aim = aimPose(start.alliance, i);
            if (!finitePose(aim) || solve(aim, start.alliance, i) == null) {
                return "No valid " + (i == 0 ? "pollen" : "nectar") + " shot from this shooting position. Check calibration/geometry/RPM limit.";
            }
        }
        return null;
    }

    // Heading accounts for both the horizontal mounting angle and the off-center launch point.
    private Pose aimPose(FieldConfig.Alliance selectedAlliance, int shooter) {
        FieldConfig.ShootingPosition point = selectedAlliance.shootingPosition;
        double heading = ShooterMath.aimHeading(point.xInches * FieldConfig.METERS_PER_INCH,
                point.yInches * FieldConfig.METERS_PER_INCH, settings[shooter], target(selectedAlliance, shooter));
        return new Pose(point.xInches, point.yInches, heading);
    }

    private ShooterMath.Shot solve(Pose pose, FieldConfig.Alliance selectedAlliance, int shooter) {
        return ShooterMath.solve(pose.x() * FieldConfig.METERS_PER_INCH,
                pose.y() * FieldConfig.METERS_PER_INCH, pose.heading(), settings[shooter], target(selectedAlliance, shooter));
    }

    private FieldConfig.Target target(FieldConfig.Alliance selectedAlliance, int shooter) {
        return shooter == 0 ? selectedAlliance.pollenHive : selectedAlliance.nectarHive;
    }

    @Override
    public void start() {
        String problem = configurationProblem();
        if (problem != null) {
            abort(problem);
            return;
        }
        alliance = starts[startIndex].alliance;
        Pose start = starts[startIndex].toPedroPose();
        aimPoses[0] = aimPose(alliance, 0);
        aimPoses[1] = aimPose(alliance, 1);
        follower.setPose(start);
        follower.update();
        clock.reset();
        phaseStarted = 0;
        stopMechanisms();
        if (positionError(start) <= AutonomousConfig.POSITION_TOLERANCE_INCHES) {
            beginShot(0); // Avoid building a zero-length path when already at the shooting point.
        } else {
            // Both red starts share red's endpoint; both blue starts share blue's endpoint.
            // This is a straight route. Change this path if field obstacles require a detour.
            follower.follow(line(start, aimPoses[0]).linear(start, aimPoses[0])
                    .with(Constants.foresightConfig.maxVelocityConstraint.at(
                            AutonomousConfig.MAX_PATH_SPEED_INCHES_PER_SECOND)));
            state = State.DRIVE;
            message = "Driving to the " + alliance + " shooting position.";
        }
    }

    @Override
    public void loop() {
        if (state == State.INIT || state == State.DONE || state == State.ABORTED) {
            showStatus();
            return;
        }
        if (clock.seconds() >= AutonomousConfig.TOTAL_TIMEOUT_SECONDS) {
            abort("Overall autonomous timeout.");
            showStatus();
            return;
        }
        follower.update();
        Pose pose = follower.pose();
        Velocity velocity = follower.velocity();
        if (!finitePose(pose) || !Double.isFinite(velocity.vx)
                || !Double.isFinite(velocity.vy) || !Double.isFinite(velocity.omega)) {
            abort("Invalid odometry reading.");
            showStatus();
            return;
        }
        if (state == State.DRIVE) {
            stopMechanisms();
            if (clock.seconds() - phaseStarted >= AutonomousConfig.DRIVE_TIMEOUT_SECONDS) {
                abort("Could not reach the shooting position before the drive timeout.");
            } else if (positionError(pose) <= AutonomousConfig.POSITION_TOLERANCE_INCHES
                    && slowEnough(velocity) && (!follower.following() || follower.atParametricEnd())) {
                beginShot(0);
            }
        } else {
            updateShot(state == State.POLLEN ? 0 : 1, pose, velocity);
        }
        showStatus();
    }

    private void beginShot(int shooter) {
        stopMechanisms();
        follower.drivetrain.stop();
        follower.hold(aimPoses[shooter]);
        state = shooter == 0 ? State.POLLEN : State.NECTAR;
        phaseStarted = clock.seconds();
        shotTimer = new TimedShot(AutonomousConfig.READY_SETTLE_SECONDS, AutonomousConfig.GATE_OPEN_SECONDS,
                shooter == 0 ? AutonomousConfig.POLLEN_FEED_SECONDS : AutonomousConfig.NECTAR_FEED_SECONDS);
        message = "Holding position and settling " + state + " aim/RPM.";
    }

    private void updateShot(int shooter, Pose pose, Velocity velocity) {
        if (clock.seconds() - phaseStarted >= AutonomousConfig.SHOOTER_TIMEOUT_SECONDS) {
            abort(state + " timed out while aiming, spinning up, or feeding.");
            return;
        }
        Turret active = turrets[shooter];
        turrets[1 - shooter].stop();
        active.update(pose, target(alliance, shooter), true);
        ShooterMath.Shot shot = active.getShot();
        if (shot == null) {
            abort("No valid " + state + " shot at the current pose.");
            return;
        }
        double measured = active.getMeasuredRpm();
        boolean ready = positionError(pose) <= AutonomousConfig.POSITION_TOLERANCE_INCHES
                && Math.abs(shot.headingError) <= AutonomousConfig.AIM_TOLERANCE_RADIANS
                && slowEnough(velocity) && Double.isFinite(measured)
                && Math.abs(measured - shot.wheelRpm) <= Math.max(AutonomousConfig.RPM_TOLERANCE_MIN,
                        shot.wheelRpm * AutonomousConfig.RPM_TOLERANCE_FRACTION);
        shotTimer.update(clock.seconds(), ready);
        active.setGateOpen(shotTimer.isGateOpen());
        intakeTransfer.setPower(shotTimer.isFeeding() ? 1 : 0);
        message = shotTimer.isFeeding() ? "Feeding " + state + "."
                : shotTimer.isGateOpen() ? "Waiting for gate to open." : "Waiting for stable position, aim and RPM.";
        if (shotTimer.isComplete()) {
            if (shooter == 0) beginShot(1);
            else {
                state = State.DONE;
                message = "Finished pollen and nectar feed periods. All outputs stopped; gates closed.";
                stopOutputs();
            }
        }
    }

    private boolean slowEnough(Velocity velocity) {
        return velocity.linSpeed() <= AutonomousConfig.MAX_SHOOT_SPEED_INCHES_PER_SECOND
                && Math.abs(velocity.omega) <= AutonomousConfig.MAX_SHOOT_TURN_RATE_RADIANS_PER_SECOND;
    }

    private double positionError(Pose pose) {
        return Math.hypot(pose.x() - alliance.shootingPosition.xInches,
                pose.y() - alliance.shootingPosition.yInches);
    }

    private static boolean finitePose(Pose pose) {
        return Double.isFinite(pose.x()) && Double.isFinite(pose.y()) && Double.isFinite(pose.heading());
    }

    private void abort(String reason) {
        state = State.ABORTED;
        message = reason;
        stopOutputs();
    }

    private void showStatus() {
        telemetry.addData("Start / alliance", "%s / %s", starts[startIndex].label, starts[startIndex].alliance);
        telemetry.addData("State", state);
        telemetry.addData("Status", message);
        telemetry.addData("Elapsed seconds", "%.1f", clock.seconds());
        if (follower != null) {
            Pose pose = follower.pose();
            telemetry.addData("Pose (in, in, deg)", "%.1f, %.1f, %.1f", pose.x(), pose.y(), Math.toDegrees(pose.heading()));
            if (alliance != null) telemetry.addData("Shooting position error (in)", "%.2f", positionError(pose));
        }
        for (int i = 0; i < turrets.length; i++) {
            if (turrets[i] != null) telemetry.addData(i == 0 ? "Pollen RPM / gate" : "Nectar RPM / gate",
                    "%.0f commanded / %.0f measured / %s", turrets[i].getCommandedRpm(),
                    turrets[i].getMeasuredRpm(), turrets[i].isGateOpen() ? "Open" : "Closed");
        }
        if (shotTimer != null) telemetry.addData("Current shooter feed time (s)", "%.2f", shotTimer.getFedSeconds());
        telemetry.update();
    }

    private void stopMechanisms() {
        if (intakeTransfer != null) intakeTransfer.stop();
        for (Turret turret : turrets) if (turret != null) turret.stop();
    }

    private void stopOutputs() {
        stopMechanisms();
        if (follower != null) {
            follower.stop();
            follower.drivetrain.stop();
        }
    }

    @Override
    public void stop() { stopOutputs(); }
}
