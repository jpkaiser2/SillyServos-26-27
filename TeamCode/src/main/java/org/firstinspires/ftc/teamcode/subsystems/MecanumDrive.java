package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.config.HardwareNames;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

// Standard robot-centric mecanum drive. No localizer, IMU, or path follower required.
public final class MecanumDrive {
    private final DcMotor frontLeft, frontRight, backLeft, backRight;

    public MecanumDrive(HardwareMap hardwareMap) {
        frontLeft = hardwareMap.get(DcMotor.class, HardwareNames.LEFT_FRONT_DRIVE);
        frontRight = hardwareMap.get(DcMotor.class, HardwareNames.RIGHT_FRONT_DRIVE);
        backLeft = hardwareMap.get(DcMotor.class, HardwareNames.LEFT_BACK_DRIVE);
        backRight = hardwareMap.get(DcMotor.class, HardwareNames.RIGHT_BACK_DRIVE);
        frontLeft.setDirection(RobotConstants.LEFT_DRIVE_DIRECTION);
        backLeft.setDirection(RobotConstants.LEFT_DRIVE_DIRECTION);
        frontRight.setDirection(RobotConstants.RIGHT_DRIVE_DIRECTION);
        backRight.setDirection(RobotConstants.RIGHT_DRIVE_DIRECTION);
        for (DcMotor motor : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            motor.setPower(0);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
    }

    // Forward, strafe LEFT, and counterclockwise turn are positive.
    public void drive(double forward, double strafe, double turn) {
        forward = Range.clip(forward, -1, 1);
        strafe = Range.clip(strafe, -1, 1);
        turn = Range.clip(turn, -1, 1);
        // Normalize all wheels together, preserving the requested direction when mixing inputs.
        double scale = Math.max(1, Math.abs(forward) + Math.abs(strafe) + Math.abs(turn));
        frontLeft.setPower((forward - strafe - turn) / scale);
        frontRight.setPower((forward + strafe + turn) / scale);
        backLeft.setPower((forward + strafe - turn) / scale);
        backRight.setPower((forward - strafe + turn) / scale);
    }

    public void stop() {
        drive(0, 0, 0);
    }
}
