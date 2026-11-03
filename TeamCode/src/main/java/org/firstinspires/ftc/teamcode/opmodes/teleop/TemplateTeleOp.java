package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroMecanumDrive;

@TeleOp(name = "Template TeleOp", group = "Template")
public class TemplateTeleOp extends OpMode {
    private PedroMecanumDrive drive;

    @Override
    public void init() {
        drive = new PedroMecanumDrive(hardwareMap);
        telemetry.addLine("Template TeleOp ready");
        telemetry.update();
    }

    @Override
    public void start() {
        // This template uses a local origin; Pinpoint may retain a previous OpMode's pose.
        drive.setStartingPose(Pose.zero());
        drive.startTeleOpDrive();
    }

    @Override
    public void loop() {
        double forward = Range.clip(-gamepad1.left_stick_y, -RobotConstants.MAX_DRIVE_POWER, RobotConstants.MAX_DRIVE_POWER);
        double strafe = Range.clip(-gamepad1.left_stick_x, -RobotConstants.MAX_DRIVE_POWER, RobotConstants.MAX_DRIVE_POWER);
        double turn = Range.clip(-gamepad1.right_stick_x, -RobotConstants.MAX_TURN_POWER, RobotConstants.MAX_TURN_POWER);
        drive.setDrivePowers(forward, strafe, turn, RobotConstants.FIELD_CENTRIC_TELEOP);
        drive.update();

        telemetry.addData("x", drive.getPose().x());
        telemetry.addData("y", drive.getPose().y());
        telemetry.addData("heading", drive.getPose().heading());
        telemetry.update();
    }

    @Override
    public void stop() {
        drive.stop();
    }
}
