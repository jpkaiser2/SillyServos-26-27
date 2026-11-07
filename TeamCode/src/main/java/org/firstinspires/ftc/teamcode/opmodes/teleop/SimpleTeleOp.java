package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.config.HardwareNames;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.subsystems.IntakeTransfer;
import org.firstinspires.ftc.teamcode.subsystems.MecanumDrive;

// One manually controlled shooter. No Pedro, odometry, gates, or shooter calculations.
@TeleOp(name = "Simple TeleOp", group = "Silly Servos")
public final class SimpleTeleOp extends OpMode {
    // Change these two names if the single shooter uses different configuration names.
    private static final String FLYWHEEL_NAME = HardwareNames.FLYWHEEL_P;
    private static final String ANGLE_SERVO_NAME = HardwareNames.HOOD_P;
    private static final DcMotorSimple.Direction FLYWHEEL_DIRECTION = DcMotorSimple.Direction.FORWARD;

    private static final double INITIAL_FLYWHEEL_POWER = 0.6;
    private static final double POWER_STEP = 0.05;
    // Starting limits for the linkage; adjust to the actual servo's usable travel.
    private static final double ANGLE_MIN = 0.2;
    private static final double ANGLE_MAX = 0.8;
    private static final double ANGLE_START = 0.5;
    private static final double ANGLE_STEP = 0.01;

    private MecanumDrive drive;
    private IntakeTransfer intakeTransfer;
    private DcMotor flywheel;
    private Servo angleServo;
    private double flywheelPower = INITIAL_FLYWHEEL_POWER;
    private double anglePosition = ANGLE_START;
    private boolean previousUp, previousDown, previousLeft, previousRight;

    @Override
    public void init() {
        drive = new MecanumDrive(hardwareMap);
        intakeTransfer = new IntakeTransfer(hardwareMap);
        flywheel = hardwareMap.get(DcMotor.class, FLYWHEEL_NAME);
        angleServo = hardwareMap.get(Servo.class, ANGLE_SERVO_NAME);

        flywheel.setPower(0);
        flywheel.setDirection(FLYWHEEL_DIRECTION);
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        angleServo.setDirection(Servo.Direction.FORWARD);
        angleServo.setPosition(anglePosition);
        showControls();
    }

    @Override
    public void init_loop() {
        showControls();
    }

    @Override
    public void loop() {
        // MecanumDrive takes positive forward, left strafe, and counterclockwise turn.
        drive.drive(
                -gamepad1.left_stick_y * RobotConstants.MAX_DRIVE_POWER,
                -gamepad1.left_stick_x * RobotConstants.MAX_DRIVE_POWER,
                -gamepad1.right_stick_x * RobotConstants.MAX_TURN_POWER);

        // One adjustment per press, independent of the loop speed.
        if (gamepad2.dpad_up && !previousUp) flywheelPower += POWER_STEP;
        if (gamepad2.dpad_down && !previousDown) flywheelPower -= POWER_STEP;
        // Keep zero exact after repeated 0.05 adjustments so RT cannot feed at zero power.
        flywheelPower = Range.clip(Math.round(flywheelPower * 100) / 100.0, 0, 1);
        if (gamepad2.dpad_right && !previousRight) anglePosition += ANGLE_STEP;
        if (gamepad2.dpad_left && !previousLeft) anglePosition -= ANGLE_STEP;
        anglePosition = Range.clip(anglePosition, ANGLE_MIN, ANGLE_MAX);
        angleServo.setPosition(anglePosition);
        previousUp = gamepad2.dpad_up;
        previousDown = gamepad2.dpad_down;
        previousLeft = gamepad2.dpad_left;
        previousRight = gamepad2.dpad_right;

        // Reverse takes priority. RT feeds immediately; use RB to spin up first.
        boolean reverse = gamepad2.x;
        boolean shoot = gamepad2.right_trigger > 0.1 && flywheelPower > 0 && !reverse;
        boolean spin = (gamepad2.right_bumper || shoot) && !reverse;
        boolean feed = gamepad2.left_bumper || shoot;
        flywheel.setPower(spin ? flywheelPower : 0);
        // This runs the intake, transfer motor, and opposite-direction CR servos together.
        intakeTransfer.setPower(reverse ? -1 : feed ? 1 : 0);

        telemetry.addData("Flywheel", "%s | selected power %.2f", spin ? "Running" : "Stopped", flywheelPower);
        telemetry.addData("Angle servo position", "%.2f", anglePosition);
        telemetry.addData("Intake / transfer", reverse ? "Reverse" : feed ? "Forward" : "Stopped");
        showControls();
    }

    private void showControls() {
        telemetry.addLine("G1 left stick: drive/strafe | right stick: turn");
        telemetry.addLine("Hold G2 LB: intake/transfer | RB: spin flywheel | RT: shoot");
        telemetry.addLine("Hold G2 X: reverse intake/transfer and stop flywheel");
        telemetry.addLine("Tap G2 D-pad U/D: flywheel power | R/L: angle servo");
        telemetry.addLine("Spin up with RB before RT; RT feeds immediately.");
        telemetry.update();
    }

    @Override
    public void stop() {
        if (drive != null) drive.stop();
        if (intakeTransfer != null) intakeTransfer.stop();
        if (flywheel != null) flywheel.setPower(0);
    }
}
