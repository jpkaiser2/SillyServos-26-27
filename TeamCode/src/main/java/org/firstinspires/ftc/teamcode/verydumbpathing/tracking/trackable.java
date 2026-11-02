package org.firstinspires.ftc.teamcode.verydumbpathing.tracking;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public interface trackable {
    public double x(DistanceUnit unit);
    public double y(DistanceUnit unit);
    public double yaw();
    public boolean active();
    public int priority();
}
