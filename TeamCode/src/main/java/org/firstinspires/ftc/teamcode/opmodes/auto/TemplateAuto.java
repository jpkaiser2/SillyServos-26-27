package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroMecanumDrive;

@Autonomous(name = "Template Auto", group = "Template")
public class TemplateAuto extends LinearOpMode {
    @Override
    public void runOpMode() {
        PedroMecanumDrive drive = new PedroMecanumDrive(hardwareMap);
        drive.setStartingPose(new Pose(72, 72, 0));

        telemetry.addLine("Template auto ready");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        drive.startTeleOpDrive();

        ElapsedTime timer = new ElapsedTime();
        while (opModeIsActive() && timer.seconds() < RobotConstants.AUTO_FORWARD_SECONDS) {
            drive.setDrivePowers(RobotConstants.AUTO_FORWARD_POWER, 0.0, 0.0, true);
            drive.update();

            telemetry.addData("Auto", "Driving forward");
            telemetry.update();
        }

        drive.stop();
    }
}
