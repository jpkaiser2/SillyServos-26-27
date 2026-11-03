package org.firstinspires.ftc.teamcode.verydumbpathing.pathing;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

import java.util.List;

public class Path {

  public List<Spline> splines;
  public List<Routine> routines;
  public double sample_rate = 0.1; // one waypoint every {n} CM assuming linear paths
  public int index;
  public Path() {

  }

  public void addSpline(Spline spline) {
    splines.add(spline);
  }

  public Pose2D getPoint(double target) {
    int spline = (int) Math.floor(target);
    double t = target - (double) spline;

    return splines.get(spline).get_point(t);
  }
}
