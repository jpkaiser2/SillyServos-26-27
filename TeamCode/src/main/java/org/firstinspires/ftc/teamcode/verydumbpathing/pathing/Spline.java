package org.firstinspires.ftc.teamcode.verydumbpathing.pathing;

import org.firstinspires.ftc.teamcode.verydumbpathing.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// bezier curve implementation
public class Spline {
    Spline Subspline1;
    Spline Subspline2;
    private Point pt; // if defined, sets the spline to a fixed point

    public Spline(List<Point> pts) {
        if (pts.size() == 1) {
            pt = pts.get(0);
            return;
        } else {

            List<Point> ptSublist1 = pts.subList(0, pts.size() - 1);
            List<Point> ptSublist2 = pts.subList(1, pts.size());

            Subspline1 = new Spline(ptSublist1);
            Subspline2 = new Spline(ptSublist2);
        }
    }
    public Point get_point(double t) {
        if (pt != null) {
           return pt;
        } else {
            return this.interpolate(Subspline1.get_point(t), Subspline2.get_point(t), t);
        }

    }

    public Point interpolate(Point a, Point b, double t) {
        return new Point(a.x*t + b.x*(1-t), a.y*t + b.y*(1-t));
    }
}

