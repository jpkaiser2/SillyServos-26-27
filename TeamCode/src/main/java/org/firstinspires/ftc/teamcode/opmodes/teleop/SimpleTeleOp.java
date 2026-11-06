package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.config.ShooterCalibrationStore;
import org.firstinspires.ftc.teamcode.config.ShooterConfig;
import org.firstinspires.ftc.teamcode.subsystems.IntakeTransfer;
import org.firstinspires.ftc.teamcode.subsystems.MecanumDrive;
import org.firstinspires.ftc.teamcode.subsystems.Turret;

// Manual driving and shooting, independent of Pedro, Pinpoint, and field coordinates.
@TeleOp(name = "Simple TeleOp", group = "Silly Servos")
public final class SimpleTeleOp extends OpMode {
    private static final double INITIAL_RPM = 1500;
    private static final double RPM_STEP = 50;
    private static final double HOOD_STEP = 0.01;
    private final ElapsedTime clock = new ElapsedTime();
    private final Turret[] turrets = new Turret[2];
    private final ShooterConfig[] settings = new ShooterConfig[2];
    private final double[] rpm = new double[2];
    private final double[] hood = new double[2];
    private final String[] calibration = new String[2];
    private MecanumDrive drive;
    private IntakeTransfer intakeTransfer;
    private int selected, previousRpmDirection, previousHoodDirection;
    private double nextRpmAdjustment, nextHoodAdjustment;

    @Override
    public void init() {
        drive = new MecanumDrive(hardwareMap);
        intakeTransfer = new IntakeTransfer(hardwareMap);
        ShooterConfig[] defaults = {ShooterConfig.POLLEN, ShooterConfig.NECTAR};
        for (int i = 0; i < turrets.length; i++) {
            ShooterCalibrationStore.Loaded loaded = ShooterCalibrationStore.load(defaults[i]);
            settings[i] = loaded.config;
            ShooterConfig c = settings[i];
            if (!Double.isFinite(c.maxWheelRpm) || c.maxWheelRpm <= 0
                    || !Double.isFinite(c.servoAtMinAngle) || c.servoAtMinAngle < 0 || c.servoAtMinAngle > 1
                    || !Double.isFinite(c.servoAtMaxAngle) || c.servoAtMaxAngle < 0 || c.servoAtMaxAngle > 1) {
                throw new IllegalArgumentException("Check RPM limit and hood endpoints for " + c.flywheelName);
            }
            turrets[i] = new Turret(hardwareMap, c);
            rpm[i] = Math.min(INITIAL_RPM, c.maxWheelRpm);
            hood[i] = (c.servoAtMinAngle + c.servoAtMaxAngle) / 2;
            calibration[i] = loaded.message;
        }
        init_loop();
    }

    @Override
    public void init_loop() {
        telemetry.addLine("Simple TeleOp: robot-centric drive and manual hood/RPM. No Pinpoint needed.");
        telemetry.addData("Pollen calibration", calibration[0]);
        telemetry.addData("Nectar calibration", calibration[1]);
        telemetry.addLine("G2 A/B selects shooter. D-pad U/D: RPM; L/R: hood position.");
        telemetry.addLine("G2 LB: intake | RB: spin up | RT: shoot/open gate | X: reverse");
        telemetry.addLine("Aim manually with G1. Wait for hood/RPM before feeding.");
        telemetry.update();
    }

    @Override
    public void start() {
        clock.reset();
    }

    @Override
    public void loop() {
        drive.drive(
                Range.clip(-gamepad1.left_stick_y, -RobotConstants.MAX_DRIVE_POWER, RobotConstants.MAX_DRIVE_POWER),
                Range.clip(-gamepad1.left_stick_x, -RobotConstants.MAX_DRIVE_POWER, RobotConstants.MAX_DRIVE_POWER),
                Range.clip(-gamepad1.right_stick_x, -RobotConstants.MAX_TURN_POWER, RobotConstants.MAX_TURN_POWER));

        int choice = gamepad2.b ? 1 : gamepad2.a ? 0 : selected;
        if (choice != selected) {
            selected = choice;
            previousRpmDirection = previousHoodDirection = 0;
        }
        ShooterConfig c = settings[selected];
        double now = clock.seconds();
        int rpmDirection = gamepad2.dpad_up ? 1 : gamepad2.dpad_down ? -1 : 0;
        if (rpmDirection != 0 && (rpmDirection != previousRpmDirection || now >= nextRpmAdjustment)) {
            rpm[selected] = Range.clip(rpm[selected] + rpmDirection * RPM_STEP, 0, c.maxWheelRpm);
            nextRpmAdjustment = now + (rpmDirection == previousRpmDirection ? 0.12 : 0.4);
        }
        previousRpmDirection = rpmDirection;

        int hoodDirection = gamepad2.dpad_right ? 1 : gamepad2.dpad_left ? -1 : 0;
        boolean hoodChanged = hoodDirection != 0
                && (hoodDirection != previousHoodDirection || now >= nextHoodAdjustment);
        if (hoodChanged) {
            hood[selected] = Range.clip(hood[selected] + hoodDirection * HOOD_STEP,
                    Math.min(c.servoAtMinAngle, c.servoAtMaxAngle),
                    Math.max(c.servoAtMinAngle, c.servoAtMaxAngle));
            nextHoodAdjustment = now + (hoodDirection == previousHoodDirection ? 0.12 : 0.4);
        }
        previousHoodDirection = hoodDirection;

        boolean reverse = gamepad2.x;
        boolean shoot = gamepad2.right_trigger > 0.1 && !reverse;
        boolean spin = (gamepad2.right_bumper || shoot) && !reverse;
        Turret active = turrets[selected];
        turrets[1 - selected].stop();
        boolean commandValid = false;
        if (!reverse && (spin || hoodChanged)) {
            // Direct servo/RPM control; no pose, trajectory solver, or path follower is used.
            commandValid = active.tune(hood[selected], spin ? rpm[selected] : 0);
        } else active.stop();

        boolean releaseBalls = shoot && commandValid && active.getCommandedRpm() > 0;
        active.setGateOpen(releaseBalls);
        boolean feed = gamepad2.left_bumper || releaseBalls;
        intakeTransfer.setPower(reverse ? -1 : feed ? 1 : 0);

        telemetry.addData("Selected", selected == 0 ? "Pollen" : "Nectar");
        telemetry.addData("Manual RPM / hood position", "%.0f / %.3f", rpm[selected], hood[selected]);
        telemetry.addData("Hood position limits", "%.3f to %.3f",
                Math.min(c.servoAtMinAngle, c.servoAtMaxAngle), Math.max(c.servoAtMinAngle, c.servoAtMaxAngle));
        telemetry.addData("Pollen RPM commanded / measured", "%.0f / %.0f",
                turrets[0].getCommandedRpm(), turrets[0].getMeasuredRpm());
        telemetry.addData("Nectar RPM commanded / measured", "%.0f / %.0f",
                turrets[1].getCommandedRpm(), turrets[1].getMeasuredRpm());
        telemetry.addData("Pollen / nectar gates", "%s / %s",
                turrets[0].isGateOpen() ? "Open" : "Closed", turrets[1].isGateOpen() ? "Open" : "Closed");
        telemetry.addData("Intake / transfer", reverse ? "Reverse" : feed ? "Forward" : "Stopped");
        if (spin && !commandValid) telemetry.addLine("Invalid manual command: check encoder/gearing and RPM settings.");
        telemetry.addLine("G1 sticks: drive/turn relative to robot. Aim manually.");
        telemetry.addLine("G2 A/B: pollen/nectar | D-pad U/D: +/-50 RPM | R/L: +/-0.01 hood");
        telemetry.addLine("G2 LB: intake | RB: spin up | RT: shoot | X: reverse");
        telemetry.addLine("RT feeds immediately; wait for hood/RPM to settle. Settings last for this run only.");
        telemetry.update();
    }

    @Override
    public void stop() {
        if (drive != null) drive.stop();
        if (intakeTransfer != null) intakeTransfer.stop();
        for (Turret turret : turrets) if (turret != null) turret.stop();
    }
}
