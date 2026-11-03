package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.config.HardwareNames;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

public class PedroMecanumDrive {
    private final Follower follower;

    public PedroMecanumDrive(HardwareMap hardwareMap) {
        FollowerConstants followerConstants = new FollowerConstants();
        PathConstraints pathConstraints = new PathConstraints(0.99, 100, 1, 1);

        follower = new FollowerBuilder(followerConstants, hardwareMap)
                .mecanumDrivetrain(new MecanumConstants()
                        .leftFrontMotorName(HardwareNames.LEFT_FRONT_DRIVE)
                        .leftRearMotorName(HardwareNames.LEFT_BACK_DRIVE)
                        .rightFrontMotorName(HardwareNames.RIGHT_FRONT_DRIVE)
                        .rightRearMotorName(HardwareNames.RIGHT_BACK_DRIVE)
                        .leftFrontMotorDirection(RobotConstants.LEFT_DRIVE_DIRECTION)
                        .leftRearMotorDirection(RobotConstants.LEFT_DRIVE_DIRECTION)
                        .rightFrontMotorDirection(RobotConstants.RIGHT_DRIVE_DIRECTION)
                        .rightRearMotorDirection(RobotConstants.RIGHT_DRIVE_DIRECTION))
                .pinpointLocalizer(new PinpointConstants()
                        .hardwareMapName(HardwareNames.PINPOINT)
                        .forwardPodY(RobotConstants.FORWARD_POD_Y_INCHES)
                        .strafePodX(RobotConstants.STRAFE_POD_X_INCHES)
                        .encoderResolution(RobotConstants.ODOMETRY_PODS)
                        .forwardEncoderDirection(RobotConstants.FORWARD_ENCODER_DIRECTION)
                        .strafeEncoderDirection(RobotConstants.STRAFE_ENCODER_DIRECTION))
                .pathConstraints(pathConstraints)
                .build();
    }

    public void setStartingPose(Pose pose) {
        follower.setStartingPose(pose);
    }

    public void startTeleOpDrive() {
        follower.startTeleopDrive();
    }

    public void setDrivePowers(double forward, double strafe, double turn, boolean fieldCentric) {
        // Pedro 2.x's fourth argument is robotCentric.
        follower.setTeleOpDrive(forward, strafe, turn, !fieldCentric);
    }

    public void update() {
        follower.update();
    }

    public void stop() {
        follower.setTeleOpDrive(0, 0, 0, true);
        follower.update();
    }

    public Pose getPose() {
        return follower.getPose();
    }
}
