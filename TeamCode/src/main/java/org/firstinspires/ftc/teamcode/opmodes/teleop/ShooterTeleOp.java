package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.config.FieldConfig;
import org.firstinspires.ftc.teamcode.config.ShooterConfig;
import org.firstinspires.ftc.teamcode.config.ShooterCalibrationStore;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroMecanumDrive;
import org.firstinspires.ftc.teamcode.subsystems.IntakeTransfer;
import org.firstinspires.ftc.teamcode.subsystems.ShooterMath;
import org.firstinspires.ftc.teamcode.subsystems.Turret;

// TemplateTeleOp drive controls plus independent pollen/nectar shooter control.
@TeleOp(name = "Shooter TeleOp", group = "Silly Servos")
public class ShooterTeleOp extends OpMode {
    private PedroMecanumDrive drive;
    private Turret pollen;
    private Turret nectar;
    private IntakeTransfer intakeTransfer;
    private boolean selectPollen = true;
    private final FieldConfig.StartPosition[] starts = FieldConfig.StartPosition.values();
    private int startIndex = 0;
    private boolean previousLeft;
    private boolean previousRight;
    private boolean fieldReady;
    private String pollenCalibration;
    private String nectarCalibration;

    @Override
    public void init() {
        drive = new PedroMecanumDrive(hardwareMap);
        ShooterCalibrationStore.Loaded pollenSettings = ShooterCalibrationStore.load(ShooterConfig.POLLEN);
        ShooterCalibrationStore.Loaded nectarSettings = ShooterCalibrationStore.load(ShooterConfig.NECTAR);
        pollen = new Turret(hardwareMap, pollenSettings.config);
        nectar = new Turret(hardwareMap, nectarSettings.config);
        pollenCalibration = pollenSettings.message;
        nectarCalibration = nectarSettings.message;
        intakeTransfer = new IntakeTransfer(hardwareMap);
        showStartSelection();
    }

    @Override
    public void init_loop() {
        // Only change starts before Play, once per D-pad press.
        if (gamepad1.dpad_right && !previousRight) startIndex = (startIndex + 1) % starts.length;
        if (gamepad1.dpad_left && !previousLeft) startIndex = (startIndex + starts.length - 1) % starts.length;
        previousRight = gamepad1.dpad_right;
        previousLeft = gamepad1.dpad_left;
        showStartSelection();
    }

    private void showStartSelection() {
        FieldConfig.StartPosition start = starts[startIndex];
        telemetry.addLine("G1 D-pad left/right: select starting position before Play");
        telemetry.addData("Starting position", start.label);
        telemetry.addData("Target alliance", start.alliance);
        telemetry.addData("Pollen calibration", pollenCalibration);
        telemetry.addData("Nectar calibration", nectarCalibration);
        telemetry.addData("Start measured", start.isMeasured());
        telemetry.addData("Pollen hive measured", start.alliance.pollenHive.isMeasured());
        telemetry.addData("Nectar hive measured", start.alliance.nectarHive.isMeasured());
        if (start.isMeasured()) {
            telemetry.addData("Starting pose (in, in, deg)", "%.1f, %.1f, %.1f",
                    start.xInches, start.yInches, start.headingDegrees);
        }
        telemetry.addLine("Enter missing measurements in FieldConfig. Place robot at selected start.");
        telemetry.addLine("G2 A: pollen | B: nectar | RB: spin up | RT: shoot");
        telemetry.addLine("G2 hold LB: intake | hold X: reverse intake/transfer");
        telemetry.addLine("G1 sticks: drive | hold LB: stop translation and aim chassis");
        telemetry.update();
    }

    @Override
    public void start() {
        FieldConfig.StartPosition start = starts[startIndex];
        fieldReady = start.isMeasured() && start.alliance.pollenHive.isMeasured()
                && start.alliance.nectarHive.isMeasured();
        if (!fieldReady) {
            telemetry.addLine("Cannot start: selected pose or alliance hive measurements are missing in FieldConfig.");
            telemetry.update();
            requestOpModeStop();
            return;
        }
        // Seed field-relative odometry once. Never reset it as the robot drives.
        drive.setStartingPose(start.toPedroPose());
        drive.startTeleOpDrive();
    }

    @Override
    public void loop() {
        if (!fieldReady) return;
        if (gamepad2.a) selectPollen = true;
        if (gamepad2.b) selectPollen = false;
        // Apply drive commands and refresh odometry once per loop before solving.
        drive.update();
        Pose pose = drive.getPose();
        boolean reverse = gamepad2.x;
        boolean shoot = gamepad2.right_trigger > 0.1 && !reverse;
        boolean spin = (gamepad2.right_bumper || shoot) && !reverse;
        FieldConfig.Alliance alliance = starts[startIndex].alliance;
        pollen.update(pose, alliance.pollenHive, selectPollen && spin);
        nectar.update(pose, alliance.nectarHive, !selectPollen && spin);
        FieldConfig.Target hive = selectPollen ? alliance.pollenHive : alliance.nectarHive;
        ShooterMath.Shot shot = (selectPollen ? pollen : nectar).getShot();
        // Intake and shooting run BOTH feed paths. Reverse takes priority.
        // A rejected shot cannot feed by itself; LB still allows ordinary intake.
        boolean feed = gamepad2.left_bumper || (shoot && shot != null);
        // Both feed paths run, but only the selected shooter's gate releases balls.
        pollen.setGateOpen(selectPollen && shoot && shot != null);
        nectar.setGateOpen(!selectPollen && shoot && shot != null);
        intakeTransfer.setPower(reverse ? -1 : feed ? 1 : 0);

        double forward = Range.clip(-gamepad1.left_stick_y,
                -RobotConstants.MAX_DRIVE_POWER, RobotConstants.MAX_DRIVE_POWER);
        double strafe = Range.clip(-gamepad1.left_stick_x,
                -RobotConstants.MAX_DRIVE_POWER, RobotConstants.MAX_DRIVE_POWER);
        double turn = Range.clip(-gamepad1.right_stick_x,
                -RobotConstants.MAX_TURN_POWER, RobotConstants.MAX_TURN_POWER);
        if (gamepad1.left_bumper) {
            forward = 0;
            strafe = 0;
            turn = shot == null ? 0 : Range.clip(RobotConstants.AIM_KP * shot.headingError,
                    -RobotConstants.MAX_AIM_POWER, RobotConstants.MAX_AIM_POWER);
            if (shot != null && Math.abs(shot.headingError) < RobotConstants.AIM_TOLERANCE_RADIANS) {
                turn = 0;
            }
        }
        drive.setDrivePowers(forward, strafe, turn, RobotConstants.FIELD_CENTRIC_TELEOP);

        telemetry.addData("Selected", selectPollen ? "Pollen" : "Nectar");
        telemetry.addData("Starting position / alliance", "%s / %s", starts[startIndex].label, alliance);
        telemetry.addData("Hive (in, in, height in)", "%.1f, %.1f, %.1f",
                hive.xInches, hive.yInches, hive.heightInches);
        telemetry.addData("Robot-to-hive distance (m)", "%.2f", Math.hypot(
                hive.xInches - pose.x(), hive.yInches - pose.y()) * FieldConfig.METERS_PER_INCH);
        telemetry.addData("Intake / transfer", reverse ? "Reverse" : feed ? "Forward" : "Stopped");
        telemetry.addData("Pollen / nectar gates", "%s / %s",
                pollen.isGateOpen() ? "Open" : "Closed", nectar.isGateOpen() ? "Open" : "Closed");
        telemetry.addData("Pose (in, in, deg)", "%.1f, %.1f, %.1f",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        telemetry.addData("Pollen RPM commanded / measured", "%.0f / %.0f",
                pollen.getCommandedRpm(), pollen.getMeasuredRpm());
        telemetry.addData("Nectar RPM commanded / measured", "%.0f / %.0f",
                nectar.getCommandedRpm(), nectar.getMeasuredRpm());
        if (shot == null) {
            telemetry.addLine("No valid shot: check geometry, hood limits and RPM limit.");
        } else {
            telemetry.addData("Launch-to-hive distance (m)", "%.2f", shot.distance);
            telemetry.addData("Launch angle (deg)", "%.1f", Math.toDegrees(shot.angle));
            telemetry.addData("Aim error (deg)", "%.1f", Math.toDegrees(shot.headingError));
        }
        telemetry.addLine("G2 LB: intake | RB: spin up | RT: shoot | X: reverse");
        telemetry.addLine("Spin up with RB, then hold RT once the robot, hood and RPM settle.");
        telemetry.update();
    }

    @Override
    public void stop() {
        if (intakeTransfer != null) intakeTransfer.stop();
        if (pollen != null) pollen.stop();
        if (nectar != null) nectar.stop();
        if (drive != null) drive.stop();
    }
}
