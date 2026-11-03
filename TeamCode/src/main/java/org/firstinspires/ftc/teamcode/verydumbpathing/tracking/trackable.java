package org.firstinspires.ftc.teamcode.verydumbpathing.tracking;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public interface trackable {
    public double x();
    public double y();
    public double yaw();
    public boolean active();
    public int priority();
}
