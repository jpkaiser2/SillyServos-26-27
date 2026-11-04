package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.follower.Follower;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Constants {
  public static MecanumConfig drivetrainConfig; //= new MecanumConfig(

  public static PinpointConfig localizerConfig; //= new PinpointConfig(

  public static ForesightConfig foresightConfig; //= new ForesightConfig();

  public static Follower create(HardwareMap h) {
    Follower follower =  new Follower(
            new PinpointLocalizer(h, localizerConfig),
            new Mecanum(h, drivetrainConfig),
            new Foresight(foresightConfig)
    );

    return follower;
  }
}