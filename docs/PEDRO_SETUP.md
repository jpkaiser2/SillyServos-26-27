# Pedro Pathing 3 setup

The project uses `com.pedropathing:revhub:3.0.2` and `com.pedropathing:tuning:1.0.3`,
matching the [official Quickstart's dependencies](https://github.com/Pedro-Pathing/Quickstart/blob/master/build.dependencies.gradle).
`revhub` includes Pedro core. The old `ftc:2.0.5` dependency has been removed.
The Dairy repository from the [installation guide](https://pedropathing.com/docs/pathing/installation)
is configured alongside Maven Central in `build.dependencies.gradle`.

The drive wrapper, TeleOps, TemplateAuto, and shooter pose calculations have been
migrated to Pedro 3. `pedroPathing/Constants.java` shares hardware configuration between the
drive wrapper and AutoTune. `pedroPathing/Tuning.java` registers the official Mecanum,
Pinpoint, and Foresight procedures under `pedroPathing/procedures`.

## What to do on your computer and robot

1. **Sync Gradle in Android Studio**, then deploy the Robot Controller app to the robot.
2. Verify the active Robot Controller hardware configuration matches `HardwareNames.java`.
   The drive uses `leftFront`, `rightFront`, `leftBack`, `rightBack`, and `pinpoint`.
3. Connect the computer to the robot's Wi-Fi and open the
   [AutoTune interface](http://192.168.43.1:10158). Follow its prompts and the Driver Station
   instructions. This address assumes the standard Control Hub network.
4. Run the **Mecanum Tuner**, then **Pinpoint Tuner**. Keep the robot still during Pinpoint
   initialization, which recalibrates its IMU. These procedures require physically checking
   wheel directions and moving/rotating the robot as prompted.
5. Copy each procedure's generated Java configuration into the matching `drivetrainConfig`
   or `localizerConfig` in `pedroPathing/Constants.java`, including any generated imports.
   Rebuild and redeploy after applying these results, before running Foresight.
   Use TemplateTeleOp to verify that forward travel increases X, left travel increases Y,
   and counterclockwise rotation increases heading when starting at heading zero.
6. For autonomous path following, run **Foresight Tuner** with space for the robot to move.
   Replace `foresightConfig = null` in `Constants.java` with its generated `ForesightConfig`
   and add the generated imports. Rebuild and redeploy.

The initial drivetrain and localizer configurations read the existing hardware names,
directions, pod type, and offsets from `HardwareNames` and `RobotConstants`. Once replaced
with generated AutoTune configurations, those generated settings in `Constants.java` are
the settings Pedro actually uses. Keep global localization units and offsets in **inches**.

Follow Pedro's [Mecanum](https://pedropathing.com/docs/pathing/tuning/drivetrain/mecanum),
[Pinpoint](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint), and
[Foresight](https://pedropathing.com/docs/pathing/tuning/foresight) guides for the physical steps.

Foresight rejects nonpositive or nonfinite distances/velocities and deceleration speeds
at or above either measured maximum. Stop cancels an automatic measurement; it does not
save partial results. In the Mecanum and Pinpoint procedures, Stop still finishes the
manual observation as instructed by AutoTune.

When pasting generated configuration, Android Studio may need to add imports for
`DcMotorSimple`, `GoBildaPinpointDriver`, or `OptionalDouble`. Foresight's generated block
also uses `com.pedropathing.controllers.Controller`, `com.pedropathing.math.Matrix`, and
`com.pedropathing.math.Vector2D`.

## Manual drive versus paths

The existing TeleOps and timed TemplateAuto use `Constants.createManual()`, so they can run
with the drivetrain/localizer configured, before Foresight tuning. The wrapper uses Pedro 3's
`manual()`, `ManualDrive.fieldCentric()`, and `setPose()` APIs. Its input convention stays
forward positive, strafe left positive, and counterclockwise turn positive.
TemplateTeleOp sets a local `(0, 0, 0)` pose at Play, so previous Pinpoint runs do not
change its field-centric controls. Its pose is relative to that starting position;
ShooterTeleOp instead uses the measured field start selected during initialization.

For future autonomous paths or position holding, use `Constants.create(hardwareMap)` after
filling in `foresightConfig`. It deliberately reports an error if that configuration is
missing. A manual-only follower has no path/hold algorithm and should not be used for those
operations or algorithm debug logging. TemplateAuto remains a timed driving example.

## Shooter setup still required

Pedro tuning does not supply field start positions, hive coordinates, or shooter calibration.
Fill in `FieldConfig` and `ShooterConfig` as described in [SHOOTERS.md](SHOOTERS.md).
ShooterTeleOp still requires its selected start and alliance hive measurements before Play.
The four-start selector and the bottom-left-origin Pedro field coordinates are preserved.
Turret Tuner instead establishes a local origin beneath a practice target at Play. Once
Pedro is tuned, it can calibrate the shooters without field start or hive coordinates;
see the [README walkthrough](../README.md#calibrate-the-shooters-and-feeding).
