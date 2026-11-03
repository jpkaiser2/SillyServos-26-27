package org.firstinspires.ftc.teamcode.verydumbpathing.pathing;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

import java.util.List;

// bezier curve implementation
public class Spline {
    Spline Subspline1;
    Spline Subspline2;
    private Pose2D pt; // if defined, sets the spline to a fixed point

    public Spline(List<Pose2D> pts) {
        if (pts.size() == 1) {
            pt = pts.get(0);
            return;
        } else {

            List<Pose2D> ptSublist1 = pts.subList(0, pts.size() - 1);
            List<Pose2D> ptSublist2 = pts.subList(1, pts.size());

            Subspline1 = new Spline(ptSublist1);
            Subspline2 = new Spline(ptSublist2);
        }
    }
    public Pose2D get_point(double t) {
        if (pt != null) {
           return pt;
        } else {
            return this.interpolate(Subspline1.get_point(t), Subspline2.get_point(t), t);
        }

    }

    public Pose2D interpolate(Pose2D a, Pose2D b, double t) {
        return new Pose2D(
                DistanceUnit.MM,
                linearSpline(a.getX(DistanceUnit.MM), b.getX(DistanceUnit.MM),t),
                linearSpline(a.getY(DistanceUnit.MM), b.getY(DistanceUnit.MM),t),
                AngleUnit.DEGREES,
                linearSpline(a.getHeading(AngleUnit.DEGREES), b.getHeading(AngleUnit.DEGREES),t)
        );


    }

    public Pose2D startingPoint() {
        if (pt != null) {
            return pt;
        } else {
            return Subspline1.startingPoint();
        }
    }

    public Pose2D endingPoint() {
        if (pt != null) {
            return pt;
        } else {
            return Subspline2.endingPoint();
        }
    }

    public double linearSpline(double a, double b, double t) {
        return a*t + b*(1-t);
    }


}

