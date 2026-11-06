package org.firstinspires.ftc.teamcode.config;

import android.util.AtomicFile;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Robot-local calibration. Source geometry stays authoritative; only fitted values are overridden.
public final class ShooterCalibrationStore {
    public static final String FILE_NAME = "SillyServosShooterCalibration.json";

    private ShooterCalibrationStore() { }

    public static final class Loaded {
        public final ShooterConfig config;
        public final boolean saved;
        public final String message;

        private Loaded(ShooterConfig config, boolean saved, String message) {
            this.config = config;
            this.saved = saved;
            this.message = message;
        }
    }

    public static Loaded load(ShooterConfig defaults) {
        ShooterConfig config = defaults.copy();
        try {
            JSONObject entry = read().optJSONObject(defaults.flywheelName);
            if (entry == null) return new Loaded(config, false, "Using ShooterConfig defaults; no saved calibration.");
            JSONObject geometry = entry.getJSONObject("geometry");
            JSONObject expected = geometry(defaults);
            for (String key : new String[]{"launchHeight", "forwardOffset", "leftOffset", "wheelDiameter",
                    "wheelToMotorRatio", "ticksPerMotorRevolution"}) {
                if (Double.compare(geometry.getDouble(key), expected.getDouble(key)) != 0) {
                    return new Loaded(config, false, "Saved calibration ignored: " + key + " changed. Retune.");
                }
            }
            if (!geometry.getString("motorDirection").equals(defaults.motorDirection.name())
                    || !geometry.getString("hoodName").equals(defaults.hoodName)) {
                return new Loaded(config, false, "Saved calibration ignored: hardware/direction changed. Retune.");
            }
            config.minAngle = entry.getDouble("minAngle");
            config.maxAngle = entry.getDouble("maxAngle");
            config.servoAtMinAngle = entry.getDouble("servoAtMinAngle");
            config.servoAtMaxAngle = entry.getDouble("servoAtMaxAngle");
            config.rpmCorrection = entry.getDouble("rpmCorrection");
            config.headingOffset = entry.getDouble("headingOffset");
            validate(config);
            return new Loaded(config, true, "Saved turret calibration loaded.");
        } catch (IOException | JSONException | IllegalArgumentException e) {
            return new Loaded(defaults.copy(), false, "Calibration ignored: " + e.getMessage());
        }
    }

    public static void save(ShooterConfig config) throws IOException, JSONException {
        validate(config);
        JSONObject root = read();
        JSONObject entry = new JSONObject();
        entry.put("geometry", geometry(config));
        entry.put("minAngle", config.minAngle);
        entry.put("maxAngle", config.maxAngle);
        entry.put("servoAtMinAngle", config.servoAtMinAngle);
        entry.put("servoAtMaxAngle", config.servoAtMaxAngle);
        entry.put("rpmCorrection", config.rpmCorrection);
        entry.put("headingOffset", config.headingOffset);
        root.put(config.flywheelName, entry);
        write(root);
    }

    // Only removes the selected shooter's entry. The other calibration is preserved.
    public static void remove(ShooterConfig config) throws IOException, JSONException {
        JSONObject root = read();
        root.remove(config.flywheelName);
        write(root);
    }

    private static JSONObject geometry(ShooterConfig c) throws JSONException {
        JSONObject geometry = new JSONObject();
        geometry.put("launchHeight", c.launchHeight);
        geometry.put("forwardOffset", c.forwardOffset);
        geometry.put("leftOffset", c.leftOffset);
        geometry.put("wheelDiameter", c.wheelDiameter);
        geometry.put("wheelToMotorRatio", c.wheelToMotorRatio);
        geometry.put("ticksPerMotorRevolution", c.ticksPerMotorRevolution);
        geometry.put("motorDirection", c.motorDirection.name());
        geometry.put("hoodName", c.hoodName);
        return geometry;
    }

    private static File file() {
        return AppUtil.getInstance().getSettingsFile(FILE_NAME);
    }

    private static JSONObject read() throws IOException, JSONException {
        File file = file();
        if (!file.exists() && !new File(file.getPath() + ".bak").exists()) {
            return new JSONObject().put("version", 1);
        }
        JSONObject root = new JSONObject(new String(new AtomicFile(file).readFully(), StandardCharsets.UTF_8));
        if (root.getInt("version") != 1) throw new IOException("Unsupported calibration file version.");
        return root;
    }

    private static void write(JSONObject root) throws IOException, JSONException {
        byte[] bytes = root.toString(2).getBytes(StandardCharsets.UTF_8);
        AtomicFile file = new AtomicFile(file());
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(bytes);
            file.finishWrite(output);
        } catch (IOException e) {
            if (output != null) file.failWrite(output);
            throw e;
        }
    }

    private static void validate(ShooterConfig c) {
        if (!Double.isFinite(c.minAngle) || !Double.isFinite(c.maxAngle)
                || c.minAngle <= -Math.PI / 2 || c.maxAngle >= Math.PI / 2 || c.minAngle >= c.maxAngle
                || !Double.isFinite(c.servoAtMinAngle) || c.servoAtMinAngle < 0 || c.servoAtMinAngle > 1
                || !Double.isFinite(c.servoAtMaxAngle) || c.servoAtMaxAngle < 0 || c.servoAtMaxAngle > 1
                || c.servoAtMinAngle == c.servoAtMaxAngle
                || !Double.isFinite(c.rpmCorrection) || c.rpmCorrection <= 0
                || !Double.isFinite(c.headingOffset)) {
            throw new IllegalArgumentException("Invalid hood/RPM calibration.");
        }
    }
}
