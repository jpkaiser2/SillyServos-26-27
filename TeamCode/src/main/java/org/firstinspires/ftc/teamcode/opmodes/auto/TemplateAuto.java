package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;


@Autonomous(name = "Template Auto", group = "Template")
public class TemplateAuto extends OpMode {
    private final PoseFactory p = PoseFactory.degrees();
    private Follower follower;
    private final Pose startPose = p.of(24, 24, 0);
    private final Pose park = p.of(48,48,90);


    @Override
    public void init() {
        follower = Constants.create(hardwareMap);
        follower.setPose(startPose);

        telemetry.addLine("Template auto ready");
        telemetry.update();
    }

    @Override
    public void loop() {

    }
}
