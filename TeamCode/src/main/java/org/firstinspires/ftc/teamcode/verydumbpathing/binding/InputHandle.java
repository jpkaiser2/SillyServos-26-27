package org.firstinspires.ftc.teamcode.verydumbpathing.binding;

public interface InputHandle {
  public default double linearValue() {
    return value() ? 1 : 0;
  }

  public default boolean value() {
    return linearValue() > 0.2;
  }
}
