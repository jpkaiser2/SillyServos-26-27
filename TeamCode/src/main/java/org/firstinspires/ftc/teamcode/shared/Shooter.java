package org.firstinspires.ftc.teamcode.shared;

import com.pedropathing.controllers.PIDController;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.*;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

public class Shooter {
  private Servo hood;
  private DcMotorEx flywheel;

  public Shooter(Servo hood, DcMotorEx flywheel) {
    this.hood = hood;
    this.flywheel = flywheel;
 }

  public void setFlywheelTargetRpm(double rpm, double ticks_per_rev) {
    flywheel.setVelocity(rpm*ticks_per_rev);
  }

  public abstract class Shoot implements Command {
    public Shoot() {

    }

    @Override
    public void start() {
    }

    @Override
    public void execute() {

    }

    @Override
    public boolean done() {
      return false; // should be a bool to indicate if the robot has shot at the target for this time
    }

    @Override
    public void end(EndCondition endCondition) {
      // ending code
    }
  }
}