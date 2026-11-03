package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.config.FieldConfig;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.config.ShooterCalibrationStore;
import org.firstinspires.ftc.teamcode.config.ShooterConfig;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroMecanumDrive;
import org.firstinspires.ftc.teamcode.subsystems.IntakeTransfer;
import org.firstinspires.ftc.teamcode.subsystems.ShooterMath;
import org.firstinspires.ftc.teamcode.subsystems.Turret;
import org.firstinspires.ftc.teamcode.subsystems.TurretCalibration;

/** Guided, stationary-shot calibration in a local Pedro frame. No FieldConfig starts required. */
@TeleOp(name = "Turret Tuner", group = "Silly Servos")
public final class TurretTuningTeleOp extends OpMode {
    private static final double INCH = FieldConfig.METERS_PER_INCH;
    private static final int A = 1, B = 2, Y = 4, BACK = 8, START = 16, LEFT = 32, RIGHT = 64;
    private final ElapsedTime clock = new ElapsedTime();
    private PedroMecanumDrive drive;
    private IntakeTransfer feed;
    private Session[] sessions;
    private int selected, parameter, previousButtons, heldAdjustment;
    private boolean previousModeButton, requireRelease = true, triggerSampled;
    private double nextAdjustment, readySince = Double.NaN;
    private Pose stillPose = Pose.zero();
    private double stillSince;
    private TurretCalibration.Sample pending;
    private String message = "Mark the target on the floor; place the odometry reference over that mark before Play.";

    private static final class Session {
        final ShooterConfig defaults, config;
        final Turret turret;
        final TurretCalibration.Sample[] samples = new TurretCalibration.Sample[4];
        final String loadMessage;
        int count;
        double hood, rpm, heightInches = 24;
        boolean calibrated, verify, dirty;

        Session(ShooterConfig defaults, ShooterCalibrationStore.Loaded loaded, Turret turret) {
            this.defaults = defaults;
            config = loaded.config;
            this.turret = turret;
            loadMessage = loaded.message;
            calibrated = loaded.saved;
            verify = loaded.saved;
            hood = config.servoAtMinAngle;
            rpm = Math.min(1500, config.maxWheelRpm);
        }
    }

    @Override
    public void init() {
        drive = new PedroMecanumDrive(hardwareMap);
        feed = new IntakeTransfer(hardwareMap);
        sessions = new Session[2];
        ShooterConfig[] defaults = {ShooterConfig.POLLEN, ShooterConfig.NECTAR};
        for (int i = 0; i < sessions.length; i++) {
            ShooterCalibrationStore.Loaded loaded = ShooterCalibrationStore.load(defaults[i]);
            sessions[i] = new Session(defaults[i], loaded, new Turret(hardwareMap, loaded.config));
        }
        init_loop();
    }

    @Override
    public void init_loop() {
        telemetry.addLine("TURRET TUNER: Pedro must already be calibrated.");
        telemetry.addLine("Place the ODOMETRY REFERENCE over the floor mark directly below the target aim point.");
        telemetry.addLine("You may install the practice target after driving away from the mark.");
        telemetry.addLine("Keep still during initialization. Play makes this mark local (0,0).");
        telemetry.addLine("Target must be at least 6 inches ABOVE each ball's launch point.");
        telemetry.addLine("After Play, set Height to that measured vertical difference (default 24 in per shooter).");
        telemetry.addData("Pollen", sessions[0].loadMessage);
        telemetry.addData("Nectar", sessions[1].loadMessage);
        telemetry.update();
    }

    @Override
    public void start() {
        drive.setStartingPose(Pose.zero());
        drive.startTeleOpDrive();
        clock.reset();
        stillSince = 0;
        stillPose = Pose.zero();
        message = "Drive away from the mark. Set Height, then choose a low hood position and RPM.";
    }

    @Override
    public void loop() {
        drive.update();
        Pose pose = drive.getPose();
        double now = clock.seconds();
        int buttons = buttons();
        int pressed = buttons & ~previousButtons;
        previousButtons = buttons;

        int choice = (pressed & B) != 0 ? 1 : (pressed & A) != 0 ? 0 : selected;
        if (choice != selected) {
            stopOutputs();
            selected = choice;
            parameter = 0;
            resetObservation(true);
            message = "Selected " + name() + ". Check its Height setting.";
        }
        Session s = sessions[selected];
        if ((buttons & (BACK | START)) == (BACK | START) && (pressed & (BACK | START)) != 0) {
            deleteSaved(s);
        } else if ((pressed & BACK) != 0) {
            undo(s);
        } else if ((pressed & START) != 0) {
            save(s);
        }
        if (gamepad1.y && !previousModeButton) {
            if (s.calibrated) {
                s.verify = !s.verify;
                resetObservation(true);
                stopOutputs();
                message = s.verify ? "VERIFY: math controls hood/RPM. Check several distances before saving."
                        : "MANUAL: finish/redo the four successful observations.";
            } else message = "Record four successful shots first to unlock Verify.";
        }
        previousModeButton = gamepad1.y;
        if ((pressed & LEFT) != 0) parameter = (parameter + 2) % 3;
        if ((pressed & RIGHT) != 0) parameter = (parameter + 1) % 3;
        int adjustment = gamepad2.dpad_up ? 1 : gamepad2.dpad_down ? -1 : 0;
        if (adjustment != 0 && (adjustment != heldAdjustment || now >= nextAdjustment)) {
            adjust(s, adjustment);
            nextAdjustment = now + (adjustment == heldAdjustment ? 0.12 : 0.4);
        }
        heldAdjustment = adjustment;

        boolean trigger = gamepad2.right_trigger > 0.1;
        if (!trigger) triggerSampled = false;
        if (!gamepad2.left_bumper && !gamepad2.right_bumper && !trigger && !gamepad2.x) {
            requireRelease = false;
        }
        boolean reverse = !requireRelease && gamepad2.x;
        boolean spin = !requireRelease && gamepad2.right_bumper && !reverse;
        boolean moveHood = !requireRelease && (gamepad2.left_bumper || spin) && !reverse;

        double heading = pose.heading();
        double launchX = pose.x() * INCH + s.config.forwardOffset * Math.cos(heading)
                - s.config.leftOffset * Math.sin(heading);
        double launchY = pose.y() * INCH + s.config.forwardOffset * Math.sin(heading)
                + s.config.leftOffset * Math.cos(heading);
        double distance = Math.hypot(launchX, launchY);
        double bearingOffset = ShooterMath.wrapToPi(Math.atan2(-launchY, -launchX) - heading);
        FieldConfig.Target target = new FieldConfig.Target(0, 0,
                s.config.launchHeight / INCH + s.heightInches);
        ShooterMath.Shot shot = ShooterMath.solve(pose.x() * INCH, pose.y() * INCH,
                heading, s.config, target);
        double hood = s.verify && shot != null ? ShooterMath.hoodPosition(shot.angle, s.config) : s.hood;
        double rpm = s.verify ? (shot == null ? 0 : shot.wheelRpm) : s.rpm;
        boolean valid = Double.isFinite(distance) && distance >= 12 * INCH
                && Double.isFinite(s.config.launchHeight) && s.config.launchHeight >= 0
                && Double.isFinite(s.config.wheelDiameter) && s.config.wheelDiameter > 0
                && s.heightInches >= 6 && (!s.verify || shot != null);
        sessions[1 - selected].turret.stop();
        boolean outputValid = true;
        if (moveHood && (!s.verify || shot != null)) {
            outputValid = s.turret.tune(hood, spin && valid ? rpm : 0);
        } else s.turret.stop();

        // Robot-centric slow positioning; all drive output stops during hood/spin/feed operation.
        double forward = Range.clip(-gamepad1.left_stick_y, -0.35, 0.35);
        double strafe = Range.clip(-gamepad1.left_stick_x, -0.35, 0.35);
        double turn = Range.clip(-gamepad1.right_stick_x, -0.3, 0.3);
        if (s.verify && gamepad1.left_bumper) {
            forward = strafe = 0;
            turn = shot == null ? 0 : Range.clip(RobotConstants.AIM_KP * shot.headingError, -0.3, 0.3);
            if (shot != null && Math.abs(shot.headingError) < RobotConstants.AIM_TOLERANCE_RADIANS) turn = 0;
        }
        if (moveHood || trigger || reverse || requireRelease) forward = strafe = turn = 0;
        drive.setDrivePowers(forward, strafe, turn, false);
        if (Math.hypot(pose.x() - stillPose.x(), pose.y() - stillPose.y()) > 0.25
                || Math.abs(ShooterMath.wrapToPi(heading - stillPose.heading())) > Math.toRadians(0.5)
                || forward != 0 || strafe != 0 || turn != 0) {
            stillPose = pose;
            stillSince = now;
        }
        double measuredRpm = s.turret.getMeasuredRpm();
        boolean settled = spin && valid && outputValid && rpm >= 100 && now - stillSince >= 0.5
                && Double.isFinite(measuredRpm) && Math.abs(measuredRpm - rpm) <= Math.max(50, rpm * 0.03);
        if (!settled) readySince = Double.NaN;
        else if (Double.isNaN(readySince)) readySince = now;
        boolean ready = settled && now - readySince >= 0.5;
        boolean feeding = ready && trigger;
        feed.setPower(reverse ? -1 : feeding ? 1 : 0);
        if (feeding && !triggerSampled) {
            triggerSampled = true;
            if (!s.verify) {
                pending = new TurretCalibration.Sample(distance, s.heightInches * INCH,
                        measuredRpm, bearingOffset, hood);
                message = "Shot captured. If it HIT the aim point, release RT and press G2 Y. Miss? Retry RT.";
            }
        }
        if ((pressed & Y) != 0) acceptHit(s);
        show(s, pose, distance, rpm, measuredRpm, hood, ready, shot);
    }

    private void adjust(Session s, int direction) {
        if (parameter == 0 && s.verify) {
            s.config.rpmCorrection = Range.clip(s.config.rpmCorrection * Math.pow(1.01, direction), 0.01, 20);
            s.dirty = true;
        } else if (parameter == 0) {
            if (s.count % 2 == 1) {
                message = "Hood locked for this pair. G2 Back undoes the first observation if you need to change it.";
                return;
            }
            s.hood = Range.clip(s.hood + direction * 0.005, 0, 1);
        } else if (parameter == 1) {
            if (s.verify) {
                message = "Verify calculates RPM. Adjust k instead.";
                return;
            }
            s.rpm = Range.clip(s.rpm + direction * 50, 0, s.config.maxWheelRpm);
        } else {
            if (s.count > 0) {
                message = "Height is locked after a recorded hit. Undo all observations before changing it.";
                return;
            }
            s.heightInches = Range.clip(s.heightInches + direction * 0.5, 6, 120);
        }
        resetObservation(false);
    }

    private void acceptHit(Session s) {
        if (s.verify || s.count == 4) {
            message = "Verify at several distances, then G2 Start saves. G2 Back reopens the last observation.";
            return;
        }
        if (pending == null || gamepad2.right_trigger > 0.1) {
            message = "Feed one shot with RB + RT, release RT, then Y only if it hit the aim point.";
            return;
        }
        s.samples[s.count] = pending;
        try {
            if (s.count % 2 == 1) {
                TurretCalibration.fitPair(s.samples[s.count - 1], pending, s.config.wheelDiameter);
            }
            if (s.count == 3) {
                TurretCalibration.Result fit = TurretCalibration.fit(s.samples, s.config.wheelDiameter);
                s.config.minAngle = fit.minAngle;
                s.config.maxAngle = fit.maxAngle;
                s.config.servoAtMinAngle = fit.servoAtMin;
                s.config.servoAtMaxAngle = fit.servoAtMax;
                s.config.rpmCorrection = fit.correction;
                s.config.headingOffset = fit.headingOffset;
                s.calibrated = s.verify = s.dirty = true;
            }
            s.count++;
            if (s.count == 2) {
                s.hood = s.config.servoAtMaxAngle;
                parameter = 0;
            }
            message = s.count == 4 ? "Fit complete. VERIFY at several distances, then G2 Start to save."
                    : s.count == 2 ? "Low pair complete. Choose a visibly HIGHER launch angle for the next pair."
                    : "Hit accepted. Move at least 12 inches nearer/farther, keep hood fixed, and find the next hit.";
        } catch (IllegalArgumentException e) {
            s.samples[s.count] = null;
            message = e.getMessage();
        }
        resetObservation(true);
        stopOutputs();
    }

    private void undo(Session s) {
        if (s.count > 0) {
            s.count--;
            s.hood = s.samples[s.count].servo;
            s.samples[s.count] = null;
        }
        s.verify = false;
        resetObservation(true);
        stopOutputs();
        message = "Manual observation " + (s.count + 1) + "/4. Saved calibration has not changed.";
    }

    private void save(Session s) {
        if (!s.calibrated || !s.verify || !controlsReleased()) {
            message = "Finish a fit, select Verify, and release hood/spin/feed controls before saving.";
            return;
        }
        try {
            ShooterCalibrationStore.save(s.config);
            s.dirty = false;
            message = "SAVED " + name() + ". Shooter TeleOp will load this at its next INIT.";
        } catch (Exception e) {
            message = "Save failed: " + e.getMessage();
        }
    }

    private void deleteSaved(Session s) {
        if (!controlsReleased()) {
            message = "Release hood/spin/feed controls before deleting a calibration.";
            return;
        }
        try {
            ShooterCalibrationStore.remove(s.config);
            s.config.minAngle = s.defaults.minAngle;
            s.config.maxAngle = s.defaults.maxAngle;
            s.config.servoAtMinAngle = s.defaults.servoAtMinAngle;
            s.config.servoAtMaxAngle = s.defaults.servoAtMaxAngle;
            s.config.rpmCorrection = s.defaults.rpmCorrection;
            s.config.headingOffset = s.defaults.headingOffset;
            s.count = 0;
            s.calibrated = s.verify = s.dirty = false;
            s.hood = s.config.servoAtMinAngle;
            resetObservation(true);
            stopOutputs();
            message = "Deleted only " + name() + " calibration. Using ShooterConfig defaults.";
        } catch (Exception e) {
            message = "Delete failed: " + e.getMessage();
        }
    }

    private boolean controlsReleased() {
        return !gamepad2.left_bumper && !gamepad2.right_bumper
                && gamepad2.right_trigger <= 0.1 && !gamepad2.x;
    }

    private void resetObservation(boolean release) {
        pending = null;
        readySince = Double.NaN;
        if (release) requireRelease = true;
    }

    private void show(Session s, Pose pose, double distance, double rpm, double measuredRpm,
                      double hood, boolean ready, ShooterMath.Shot shot) {
        telemetry.addData("Shooter / mode", "%s / %s", name(), s.verify ? "VERIFY" : "MANUAL");
        telemetry.addData("Message", message);
        telemetry.addData("Observation", s.count == 4 ? "4/4 complete" : (s.count + 1) + "/4: "
                + (s.count < 2 ? "LOW hood" : "HIGH hood") + (s.count % 2 == 0 ? ", first distance" : ", second distance"));
        telemetry.addData("Adjust with G2 D-pad", parameter == 0 ? (s.verify ? "k (1% steps)" : "Hood (0.005 steps)")
                : parameter == 1 ? "RPM (50 steps)" : "Target ABOVE launch (0.5 inch steps)");
        telemetry.addData("Height above launch / distance (in)", "%.1f / %.1f", s.heightInches, distance / INCH);
        if (s.count % 2 == 1) telemetry.addData("First distance in this pair (in)", "%.1f", s.samples[s.count - 1].distance / INCH);
        telemetry.addData("Hood position / requested RPM", "%.3f / %.0f", hood, rpm);
        telemetry.addData("Commanded / measured RPM", "%.0f / %.0f", s.turret.getCommandedRpm(), measuredRpm);
        telemetry.addData("Feed", ready ? "READY: RT feeds both paths" : "Waiting: hold RB, stationary, RPM settled");
        telemetry.addData("Pose (local in, in, deg)", "%.1f, %.1f, %.1f", pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        if (s.verify && shot == null) telemetry.addLine("No valid calculated shot: check distance, height, geometry and RPM limit.");
        if (s.verify && shot != null) telemetry.addData("Launch angle / aim error (deg)", "%.2f / %.2f",
                Math.toDegrees(shot.angle), Math.toDegrees(shot.headingError));
        if (s.calibrated) {
            telemetry.addData("Fit: low/high angle (deg)", "%.2f / %.2f", Math.toDegrees(s.config.minAngle), Math.toDegrees(s.config.maxAngle));
            telemetry.addData("Fit: low/high servo", "%.3f / %.3f", s.config.servoAtMinAngle, s.config.servoAtMaxAngle);
            telemetry.addData("Fit: k / heading offset (deg)", "%.5f / %.2f", s.config.rpmCorrection, Math.toDegrees(s.config.headingOffset));
            telemetry.addData("Calibration", s.dirty ? "UNSAVED CHANGES" : "Saved profile available");
        }
        telemetry.addLine("G1 sticks: slow robot-centric drive | LB: aim in Verify | Y: Manual/Verify");
        telemetry.addLine("G2 A/B: pollen/nectar | D-pad L/R: field | U/D: change (hold to repeat)");
        telemetry.addLine("G2 hold LB: hood only | hold RB: hood + spin | RB + RT: feed | X: reverse");
        telemetry.addLine("G2 Y: accept last HIT | Back: undo | Start: save | Back + Start: delete selected saved profile");
        telemetry.addLine("Release hood/spin/feed controls to drive. Saved data stays on this Robot Controller.");
        telemetry.update();
    }

    private int buttons() {
        return (gamepad2.a ? A : 0) | (gamepad2.b ? B : 0) | (gamepad2.y ? Y : 0)
                | (gamepad2.back ? BACK : 0) | (gamepad2.start ? START : 0)
                | (gamepad2.dpad_left ? LEFT : 0) | (gamepad2.dpad_right ? RIGHT : 0);
    }

    private String name() { return selected == 0 ? "Pollen" : "Nectar"; }

    private void stopOutputs() {
        if (drive != null) drive.stop();
        if (feed != null) feed.stop();
        if (sessions != null) for (Session s : sessions) if (s != null) s.turret.stop();
    }

    @Override
    public void stop() { stopOutputs(); }
}
