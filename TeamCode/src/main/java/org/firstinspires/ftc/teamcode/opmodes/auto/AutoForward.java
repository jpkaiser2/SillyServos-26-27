package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystems.MecanumDrive;

// Timed forward drive using only the four drive motors. No Pedro or odometry.
@Autonomous(name = "autoForward", group = "Silly Servos")
public final class AutoForward extends LinearOpMode {
    private static final double DRIVE_POWER = 0.3;
    // Starting estimate only: tune on the robot until it travels about 12 inches.
    // Increase if it stops short; decrease if it travels too far.
    private static final double DRIVE_SECONDS = 0.6;

    @Override
    public void runOpMode() {
        MecanumDrive drive = new MecanumDrive(hardwareMap);
        try {
            telemetry.addLine("autoForward ready: short timed forward drive.");
            telemetry.addLine("Tune DRIVE_SECONDS for approximately one foot.");
            telemetry.update();

            waitForStart();
            if (isStopRequested()) return;

            ElapsedTime timer = new ElapsedTime();
            while (opModeIsActive() && timer.seconds() < DRIVE_SECONDS) {
                drive.drive(DRIVE_POWER, 0, 0);
                idle();
            }
        } finally {
            drive.stop();
        }
    }
}
