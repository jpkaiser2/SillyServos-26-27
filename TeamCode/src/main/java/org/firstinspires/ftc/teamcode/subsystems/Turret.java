package org.firstinspires.ftc.teamcode.subsystems;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.config.ShooterConfig;

/** One flywheel and adjustable hood. Horizontal aiming is done by the chassis. */
public final class Turret {
    private final DcMotorEx flywheel;
    private final Servo hood;
    private final ShooterConfig config;
    private ShooterMath.Shot shot;
    private double commandedRpm;

    public Turret(HardwareMap hardwareMap, ShooterConfig config) {
        this.config = config;
        flywheel = hardwareMap.get(DcMotorEx.class, config.flywheelName);
        hood = hardwareMap.get(Servo.class, config.hoodName);
        flywheel.setDirection(config.motorDirection);
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        stop();
    }

    /** Call every loop, even when stopped, so aim follows the current odometry pose. */
    public void update(Pose pose, boolean spin) {
        // Pedro uses inches; the PDF uses meters. Heading is already in radians.
        shot = ShooterMath.solve(pose.getX() * 0.0254, pose.getY() * 0.0254,
                pose.getHeading(), config);
        if (!spin || shot == null) {
            stop();
            return;
        }
        hood.setPosition(ShooterMath.hoodPosition(shot.angle, config));
        commandedRpm = shot.wheelRpm;
        flywheel.setVelocity(ShooterMath.motorTicksPerSecond(commandedRpm, config));
    }

    public ShooterMath.Shot getShot() {
        return shot;
    }

    public double getCommandedRpm() {
        return commandedRpm;
    }

    public double getMeasuredRpm() {
        return ShooterMath.wheelRpm(flywheel.getVelocity(), config);
    }

    public void stop() {
        commandedRpm = 0;
        flywheel.setVelocity(0);
        flywheel.setPower(0);
    }
}
