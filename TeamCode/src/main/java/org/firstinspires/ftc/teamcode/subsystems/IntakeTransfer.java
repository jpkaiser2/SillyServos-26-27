package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.config.HardwareNames;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

// Runs the nectar intake/feeder and pollen transfer together.
public final class IntakeTransfer {
    private final DcMotor intake;
    private final DcMotor pollenTransfer;
    private final CRServo transferLeft;
    private final CRServo transferRight;

    public IntakeTransfer(HardwareMap hardwareMap) {
        intake = hardwareMap.get(DcMotor.class, HardwareNames.INTAKE_MAIN);
        pollenTransfer = hardwareMap.get(DcMotor.class, HardwareNames.INTAKE_SMALL);
        transferLeft = hardwareMap.get(CRServo.class, HardwareNames.POLLEN_TRANSFER_LEFT);
        transferRight = hardwareMap.get(CRServo.class, HardwareNames.POLLEN_TRANSFER_RIGHT);

        intake.setDirection(RobotConstants.INTAKE_DIRECTION);
        pollenTransfer.setDirection(RobotConstants.POLLEN_TRANSFER_DIRECTION);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        pollenTransfer.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        pollenTransfer.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        transferLeft.setDirection(RobotConstants.TRANSFER_LEFT_SERVO_DIRECTION);
        transferRight.setDirection(RobotConstants.TRANSFER_LEFT_SERVO_DIRECTION
                == DcMotorSimple.Direction.FORWARD
                ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);
        stop();
    }

    // Positive runs intake/feed, negative reverses both, and zero stops everything.
    public void setPower(double power) {
        power = Range.clip(power, -1, 1);
        double transferPower = power * RobotConstants.POLLEN_TRANSFER_POWER;
        intake.setPower(power * RobotConstants.INTAKE_POWER);
        pollenTransfer.setPower(transferPower);
        double servoPower = transferPower == 0 ? 0 : power * RobotConstants.TRANSFER_SERVO_POWER;
        // Opposite configured directions turn these servos in opposite directions.
        transferLeft.setPower(servoPower);
        transferRight.setPower(servoPower);
    }

    public void stop() {
        setPower(0);
    }
}
