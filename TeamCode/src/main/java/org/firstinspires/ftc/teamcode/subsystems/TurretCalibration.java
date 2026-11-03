package org.firstinspires.ftc.teamcode.subsystems;

/** Fits the existing projectile model to successful shots. All distances are meters. */
public final class TurretCalibration {
    private static final double GRAVITY = 9.81;

    private TurretCalibration() { }

    public static final class Sample {
        public final double distance, height, rpm, bearingOffset, servo;

        public Sample(double distance, double height, double rpm, double bearingOffset, double servo) {
            this.distance = distance;
            this.height = height;
            this.rpm = rpm;
            this.bearingOffset = bearingOffset;
            this.servo = servo;
        }
    }

    public static final class Fit {
        public final double angle, correction;

        private Fit(double angle, double correction) {
            this.angle = angle;
            this.correction = correction;
        }
    }

    public static final class Result {
        public final double minAngle, maxAngle, servoAtMin, servoAtMax, correction, headingOffset;

        private Result(Fit low, Fit high, Sample[] samples, double correction, double headingOffset) {
            minAngle = low.angle;
            maxAngle = high.angle;
            servoAtMin = samples[0].servo;
            servoAtMax = samples[2].servo;
            this.correction = correction;
            this.headingOffset = headingOffset;
        }
    }

    /**
     * At a fixed hood, h = d*tan(theta) - b*k^2*(1 + tan(theta)^2),
     * where b = g*d^2/(2*(pi*D*RPM/60)^2). Two shots give two linear equations
     * for tan(theta) and k^2*(1 + tan(theta)^2). Equal-height targets cannot identify
     * both quantities; closely spaced/ill-conditioned observations are also rejected.
     */
    public static Fit fitPair(Sample first, Sample second, double diameter) {
        validate(first);
        validate(second);
        require(Double.isFinite(diameter) && diameter > 0, "Check wheel diameter.");
        require(Math.abs(first.servo - second.servo) < 1e-6,
                "Keep exactly the same hood position for both shots in a pair.");
        require(Math.abs(first.height - second.height) < 1e-6,
                "Target height changed. Restart this pair.");
        require(Math.abs(first.distance - second.distance) >= 0.3048 - 1e-9,
                "Move at least 12 inches nearer/farther for the second shot.");
        double b1 = dropCoefficient(first, diameter);
        double b2 = dropCoefficient(second, diameter);
        double determinant = first.distance * b2 - second.distance * b1;
        double scale = Math.max(Math.abs(first.distance * b2), Math.abs(second.distance * b1));
        require(Double.isFinite(determinant) && Math.abs(determinant) > 0.02 * scale,
                "Shots are too similar to fit. Use a higher target or wider distance spacing.");
        double tangent = (first.height * b2 - second.height * b1) / determinant;
        double q = (second.distance * first.height - first.distance * second.height) / determinant;
        double angle = Math.atan(tangent);
        double correction = Math.sqrt(q / (1 + tangent * tangent));
        require(Double.isFinite(angle) && angle > 0 && angle < Math.toRadians(85)
                        && Double.isFinite(correction) && correction > 0,
                "Shots do not give a usable upward launch angle. Check hits, height and distances.");
        return new Fit(angle, correction);
    }

    public static Result fit(Sample[] samples, double diameter) {
        require(samples != null && samples.length == 4, "Record four successful shots.");
        Fit low = fitPair(samples[0], samples[1], diameter);
        Fit high = fitPair(samples[2], samples[3], diameter);
        require(high.angle - low.angle >= Math.toRadians(5),
                "The second hood must launch at least 5 degrees higher. Undo and choose a higher setting.");
        require(Math.abs(samples[0].servo - samples[2].servo) >= 0.01,
                "Use two different hood positions.");
        double correction = (low.correction + high.correction) / 2;
        require(Math.abs(low.correction - high.correction) / correction <= 0.15,
                "The two hood fits disagree. Repeat hits/check height; one correction may not fit this mechanism.");
        double x = 0, y = 0;
        for (Sample sample : samples) {
            x += Math.cos(sample.bearingOffset);
            y += Math.sin(sample.bearingOffset);
        }
        double headingOffset = Math.atan2(y, x);
        for (int i = 0; i < samples.length; i++) {
            Sample sample = samples[i];
            double angle = i < 2 ? low.angle : high.angle;
            double predictedHeight = sample.distance * Math.tan(angle)
                    - dropCoefficient(sample, diameter) * correction * correction
                    / (Math.cos(angle) * Math.cos(angle));
            require(Math.abs(predictedHeight - sample.height) <= 0.1016,
                    "Combined fit misses the target height by over 4 inches. Repeat more consistent hits.");
            double error = Math.atan2(Math.sin(sample.bearingOffset - headingOffset),
                    Math.cos(sample.bearingOffset - headingOffset));
            require(Math.abs(error) <= Math.toRadians(5),
                    "Horizontal aim differs between hits. Check target mark/odometry and repeat.");
        }
        return new Result(low, high, samples, correction, headingOffset);
    }

    private static double dropCoefficient(Sample sample, double diameter) {
        double surfaceSpeed = Math.PI * diameter * sample.rpm / 60;
        return GRAVITY * sample.distance * sample.distance / (2 * surfaceSpeed * surfaceSpeed);
    }

    private static void validate(Sample sample) {
        require(sample != null && Double.isFinite(sample.distance) && sample.distance >= 0.3048 - 1e-9
                        && Double.isFinite(sample.height) && sample.height >= 0.1524 - 1e-9
                        && Double.isFinite(sample.rpm) && sample.rpm > 0
                        && Double.isFinite(sample.bearingOffset)
                        && Double.isFinite(sample.servo) && sample.servo >= 0 && sample.servo <= 1,
                "Need a valid hit at least 12 inches away, with the target at least 6 inches above launch.");
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
