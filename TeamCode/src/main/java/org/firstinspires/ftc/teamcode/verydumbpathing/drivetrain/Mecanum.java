package org.firstinspires.ftc.teamcode.verydumbpathing.drivetrain;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

public class Mecanum {
    public boolean active = false; // motors will only move when this bool is set to true
    public DrivetrainInfo drivetrain;
    DcMotor[] motors;
    public Pose2D target;
    public Mecanum(String[] motor_names) {

        motors = new DcMotor[motor_names.length];

        for (int i = 0; i < motor_names.length; i++) {
            motors[i] = hardwareMap.get(DcMotor.class, motor_names[i]);
        }
    }

    // please for the love of all things good use this function or set this.ppr to specify the drivetrains pulses per revolution or I will spontaneously combust
    // dpp = Distance per Pulse in cm

    // finds number of pulses needed per motor to reach the desired position
    public double[] find_pulses(double forward, double strafe, double turn) {
        double distance_per_pulse =  (2 * Math.PI * drivetrain.wheel_radius)/drivetrain.pulses_per_rotation;

        double forward_pulses = forward / distance_per_pulse;
        double strafe_pulses = strafe / distance_per_pulse;

        double turning_radius = Math.sqrt(Math.pow(drivetrain.horizontal_distance, 2) + Math.pow(drivetrain.lengthwise_distance, 2));
        double turning_distance = Math.PI * turning_radius; // the distance wheels need to travel for 180 degrees of rotation

        double turn_pulses = (turn/180 - 1) * (turning_distance / distance_per_pulse);

        double[] pulses = motor_ratio(forward_pulses, strafe_pulses, turn_pulses);

        return pulses;
    }

    public void act(double[] motor_pulses) {
        motors[0].getCurrentPosition();
    }

    public double[] motor_ratio(double forward, double strafe, double turn) {
        double[] wheel_power = new double[4];
        for (int i = 0; i < 4; i++) {
            boolean is_left = i % 2 == 0;
            boolean is_front = Math.floor(i/2.0) == 0;

            double strafe_factor = strafe * (is_front ? -1 : 1);
            double turn_factor = turn * (is_left ? 1 : -1);
            double forward_factor = forward;

            double power_factor = strafe_factor + turn_factor + forward_factor;
            wheel_power[i] = power_factor;
        }
        return wheel_power;
    }

    public double[] motor_power(double forward, double strafe, double turn) {
        // turn is a cw factor

        double[] wheel_power = this.motor_ratio(forward, strafe, turn);
        for (int i = 0; i < 4; i++) {
            double multi = wheel_power[i] >= 0 ? 1 : -1;

            wheel_power[i] = Math.max(Math.abs(wheel_power[i]), RobotConstants.MAX_DRIVE_POWER) * multi;
        }

        return wheel_power;
    }

    public void set_motor_power(double forward, double strafe, double turn) {
        double power[] = this.motor_power(forward, strafe, turn);
        for (int i=0; i < 4; i++) {
            if (active) { // to avoid possible penalties if I mess up later on
                motors[i].setPower(power[i]);
            } else {
                motors[i].setPower(0);
            }
        }
    }
}
