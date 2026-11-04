package org.firstinspires.ftc.teamcode.opmodes.auto;

import org.firstinspires.ftc.teamcode.config.Constant;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.config.HardwareNames;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.shared.Shooter;


@Autonomous(name = "Template Auto", group = "Template")
public class TemplateAuto extends OpMode {
    private final PoseFactory p = PoseFactory.degrees();
    private Follower follower;
    private final Pose startPose = p.of(24, 24, 0);
    private final Pose park = p.of(48,48,90);

    private Shooter pollen_shooter;
    private Shooter necter_shooter;
    ;
    @Override
    public void init() {
        follower = Constants.create(hardwareMap);
        follower.setPose(startPose);

        Shooter necter_shooter = new Shooter(
                hardwareMap.get(Servo.class, HardwareNames.HOOD_L),
                hardwareMap.get(DcMotorEx.class,HardwareNames.FLYWHEEL_L)
        );
        Shooter pollen_shooter = new Shooter(
                hardwareMap.get(Servo.class, HardwareNames.HOOD_S),
                hardwareMap.get(DcMotorEx.class,HardwareNames.FLYWHEEL_S)
        );

        telemetry.addLine("Template auto ready");
        telemetry.update();
    }

    @Override
    public void start() {
        necter_shooter.setFlywheelTargetRpm(1800, Constant.TICKS_PER_REVOLUTION_FLYWHEEL);
        pollen_shooter.setFlywheelTargetRpm(1800, Constant.TICKS_PER_REVOLUTION_FLYWHEEL);
    }

    @Override
    public void loop() {

    }
}
