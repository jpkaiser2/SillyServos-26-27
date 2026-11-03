package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.config.ShooterConfig;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroMecanumDrive;
import org.firstinspires.ftc.teamcode.subsystems.IntakeTransfer;
import org.firstinspires.ftc.teamcode.subsystems.ShooterMath;
import org.firstinspires.ftc.teamcode.subsystems.Turret;

/** TemplateTeleOp drive controls plus independent pollen/nectar shooter control. */
@TeleOp(name = "Shooter TeleOp", group = "Silly Servos")
public class ShooterTeleOp extends OpMode {
    private PedroMecanumDrive drive;
    private Turret pollen;
    private Turret nectar;
    private IntakeTransfer intakeTransfer;
    private boolean selectPollen = true;

    @Override
    public void init() {
        drive = new PedroMecanumDrive(hardwareMap);
        drive.setStartingPose(new Pose(RobotConstants.START_X_INCHES,
                RobotConstants.START_Y_INCHES, RobotConstants.START_HEADING_RADIANS));
        pollen = new Turret(hardwareMap, ShooterConfig.POLLEN);
        nectar = new Turret(hardwareMap, ShooterConfig.NECTAR);
        intakeTransfer = new IntakeTransfer(hardwareMap);
        telemetry.addLine("Set measured ShooterConfig / RobotConstants before shooting.");
        telemetry.addLine("G2 A: pollen | B: nectar | RB: spin up | RT: shoot");
        telemetry.addLine("G2 hold LB: intake | hold X: reverse intake/transfer");
        telemetry.addLine("G1 sticks: drive | hold LB: stop translation and aim chassis");
        telemetry.update();
    }

    @Override
    public void start() {
        drive.startTeleOpDrive();
    }

    @Override
    public void loop() {
        if (gamepad2.a) selectPollen = true;
        if (gamepad2.b) selectPollen = false;
        // Apply drive commands and refresh odometry once per loop before solving.
        drive.update();
        Pose pose = drive.getPose();
        boolean reverse = gamepad2.x;
        boolean shoot = gamepad2.right_trigger > 0.1 && !reverse;
        boolean spin = (gamepad2.right_bumper || shoot) && !reverse;
        pollen.update(pose, selectPollen && spin);
        nectar.update(pose, !selectPollen && spin);
        ShooterMath.Shot shot = (selectPollen ? pollen : nectar).getShot();
        // Intake and shooting run BOTH feed paths. Reverse takes priority.
        // A rejected shot cannot feed by itself; LB still allows ordinary intake.
        boolean feed = gamepad2.left_bumper || (shoot && shot != null);
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
        telemetry.addData("Intake / transfer", reverse ? "Reverse" : feed ? "Forward" : "Stopped");
        telemetry.addData("Pose (in, in, deg)", "%.1f, %.1f, %.1f",
                pose.getX(), pose.getY(), Math.toDegrees(pose.getHeading()));
        telemetry.addData("Pollen RPM commanded / measured", "%.0f / %.0f",
                pollen.getCommandedRpm(), pollen.getMeasuredRpm());
        telemetry.addData("Nectar RPM commanded / measured", "%.0f / %.0f",
                nectar.getCommandedRpm(), nectar.getMeasuredRpm());
        if (shot == null) {
            telemetry.addLine("No valid shot: check geometry, hood limits and RPM limit.");
        } else {
            telemetry.addData("Distance (m)", "%.2f", shot.distance);
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
