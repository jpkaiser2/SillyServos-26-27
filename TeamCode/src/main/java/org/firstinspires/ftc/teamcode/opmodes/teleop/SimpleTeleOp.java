package org.firstinspires.ftc.teamcode.opmodes.teleop;


import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.config.HardwareNames;
import org.firstinspires.ftc.teamcode.verydumbpathing.drivetrain.DrivetrainInfo;
import org.firstinspires.ftc.teamcode.verydumbpathing.drivetrain.Mecanum;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

@TeleOp(name = "Very Dumb TeleOp TeleOp", group = "Template")
public class SimpleTeleOp extends OpMode {
    double forward;
    double strafe;
    double turn;
    Mecanum drive;

    @Override
    public void init() {
        String[] motor_names = {HardwareNames.LEFT_FRONT_DRIVE, HardwareNames.RIGHT_FRONT_DRIVE, HardwareNames.LEFT_BACK_DRIVE, HardwareNames.RIGHT_BACK_DRIVE};
        drive = new Mecanum(motor_names);

        // create drivetrain info for Mecanum drivetrain object
        DrivetrainInfo drivetrain = new DrivetrainInfo();
        drivetrain.horizontal_distance = (403.809 + 380.150 ) / 2;
        drivetrain.horizontal_distance /= 10; // to cm

        drivetrain.lengthwise_distance = 260.036;
        drivetrain.lengthwise_distance /= 10; // to cm

        drivetrain.pulses_per_rotation = 1425.1; // set pulses per rotation for 117 rpm Yellow Jacket Motor
        drivetrain.wheel_radius = 9.2;

        drive.drivetrain = drivetrain;

        telemetry.addLine("Very Dumb TeleOp ready");
        telemetry.update();
    }

    @Override
    public void start() {
        drive.active = true;
    }

    @Override
    public void loop() {
        take_input();
        drive.set_motor_power(forward, strafe, turn);
    }

    void take_input() {
        forward = Range.clip(-gamepad1.left_stick_y, -RobotConstants.MAX_DRIVE_POWER, RobotConstants.MAX_DRIVE_POWER);
        strafe = Range.clip(-gamepad1.left_stick_x, -RobotConstants.MAX_DRIVE_POWER, RobotConstants.MAX_DRIVE_POWER);
        turn = Range.clip(-gamepad1.right_stick_x, -RobotConstants.MAX_TURN_POWER, RobotConstants.MAX_TURN_POWER);
    }

    @Override
    public void stop() {
        drive.active = false;
    }
}
