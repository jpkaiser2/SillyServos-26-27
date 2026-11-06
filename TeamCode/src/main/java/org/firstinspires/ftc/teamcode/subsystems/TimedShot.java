package org.firstinspires.ftc.teamcode.subsystems;

// Readiness dwell, gate delay, and accumulated feed time for one autonomous shooter.
public final class TimedShot {
    private final double settleSeconds, gateSeconds, feedSeconds;
    private double readySince = Double.NaN;
    private double gateSince = Double.NaN;
    private double previousTime = Double.NaN;
    private double fedSeconds;
    private boolean gateOpen, feeding, complete;

    public TimedShot(double settleSeconds, double gateSeconds, double feedSeconds) {
        if (!Double.isFinite(settleSeconds) || settleSeconds < 0
                || !Double.isFinite(gateSeconds) || gateSeconds < 0
                || !Double.isFinite(feedSeconds) || feedSeconds <= 0) {
            throw new IllegalArgumentException("Invalid autonomous shot timing.");
        }
        this.settleSeconds = settleSeconds;
        this.gateSeconds = gateSeconds;
        this.feedSeconds = feedSeconds;
    }

    public void update(double now, boolean ready) {
        if (!Double.isFinite(now) || (!Double.isNaN(previousTime) && now < previousTime)) {
            throw new IllegalArgumentException("Shot clock must be finite and monotonic.");
        }
        boolean wasFeeding = feeding;
        gateOpen = feeding = false;
        if (!complete && ready) {
            if (Double.isNaN(readySince)) readySince = now;
            if (now - readySince >= settleSeconds) {
                if (Double.isNaN(gateSince)) gateSince = now;
                gateOpen = true;
                feeding = now - gateSince >= gateSeconds;
                // Count only intervals that started and ended ready with the feeders running.
                if (wasFeeding && feeding) fedSeconds += now - previousTime;
                if (fedSeconds >= feedSeconds) {
                    complete = true;
                    gateOpen = feeding = false;
                }
            }
        } else {
            readySince = gateSince = Double.NaN;
        }
        previousTime = now;
    }

    public boolean isGateOpen() { return gateOpen; }
    public boolean isFeeding() { return feeding; }
    public boolean isComplete() { return complete; }
    public double getFedSeconds() { return fedSeconds; }
}
