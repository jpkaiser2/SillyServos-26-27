package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class PedroMecanumDrive {
    private final Follower follower;

    public PedroMecanumDrive(HardwareMap hardwareMap) {
        FollowerConstants followerConstants = new FollowerConstants();
        PathConstraints pathConstraints = new PathConstraints(0.99, 100, 1, 1);

        follower = new FollowerBuilder(followerConstants, hardwareMap)
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
        follower.setTeleOpDrive(forward, strafe, turn, fieldCentric);
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
