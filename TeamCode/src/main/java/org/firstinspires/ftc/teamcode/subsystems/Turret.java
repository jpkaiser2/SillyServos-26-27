package org.firstinspires.ftc.teamcode.subsystems;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.config.ShooterConfig;
import org.firstinspires.ftc.teamcode.config.FieldConfig;

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
    public void update(Pose pose, FieldConfig.Target target, boolean spin) {
        // Pedro uses inches; the PDF uses meters. Heading is already in radians.
        shot = ShooterMath.solve(pose.x() * FieldConfig.METERS_PER_INCH,
                pose.y() * FieldConfig.METERS_PER_INCH, pose.heading(), config, target);
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

    /** Direct hood/RPM control for calibration. The operator must explicitly hold its control. */
    public boolean tune(double hoodPosition, double wheelRpm) {
        if (!Double.isFinite(hoodPosition) || hoodPosition < 0 || hoodPosition > 1
                || !Double.isFinite(wheelRpm) || wheelRpm < 0
                || !Double.isFinite(config.maxWheelRpm) || wheelRpm > config.maxWheelRpm
                || !Double.isFinite(config.wheelToMotorRatio) || config.wheelToMotorRatio <= 0
                || !Double.isFinite(config.ticksPerMotorRevolution)
                || config.ticksPerMotorRevolution <= 0) {
            stop();
            return false;
        }
        hood.setPosition(hoodPosition);
        commandedRpm = wheelRpm;
        if (wheelRpm == 0) stop();
        else flywheel.setVelocity(ShooterMath.motorTicksPerSecond(wheelRpm, config));
        return true;
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
