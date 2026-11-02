package org.firstinspires.ftc.teamcode.verydumbpathing.tracking;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.config.HardwareNames;

// this is a bit redundant, but also means that we could implement tracking relative to april tags later
public class odo implements trackable{

    GoBildaPinpointDriver device;
    odo(String device_name, double offset_x, double offset_y) {
        this.device = hardwareMap.get(GoBildaPinpointDriver.class, device_name);
        device.setOffsets(offset_x, offset_y, DistanceUnit.CM);
        device.resetPosAndIMU();
    }

    public boolean active() {
        return true;
    }

    public double x(DistanceUnit units) {
        return device.getPosX(units);
    }

    public double y(DistanceUnit units) {
        return device.getPosY(units);
    }

    public double yaw() {
        return device.getYawScalar();
    }

    public int priority() { return 0; }
}
