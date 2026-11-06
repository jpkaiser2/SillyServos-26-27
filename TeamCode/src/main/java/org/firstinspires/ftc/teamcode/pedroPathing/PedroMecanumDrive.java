package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class PedroMecanumDrive {
    private final Follower follower;

    public PedroMecanumDrive(HardwareMap hardwareMap) {
        follower = Constants.createManual(hardwareMap);
    }

    public void setStartingPose(Pose pose) {
        follower.setPose(pose);
    }

    public void startTeleOpDrive() {
        follower.manual(0, 0, 0);
    }

    public void setDrivePowers(double forward, double strafe, double turn, boolean fieldCentric) {
        // Preserve the existing controls: strafe left and counterclockwise turn are positive.
        if (fieldCentric) {
            follower.manual(ManualDrive.fieldCentric(forward, strafe, turn, follower.pose().heading()));
        } else {
            follower.manual(forward, strafe, turn);
        }
    }

    public void update() {
        follower.update();
    }

    public void stop() {
        follower.stop();
        follower.drivetrain.stop();
    }

    public Pose getPose() {
        return follower.pose();
    }
}
