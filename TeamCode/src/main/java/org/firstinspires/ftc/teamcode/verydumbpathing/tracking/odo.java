package org.firstinspires.ftc.teamcode.verydumbpathing.tracking;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.config.HardwareNames;

// this is a bit redundant, but also means that we could implement tracking relative to april tags later using the interface with little resistance form the codebase
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

    public double x() {
        return device.getPosX(DistanceUnit.CM);
    }

    public double y() {
        return device.getPosY(DistanceUnit.CM);
    }

    public double yaw() {
        return device.getYawScalar();
    }

    public int priority() { return 0; }
}
