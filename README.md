# Silly Servos robot code

FTC robot software for driving, collecting balls, and shooting **pollen** and **nectar** into their alliance's hives. The project uses **FTC SDK 12.0.0**, **Pedro Pathing 3.0.2**, and **Pedro AutoTune 1.0.3**.

The main program is **Shooter TeleOp**. It tracks the robot's position, calculates a shot for each shooter, and lets two drivers control driving, aiming, spin-up, and feeding.

**Start here:** tune the drivetrain and odometry first. Then enter the field measurements and calibrate the shooters. Field measurements are currently unset, and shooter dimensions are examples; a successful build does not mean those values match the robot.

Drivers can start with the controls below. For first setup, work through one tuning section at a time; Foresight can wait until you are ready to add autonomous paths.

## Find what you need

- [What the code does](#what-the-code-does)
- [Driver controls](#driver-controls)
- [Hardware and first deployment](#hardware-and-first-deployment)
- [Tune driving and position tracking](#tune-driving-and-position-tracking)
- [Measure the field](#measure-the-field)
- [Calibrate the shooters and feeding](#calibrate-the-shooters-and-feeding)
- [Tune Foresight for future autonomous paths](#tune-foresight-for-future-autonomous-paths)
- [Troubleshooting](#troubleshooting)
- [Where the code lives](#where-the-code-lives)

## What the code does

An **OpMode** is a program selected on the Driver Station. This project provides:

| Program | Purpose | What it needs |
| --- | --- | --- |
| **Template TeleOp** | Drive with the sticks and display position. Sets a local `(0, 0, 0)` pose at Play. | Four drive motors and Pinpoint; configured and calibrated for accurate driving. |
| **Shooter TeleOp** | Drive, aim the chassis, control both shooters, and run intake/transfer. | All hardware below, shooter calibration, and the selected start plus both hive aim points for that alliance. |
| **Template Auto** | A timed driving example: currently 0.35 power for 1.5 seconds. Assumes `(72, 72, 0)` at initialization. | Drive motors and Pinpoint. This does not follow a Pedro path or score balls. |

**Pedro and Pinpoint track position.** The Pinpoint uses odometry wheels and its heading sensor to estimate the robot's **pose**: X, Y, and which direction it faces. At Play, Shooter TeleOp sets this pose to the selected starting position. From then on, movement updates the pose.

**Each shooter has its own calculation and hardware.** Two instances of `Turret` each control one flywheel motor and one servo hood. Despite the class name, there is no separate horizontal turret motor: the **whole robot turns to aim**. The hood sets the vertical launch angle.

Every loop, `ShooterMath` uses the robot pose, the shooter's mounting offset, and the fixed hive coordinates to calculate distance, launch angle, flywheel RPM, and heading error. When spin-up is held, the selected hood moves and the flywheel uses encoder velocity control to reach the requested speed. The other flywheel stops.

**Both feed paths run together.** The intake also feeds nectar. Pollen has a separate transfer motor and two continuous-rotation servos commanded in opposite directions. Intake and shooting commands run both paths; reverse reverses both paths and stops the flywheels.

The shot calculation assumes a stationary robot. There is no camera-based position correction, automatic ball detection, or automatic wait for aim/RPM before feeding. Stop moving and let the hood and flywheel settle before shooting.

<details>
<summary>The shot math, if you want the details</summary>

The implementation follows the supplied *SillyServosShooterMath* equations. It rotates each shooter's forward/left mounting offset into field coordinates, then finds the horizontal distance `d` and height difference `h` from the ball's launch point to the hive aim point.

```text
idealAngle = PI/4 + atan2(h, d)/2
angle = idealAngle limited to the hood's measured travel
ballSpeed = sqrt(9.81*d² / (2*cos(angle)² * (d*tan(angle) - h)))
wheelRPM = 60 * rpmCorrection * ballSpeed / (PI * wheelDiameter)
```

Using the general speed equation after limiting the angle keeps the trajectory calculation consistent with the hood's actual range. Wheel RPM is converted to motor encoder ticks per second using the gearing and encoder resolution.

Pedro positions are in inches; projectile calculations use meters. The code handles this conversion. Impossible geometry, invalid settings, or a requested speed above `maxWheelRpm` produces **No valid shot**, stopping the flywheel and preventing right-trigger feeding. Left-bumper intake remains available.

See [ShooterMath.java][shooter-math] and the [shooter reference](docs/SHOOTERS.md) for more detail.

</details>

## Driver controls

**G1** means gamepad 1, the driver. **G2** means gamepad 2, the shooter operator. **LB/RB** are the left/right bumpers; **RT** is the right trigger. Buttons marked **hold** act only while held; A/B select a shooter until changed.

### Before Play

1. Choose **Shooter TeleOp** and press **INIT**. Keep the robot still while Pinpoint initializes.
2. Use **G1 D-pad left/right** to select **Red Right, Red Left, Blue Right, or Blue Left**.
3. Check the displayed alliance and measurement status. Place the robot at that start, facing its measured heading.
4. Press **Play**. The start selection is now locked. Pollen is initially selected.

If the selected start or either hive aim point for its alliance is unmeasured, the program refuses to start. The other alliance's measurements can be filled in later. Shooter TeleOp always initializes from the selected start; it does not inherit an autonomous ending pose.

### While running

| Control | Action |
| --- | --- |
| **G1 left stick** | Drive and strafe. Field-centric by default: translation stays aligned to the field as the robot turns. |
| **G1 right stick left/right** | Turn counterclockwise/clockwise. |
| **Hold G1 LB** | Stop translation and automatically turn toward the selected shooter's hive. Overrides the sticks while held. |
| **G2 A / B** | Select pollen / nectar. Changing selection stops the previous flywheel. |
| **Hold G2 RB** | Move the selected hood and spin up the selected flywheel, without feeding. |
| **Hold G2 LB** | Run intake, pollen transfer, and both transfer servos. Does not itself spin a flywheel. |
| **Hold G2 RT** | Spin the selected flywheel and feed through both paths when the shot calculation is valid. Activates above 10% trigger travel. |
| **Hold G2 X** | Reverse both feed paths and transfer servos; stop both flywheels. Takes priority over the other G2 feed/spin controls. |
| **Release G2 LB, RB, RT, and X** | Stop feed commands and flywheel commands. The hood holds its last commanded position. |
| **Driver Station Stop** | Stop drivetrain, flywheel, feed-motor, and transfer-servo commands. Flywheels may coast down. |

In Shooter TeleOp's field-centric mode, pushing the left stick forward commands **+X**, and pushing it left commands **+Y** in Pedro's field diagram. These directions do not flip with alliance. Template TeleOp uses the robot's orientation at Play as its local zero heading. Set `FIELD_CENTRIC_TELEOP = false` in [RobotConstants][robot-constants] for translation relative to the robot instead.

**A normal shot:** select A or B → stop driving → hold G1 LB to align → hold G2 RB → watch measured RPM approach commanded RPM → hold G2 RT to feed. Keep aim held if needed. **RT feeds immediately; the software does not check that RPM or aim has settled.**

## Hardware and first deployment

Use these exact names in the active Robot Controller hardware configuration. Names are case-sensitive. Assign the real ports and motor models; Java names alone do not create the hardware configuration.

| Device | Configuration name | Type |
| --- | --- | --- |
| Front-left / front-right drive | `leftFront` / `rightFront` | Motors |
| Back-left / back-right drive | `leftBack` / `rightBack` | Motors |
| Odometry computer | `pinpoint` | goBILDA Pinpoint, with forward and lateral odometry pods |
| Pollen / nectar flywheel | `flywheelPollen` / `flywheelNectar` | Motors with working encoders |
| Pollen / nectar hood | `hoodPollen` / `hoodNectar` | Positional servos |
| Intake and nectar feeder | `intakeMain` | Motor |
| Pollen transfer | `intakeSmall` | Motor |
| Left / right pollen transfer | `pollenTransferLeft` / `pollenTransferRight` | Continuous-rotation servos |

The names are defined in [HardwareNames.java][hardware-names]. The Pinpoint forward pod connects to its X input; the lateral pod connects to Y. See the [official Pinpoint setup guide](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint) for mounting and wiring.

1. Open this repository's root folder in Android Studio and let **Gradle sync** finish. The dependencies are already installed in the project.
2. Power and connect the Robot Controller, and make sure the Driver Station connects to it.
3. Select the **TeamCode** app/module and the Robot Controller device in Android Studio, then click **Run** to build and install the app.
4. Verify the hardware configuration above is active. **Template TeleOp** needs only the drive motors and Pinpoint, so start there while other mechanisms are unfinished.

After changing Java configuration values, **build and deploy again**. Saving a file or syncing Gradle does not update the running robot.

## Tune driving and position tracking

Work through these steps in order. You do not need hive measurements or calibrated shooters for Pedro tuning. Use an unloaded robot and leave room for each requested movement; the automatic procedures drive the robot themselves.

### 1. Open AutoTune

Connect your computer to the robot's Wi-Fi, then open [http://192.168.43.1:10158](http://192.168.43.1:10158). This address assumes the standard Control Hub network. Keep the Driver Station connected and follow the browser's prompts for starting and stopping each step.

The project already registers **Mecanum Tuner**, **Pinpoint Tuner**, and **Foresight Tuner**. You do not need to add them to `Tuning.java`.

### 2. Set the drive motor directions

Select **Mecanum Tuner**. Support the robot so its wheels can spin freely for this direction check.

1. Enter `leftFront`, `rightFront`, `leftBack`, and `rightBack` in their matching fields.
2. Follow the diagram as each wheel spins. Stop that observation when you can identify the wheel and direction, then answer the prompts.
3. If the wrong wheel spins, correct the hardware name or wiring before continuing.
4. Open the result's **Java** tab. In [Constants.java][pedro-constants], replace the existing `drivetrainConfig` declaration with the generated one. Keep `c.manualBrakeMode.set(true);` inside its configuration block for manual-drive braking.
5. Add any missing imports using Android Studio's import quick fix, then deploy again.

This procedure checks motor direction, not driving speed or path accuracy. [Official Mecanum guide](https://pedropathing.com/docs/pathing/tuning/drivetrain/mecanum).

### 3. Calibrate Pinpoint

Put the robot on the floor with both odometry pods touching it. Select **Pinpoint Tuner**, enter `pinpoint`, and choose the actual pod type. Keep the robot still during initialization.

Follow each prompt separately:

| Prompt | What you do |
| --- | --- |
| Custom pod resolution, if selected | Enter a distance in inches. Push straight forward exactly that distance, then Stop. |
| Forward direction | Push the robot straight forward, then Stop. |
| Strafe direction | Push the robot straight **left**, then Stop. |
| Pod offsets | Rotate the robot in place **180° counterclockwise**, then Stop. |

Copy the generated Java declaration over `localizerConfig` in [Constants.java][pedro-constants], add missing imports, and deploy again. Keep `globalDistanceUnit` and `offsetUnits` in **inches**. [Official Pinpoint guide](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint).

**Which settings now matter?** Initially, `Constants.java` reads directions, pod type, and offsets from `RobotConstants`. If you replace those blocks with generated values, the generated values in **Constants.java** become the active settings. Editing the old defaults will no longer change them.

### 4. Check that position matches real movement

Run **Template TeleOp**. At Play, its local position and heading start at zero. Its heading telemetry is in **radians**.

1. Drive slowly straight forward without turning. X should increase and roughly match the tape-measured distance in inches.
2. Return to the start, then strafe left without turning. Y should increase by the measured distance.
3. Turn counterclockwise. From zero, a quarter turn should read about **1.57 radians**. The number may wrap at its angle boundary as you keep turning.
4. Return to the same physical position and heading. The displayed pose should be near its starting value. Rotating in place should not produce a large translation.

If any of these disagree, fix pod contact, pod type, directions, or offsets before using hive aiming. For initial driving checks, lower `MAX_DRIVE_POWER` and `MAX_TURN_POWER` in [RobotConstants][robot-constants]; these cap the TeleOp stick commands, **not AutoTune's automatic movements**.

## Measure the field

Edit [FieldConfig.java][field-config]. Use the same [official Pedro field diagram](https://pedropathing.com/docs/pathing/reference/coordinates) for every measurement:

| Measurement | Convention |
| --- | --- |
| Origin | Bottom-left corner `(0, 0)`; field center is `(72, 72)` inches. |
| X / Y | Right / up in Pedro's diagram, in inches. |
| Hive height | Inches above the floor, at the intended scoring aim point. |
| Start heading | Degrees: right = 0°, up = 90°, left = 180°, down = 270°. |

1. Identify the robot point that your odometry tracks—normally the configured center of rotation. Measure starts and shooter offsets from this same reference, not a bumper or the Pinpoint enclosure.
2. Place the robot at each starting position and record that reference point's X, Y, and the chassis heading. Left/right labels are from that alliance's view into the field.
3. Replace the three `UNMEASURED` values in each `StartPosition` entry with its **X, Y, heading**.
4. In `Alliance.RED` and `Alliance.BLUE`, replace each target's values with **X, Y, height**. The first target is pollen, the second nectar. If both aim points coincide, enter the same values twice.
5. Deploy, select the matching start during Shooter TeleOp initialization, and check the displayed coordinates and distance against your measurements.

Both red starts use the same fixed red hives; both blue starts use the same fixed blue hives. Changing starts never mirrors or moves the targets. Measure all four starts eventually; to begin calibration, one measured start and both targets for its alliance are enough.

## Calibrate the shooters and feeding

Do this for **pollen and nectar separately**. Record each measurement as you go. Change one setting at a time, redeploy, and compare several shots before making another change.

### 1. Enter the physical measurements

Edit the `static { ... }` block in [ShooterConfig.java][shooter-config] using assignments such as `POLLEN.wheelDiameter = ...;` and `NECTAR.wheelDiameter = ...;`. Replace the dots with measurements, rather than leaving both shooters on the shared example defaults.

| Setting | What to measure or verify |
| --- | --- |
| `launchHeight` | Height of the ball's launch point above the floor, in **meters**. |
| `forwardOffset`, `leftOffset` | Launch point relative to the odometry reference, in **meters**. Forward/left positive; backward/right negative. |
| `headingOffset` | Fixed horizontal mounting angle relative to chassis forward, in **radians**, counterclockwise positive. Zero means straight forward. |
| `wheelDiameter` | Flywheel diameter at its ball-contact surface, in **meters**. |
| `wheelToMotorRatio` | Flywheel turns per motor output-shaft turn. `2.0` means two wheel turns for one motor turn. |
| `ticksPerMotorRevolution` | Encoder ticks per motor **output-shaft** revolution, including its internal gearbox; check the motor specification. |
| `motorDirection` | Direction that makes positive commanded velocity launch the ball forward. |
| `maxWheelRpm` | An achievable, mechanically appropriate flywheel-speed limit. Verify it on the robot; the example 5000 is not a measured limit. |

Convert inches to meters by multiplying by **0.0254**. For angle assignments, `Math.toRadians(30)` converts a measured 30° angle into radians. FieldConfig uses inches/degrees; ShooterConfig uses meters/radians.

### 2. Calibrate the servo hoods

Use the SDK's **Test Hardware** utility before attempting shots. On the Driver Station, open **Settings → Utility OpModes → Test Hardware**, then initialize/start it. This utility is included in the repository and does not need the hive measurements. [FTC utility instructions](https://ftc-docs.firstinspires.org/en/latest/utility_opmodes/index.html).

1. With no balls loaded, use G1 D-pad left/right to select `hoodPollen` or `hoodNectar`.
2. The left stick Y chooses a servo position from 0 to 1, shown as **Value**. Hold RB to apply it. Start within the mechanism's known free travel and move in small increments; do not force the linkage against its stops.
3. Find the lowest usable launch angle. Record that angle and its servo position as `minAngle` and `servoAtMinAngle`.
4. Find the highest usable angle and record `maxAngle` and `servoAtMaxAngle`.
5. Measure the ball's exit direction above horizontal, not simply the servo horn angle. Check an intermediate position too: the code assumes a straight-line relationship between servo position and launch angle.
6. Repeat for the other hood, enter the four values per shooter, and redeploy.

The servo positions may decrease as the launch angle increases; reversed endpoints are supported. If intermediate angles do not follow the straight-line mapping, `ShooterMath.hoodPosition()` needs a different mapping before the calculated shots will be accurate.

### 3. Verify feeding and flywheel speed

Once the selected start and alliance targets are measured, run **Shooter TeleOp** at that start.

- Hold **G2 LB** briefly. Both feed motors and both transfer servos should move balls toward their shooters. In [RobotConstants][robot-constants], correct `INTAKE_DIRECTION`, `POLLEN_TRANSFER_DIRECTION`, or `TRANSFER_LEFT_SERVO_DIRECTION` as needed. The right servo direction is always opposite the left.
- Adjust `INTAKE_POWER`, `POLLEN_TRANSFER_POWER`, and `TRANSFER_SERVO_POWER` between 0 and 1 for reliable feeding. Begin with reduced values and increase until balls transfer consistently. Check **G2 X** reverses the whole feed system.
- Select a shooter and hold **G2 RB** without feeding. Confirm its hood moves and its flywheel spins in the launch direction. Compare **commanded / measured RPM** on telemetry after it settles.

If RPM does not track the command, check encoder wiring, configured motor model, ticks per revolution, and gearing first. The flywheel uses the SDK's motor velocity controller; there are currently no adjustable flywheel PIDF constants in this project. Persistent speed-control problems may require adding that tuning. Do not use `rpmCorrection` to hide a motor that cannot reach its requested RPM, or raise the RPM limit merely to bypass **No valid shot**.

The Test Hardware utility can also check individual motors before field measurements are available. In that utility, releasing RB does **not** automatically zero a motor: command zero with RB or press Stop before changing devices.

### 4. Settle the chassis aim

With the shooter selected, stop translating and hold **G1 LB**. It should rotate toward the hive and settle near zero **Aim error**.

| Symptom | Adjustment in RobotConstants |
| --- | --- |
| Turns too aggressively or repeatedly overshoots | Reduce `AIM_KP` and/or `MAX_AIM_POWER`. |
| Barely turns despite a clear heading error | Verify drive directions, then increase `AIM_KP` or the power limit a little. |
| Keeps making tiny corrections when already aligned | Increase `AIM_TOLERANCE_RADIANS` slightly. |

Repeat from both sides of the target heading. If it consistently points the wrong way, check pose, hive coordinates, and the shooter's `headingOffset` before changing gains. These aim settings are independent of Foresight's autonomous tuning.

### 5. Tune shot distance

Start with the geometry and hood calibration above, then adjust **`rpmCorrection`**, also called **k**. Pollen and nectar each have their own value; both currently start at 1.0.

1. Initialize at a measured starting position and select that start. Drive to a repeatable shooting location with a clear target, then remain stationary for each shot.
2. Select the ball type, align with G1 LB, and hold G2 RB until the hood and RPM settle. Use G2 RT to feed.
3. Record distance, hood angle, commanded/measured RPM, k, and whether several shots land short, long, or on target.
4. If shots consistently fall short while aim, angle, and RPM tracking are correct, raise k slightly—about 2–5% at a time. If they consistently go long, lower it slightly. Deploy and repeat.
5. Check the result at near, middle, and far shooting locations, then repeat for the other ball type.

At the same pose, increasing k by 5% increases commanded wheel RPM by 5%. One correction factor may not remove every distance-dependent error; recheck hood mapping, wheel contact, and geometry if one distance works but others do not. The model assumes the robot is stationary and does not explicitly model air drag or ball spin.

**Ready-to-use checkpoint:** measured pose agrees with the field, aim settles, each hood reaches the calculated angle, measured RPM follows its command, and repeated shots work from several distances.

## Tune Foresight for future autonomous paths

Foresight learns how the drivetrain accelerates, brakes, and corrects position. **It is optional for the current TeleOps and timed Template Auto.** Complete the Mecanum and Pinpoint steps before running it.

1. Open AutoTune and choose **Foresight Tuner**. Use the normal robot setup and a representative charged battery, with space ahead, behind, and beside the robot.
2. Follow each prompt and reposition between steps when instructed. The sequence measures forward/strafe speed and deceleration, turning response, braking, and translation response. Some runs coast beyond the entered distance.
3. The initial distance defaults to **48 inches**. The deceleration-speed prompt suggests a value below both measured maximum speeds; it must remain below both. The later braking distance defaults to **36 inches** and is clamped to at least **15 inches**.
4. Let automatic measurements finish. Stop cancels them without accepting partial calibration. If a step reports invalid measurements, correct the cause and rerun rather than copying a failed result.
5. Replace `foresightConfig = null` in [Constants.java][pedro-constants] with the generated Java declaration, add the indicated imports, and deploy.

The [official Foresight guide](https://pedropathing.com/docs/pathing/tuning/foresight) explains the individual movements. The [Pedro setup reference](docs/PEDRO_SETUP.md) lists the imports and configuration details.

Future paths or position holding should use `Constants.create(hardwareMap)` after this calibration. The current programs use `createManual()`. Applying Foresight results does not turn Template Auto into a path-following autonomous. Pedro's optional [Tests procedure](https://pedropathing.com/docs/pathing/tuning/test) can verify paths after tuning; that procedure is **not currently registered in this project**.

## Troubleshooting

| What you see | What to check first |
| --- | --- |
| Hardware not found during INIT | Exact configuration names, device types, active configuration, and connected ports. Shooter TeleOp initializes both shooters and all feed hardware. |
| AutoTune page will not open | Computer on robot Wi-Fi, correct robot IP, `http` rather than `https`, port `10158`, and the latest app deployed/running. |
| Shooter TeleOp stops as soon as Play is pressed | Selected start and both aim points for its alliance still contain `UNMEASURED`. Read the initialization telemetry. |
| Distance changes incorrectly or aim points away from the hive | Pinpoint directions/offsets, selected start, heading, and field units. Recheck with Template TeleOp. |
| No valid shot | Shooter dimensions, angle endpoints, target height, and RPM limit. A target at the launch point or beyond the configured capability is rejected. |
| RT feeds before the wheel is ready | Expected behavior. Pre-spin with RB and watch RPM before pressing RT. |
| One shooter is selected but both feed paths run | Expected behavior: intake and transfer deliberately run together. |
| Hood does not move while only aiming | Hood commands are applied with G2 RB or RT for a valid shot. G1 LB only turns the chassis. |
| A constant change has no effect | Redeploy, and check whether generated values in `Constants.java` replaced references to `RobotConstants`. |

## Where the code lives

Most robot work belongs under `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`.

| File | Responsibility |
| --- | --- |
| [ShooterTeleOp.java][shooter-teleop] | Gamepad controls, start selection, and coordination of driving/shooting/feeding. |
| [HardwareNames.java][hardware-names] | Hardware configuration names. |
| [RobotConstants.java][robot-constants] | Feed directions/powers, manual-drive limits, chassis aim settings, and initial odometry defaults. |
| [FieldConfig.java][field-config] | Four starting poses and fixed red/blue hive aim points. |
| [ShooterConfig.java][shooter-config] | Separate pollen/nectar geometry, hood mapping, encoder/gearing data, RPM limits, and k. |
| [Turret.java][turret] | One flywheel and hood; instantiated once per ball type. |
| [ShooterMath.java][shooter-math] | Shot geometry, projectile equations, and unit/speed conversions. |
| [IntakeTransfer.java][intake-transfer] | Both feed motors and the paired transfer servos. |
| [pedroPathing/Constants.java][pedro-constants] | Active Pedro drivetrain, localizer, and optional Foresight configurations. |
| [pedroPathing/Tuning.java][tuning] | Registers the three AutoTune calibration procedures. |
| [PedroMecanumDrive.java][drive] | Shared Pedro manual-drive wrapper. |

For deeper reference, see [shooter configuration and equations](docs/SHOOTERS.md) and [Pedro setup](docs/PEDRO_SETUP.md). SDK examples and utilities live in `FtcRobotController`; they are separate from the team's robot logic. This repository builds on the [FIRST FTC SDK](https://github.com/FIRST-Tech-Challenge/FtcRobotController); its licensing terms remain in [LICENSE](LICENSE).

[shooter-teleop]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/opmodes/teleop/ShooterTeleOp.java
[hardware-names]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/config/HardwareNames.java
[robot-constants]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/config/RobotConstants.java
[field-config]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/config/FieldConfig.java
[shooter-config]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/config/ShooterConfig.java
[turret]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems/Turret.java
[shooter-math]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems/ShooterMath.java
[intake-transfer]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems/IntakeTransfer.java
[pedro-constants]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedroPathing/Constants.java
[tuning]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedroPathing/Tuning.java
[drive]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedroPathing/PedroMecanumDrive.java
