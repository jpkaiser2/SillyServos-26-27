# Silly Servos robot code

FTC robot software for driving, collecting balls, and shooting **pollen** and **nectar** into their alliance's hives. The project uses **FTC SDK 12.0.0**, **Pedro Pathing 3.0.2**, and **Pedro AutoTune 1.0.3**.

For manual operation without odometry, use **[Simple TeleOp](#simple-teleop-no-pedro-or-pinpoint)**.

The position-aware program is **Shooter TeleOp**. It tracks the robot's position, calculates a shot for each shooter, and lets two drivers control driving, aiming, spin-up, and feeding.

**Start here:** tune the drivetrain and odometry first, then use **Turret Tuner** to calibrate the shooters on a practice target. Nominal hive aim points are configured for the BIOBUZZ match-start orientation. Robot starting poses and autonomous shooting positions still need measurements, and shooter dimensions are examples; a successful build does not mean those values match the robot.

Drivers can start with the controls below. For first setup, work through one tuning section at a time; Foresight is needed for Shooting Auto, but can wait while you set up manual driving and the shooters.

## Find what you need

- [What the code does](#what-the-code-does)
- [Driver controls](#driver-controls)
- [Simple TeleOp: no Pedro or Pinpoint](#simple-teleop-no-pedro-or-pinpoint)
- [Hardware and first deployment](#hardware-and-first-deployment)
- [Tune driving and position tracking](#tune-driving-and-position-tracking)
- [Measure the field](#measure-the-field)
- [Calibrate the shooters and feeding](#calibrate-the-shooters-and-feeding)
- [Shooting Auto: four starts, two shooting positions](#shooting-auto-four-starts-two-shooting-positions)
- [Tune Foresight for autonomous paths](#tune-foresight-for-autonomous-paths)
- [Troubleshooting](#troubleshooting)
- [Where the code lives](#where-the-code-lives)

## What the code does

An **OpMode** is a program selected on the Driver Station. This project provides:

| Program | Purpose | What it needs |
| --- | --- | --- |
| **Simple TeleOp** | Standard mecanum driving, manual shooter RPM/hood, intake/transfer, and gates. | Drive motors and all shooter/feed hardware. No Pinpoint, field coordinates, or Pedro tuning required. |
| **Template TeleOp** | Drive with the sticks and display position. Sets a local `(0, 0, 0)` pose at Play. | Four drive motors and Pinpoint; configured and calibrated for accurate driving. |
| **Shooter TeleOp** | Drive, aim the chassis, control both shooters, and run intake/transfer. Loads saved turret calibration at INIT. | All hardware below, shooter calibration, and the selected start plus both hive aim points for that alliance. |
| **Turret Tuner** | Adjust hood/RPM live, record successful shots, calculate calibration, and save it on the robot. | All hardware below, tuned Pedro, basic shooter geometry, and a practice target. No FieldConfig measurements or Foresight needed. |
| **Shooting Auto** | Drive from any of the four starts to its alliance's shooting position, fire pollen, then fire nectar. | Tuned Pedro/Foresight, calibrated shooters/gates, and measured start, alliance shooting position, and both hive targets. |
| **Template Auto** | A timed driving example: currently 0.35 power for 1.5 seconds. Assumes `(72, 72, 0)` at initialization. | Drive motors and Pinpoint. This does not follow a Pedro path or score balls. |

**Pedro and Pinpoint track position.** The Pinpoint uses odometry wheels and its heading sensor to estimate the robot's **pose**: X, Y, and which direction it faces. At Play, Shooter TeleOp sets this pose to the selected starting position. From then on, movement updates the pose.

**Each shooter has its own calculation and hardware.** Two instances of `Turret` each control one flywheel motor, one servo hood, and one positional servo gate that blocks balls until shooting. Despite the class name, there is no separate horizontal turret motor: the **whole robot turns to aim**. The hood sets the vertical launch angle.

Every loop, `ShooterMath` uses the robot pose, the shooter's mounting offset, and the fixed hive coordinates to calculate distance, launch angle, flywheel RPM, and heading error. When spin-up is held, the selected hood moves and the flywheel uses encoder velocity control to reach the requested speed. The other flywheel stops.

**Both feed paths run together.** The intake also feeds nectar. Pollen has a separate transfer motor and two continuous-rotation servos commanded in opposite directions. Intake and shooting commands run both paths; reverse reverses both paths and stops the flywheels. Each shooter has its own blocking gate: only the selected shooter opens its gate on a valid shoot command. Both gates stay closed during intake-only operation and spin-up, and close on reverse or Stop.

The shot calculation assumes a stationary robot. Shooter TeleOp has no camera-based position correction, automatic ball detection, or automatic wait for aim/RPM before feeding. Turret Tuner adds a stationary/RPM check for its calibration shots. Stop moving and let the hood and flywheel settle before shooting.

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

These are **Shooter TeleOp** controls. [Turret Tuner uses its own controls](#2-set-up-turret-tuner) below.

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
| **Hold G2 RB** | Move the selected hood and spin up the selected flywheel. Both gates stay closed unless RT is also held. |
| **Hold G2 LB** | Run intake, pollen transfer, and both transfer servos. Does not itself spin a flywheel. |
| **Hold G2 RT** | Spin the selected flywheel, open its gate, and feed through both paths when the shot calculation is valid. The other gate stays closed. Activates above 10% trigger travel. |
| **Hold G2 X** | Reverse both feed paths and transfer servos; stop both flywheels. Takes priority over the other G2 feed/spin controls. |
| **Release G2 LB, RB, RT, and X** | Stop feed commands and flywheel commands, and close both gates. The hood holds its last commanded position. |
| **Driver Station Stop** | Stop drivetrain, flywheel, feed-motor, and transfer-servo commands; close both gates. Flywheels may coast down. |

In Shooter TeleOp's field-centric mode, pushing the left stick forward commands **+X**, and pushing it left commands **+Y** in Pedro's field diagram. These directions do not flip with alliance. Template TeleOp uses the robot's orientation at Play as its local zero heading. Set `FIELD_CENTRIC_TELEOP = false` in [RobotConstants][robot-constants] for translation relative to the robot instead.

**A normal shot:** select A or B → stop driving → hold G1 LB to align → hold G2 RB → watch measured RPM approach commanded RPM → hold G2 RT to feed. Keep aim held if needed. **RT feeds immediately; the software does not check that RPM or aim has settled.**

## Simple TeleOp: no Pedro or Pinpoint

Select **Simple TeleOp** on the Driver Station, press INIT, then Play. This program drives the four motors directly with standard mecanum mixing. Driving is always **robot-centric**: stick forward moves toward the robot's front, regardless of its orientation. No Pinpoint, IMU, starting-position selection, or hive coordinates are used.

It reuses the existing `Turret` and `IntakeTransfer` hardware classes. Flywheels still use encoder velocity control, both feed paths still run together, and only the selected shooter's gate opens for shooting. The driver chooses the hood position and wheel RPM instead of having a trajectory calculation choose them.

| Control | Action |
| --- | --- |
| **G1 left stick** | Drive forward/back and strafe relative to the robot. |
| **G1 right stick left/right** | Turn counterclockwise/clockwise. Aim the chassis manually. |
| **G2 A / B** | Select pollen / nectar. The other flywheel stops and its gate closes. |
| **G2 D-pad up/down** | Increase/decrease selected wheel RPM by 50. Hold to repeat. Limited to 0–`maxWheelRpm`. |
| **G2 D-pad right/left** | Increase/decrease selected hood servo position by 0.01. Hold to repeat. Moves the hood immediately unless reverse is held, without requiring the flywheel to spin. |
| **Hold G2 LB** | Run both feed paths and the paired transfer servos. Gates remain closed unless shooting. |
| **Hold G2 RB** | Apply the selected hood setting and spin its flywheel. Does not open a gate by itself. |
| **Hold G2 RT** | Spin the selected flywheel, open its gate, and run both feed paths. Requires a valid positive RPM command; activates above 10% trigger travel. |
| **Hold G2 X** | Reverse both feed paths and stop both flywheels; both gates close. Takes priority over shooting. |
| **Release G2 LB/RB/RT/X** | Stop feed and flywheel commands; close gates. The hood holds its last position. |
| **Driver Station Stop** | Stop drive/feed/flywheel commands and close both gates. |

G1 LB has no auto-aim function in this OpMode. G2 D-pad changes servo position numerically; depending on the linkage, increasing the position may raise or lower the launch angle.

Each shooter starts with **1500 wheel RPM** (or its configured limit if lower) and a requested hood position midway between its configured endpoints. The hood moves when adjusted or when spin-up/shooting is requested. Pollen and nectar keep their own requested settings while you switch between them; adjustments are not saved after the OpMode ends.

If a saved turret calibration is available, its servo endpoints supply the hood's allowed range. Otherwise the endpoints in `ShooterConfig` are used. Both endpoint orders are supported. Calibration loading is local file reading and does not initialize Pedro or odometry. Geometry-based angle, k, and aim calculations are not used here. Confirm the hood endpoints, gate positions, encoder/gearing values, and RPM limit match the robot before running.

Use **RB to spin up, watch measured RPM, aim manually, then RT to shoot**. As in Shooter TeleOp, RT feeds immediately; it does not wait for the RPM or hood to settle. Setting requested RPM to zero prevents RT from opening a gate or feeding, but LB still permits ordinary intake.

Drive direction settings come from `LEFT_DRIVE_DIRECTION` / `RIGHT_DRIVE_DIRECTION` in `RobotConstants`, and the stick limits use `MAX_DRIVE_POWER` / `MAX_TURN_POWER`. Generated Pedro drivetrain settings and `FIELD_CENTRIC_TELEOP` do not affect Simple TeleOp. The existing Pedro OpModes, turret tuner, and saved calibration format are unchanged.

## Hardware and first deployment

Use these exact names in the active Robot Controller hardware configuration. **Simple TeleOp does not require the `pinpoint` device.** Names are case-sensitive. Assign the real ports and motor models; Java names alone do not create the hardware configuration.

| Device | Configuration name | Type |
| --- | --- | --- |
| Front-left / front-right drive | `leftFront` / `rightFront` | Motors |
| Back-left / back-right drive | `leftBack` / `rightBack` | Motors |
| Odometry computer | `pinpoint` | goBILDA Pinpoint, with forward and lateral odometry pods |
| Pollen / nectar flywheel | `flywheelPollen` / `flywheelNectar` | Motors with working encoders |
| Pollen / nectar hood | `hoodPollen` / `hoodNectar` | Positional servos |
| Pollen / nectar blocking gate | `gatePollen` / `gateNectar` | Positional servos |
| Intake and nectar feeder | `intakeMain` | Motor |
| Pollen transfer | `intakeSmall` | Motor |
| Left / right pollen transfer | `pollenTransferLeft` / `pollenTransferRight` | Continuous-rotation servos |

The names are defined in [HardwareNames.java][hardware-names]. Simple TeleOp uses the same motor and servo names as Shooter TeleOp. The Pinpoint forward pod connects to its X input; the lateral pod connects to Y. See the [official Pinpoint setup guide](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint) for mounting and wiring.

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
| Origin | Bottom-left corner `(0, 0)`; BIOBUZZ diagram field center is `(70.75, 70.75)` inches. |
| X / Y | Right / up in Pedro's diagram, in inches. |
| Hive height | Inches above the floor, at the intended scoring aim point. |
| Start heading | Degrees: right = 0°, up = 90°, left = 180°, down = 270°. |

1. Identify the robot point that your odometry tracks—normally the configured center of rotation. Measure starts and shooter offsets from this same reference, not a bumper or the Pinpoint enclosure.
2. Place the robot at each starting position and record that reference point's X, Y, and the chassis heading. Left/right labels are from that alliance's view into the field.
3. Replace the three `UNMEASURED` values in each `StartPosition` entry with its **X, Y, heading**.
4. Check the configured nominal hive opening centers against your field: red **(58.00, 54.98, 59.55)** and blue **(83.50, 86.52, 59.55)**, as **X, Y, height** in inches. Both shooters use their alliance's same upward-facing cell. See [the dimension derivation and coordinate convention](docs/FIELD_COORDINATES.md).
5. Deploy, select the matching start during Shooter TeleOp initialization, and check the displayed coordinates and distance against your measurements.

Both red starts use the same fixed red hive aim point; both blue starts use the same fixed blue hive aim point. Changing starts never mirrors or moves the targets. These aim points assume the initial upward-facing cells and do not update after a tip. Measure all four starts eventually; Shooter TeleOp needs the selected start measured, while Turret Tuner uses its own local practice-target frame.

## Calibrate the shooters and feeding

**Use Turret Tuner for this.** It assumes Pedro is already tuned and takes over the repetitive work: Pedro measures shot distances, and four successful observations estimate two launch angles, the wheel-speed correction **k**, and horizontal mounting angle. You do not need a protractor, measured field starts, or repeated tape-measure distances.

The tuner does not detect hits. You find repeatable hits with live hood/RPM controls and tell it which shots succeeded. Its estimates use the same projectile model as Shooter TeleOp, so verify them at several distances before using them in a match.

### 1. Enter the few measurements that are still needed

In [ShooterConfig.java][shooter-config], set these separately for `POLLEN` and `NECTAR` in the `static { ... }` block. For example, assign `POLLEN.wheelDiameter` to the wheel's actual diameter. These are one-time geometry/specification inputs, not settings to change after every shot.

| Setting | What to enter |
| --- | --- |
| `launchHeight` | Ball exit height above the floor, in **meters**. Needed for the real hive heights in Shooter TeleOp. |
| `forwardOffset`, `leftOffset` | Ball exit location relative to the tracked robot reference, in **meters**. Forward/left positive; backward/right negative. |
| `wheelDiameter` | Ball-contact wheel diameter in **meters**, from its specification or a measurement. |
| `wheelToMotorRatio` | Wheel turns per motor output-shaft turn. `2.0` means two wheel turns for one motor turn. |
| `ticksPerMotorRevolution` | Encoder ticks per motor output-shaft revolution, including any internal gearbox; use the motor specification. |
| `motorDirection` | Direction that launches forward for a positive velocity command. |
| `maxWheelRpm` | A mechanically appropriate, achievable wheel-speed limit. The example 5000 is not measured for this robot. |

Multiply inches by **0.0254** to get meters. Deploy after changing these source settings. The tuner estimates `minAngle`, `maxAngle`, both corresponding servo positions, `rpmCorrection`, and `headingOffset`; you do not need to measure those angles manually.

For your practice target, measure its aim point's **vertical height above the ball's exit point**. Enter that difference in the tuner, in **inches**, separately for each shooter. The default 24 inches is just a starting value. Use a target at least **6 inches above** the exit point, preferably higher: a target level with the launcher cannot determine both angle and speed correction from these observations.

**Set the blocking gates before running either shooter OpMode.** Configure `gatePollen` and `gateNectar` as positional servos. Use **Test Hardware** (Driver Station Settings → Utility OpModes) with no balls loaded to find a position that blocks the ball path and one that rotates fully clear. Select the gate with G1 D-pad, choose the displayed position with the left stick Y, and press RB to apply it, staying within the linkage's free travel.

Set these independently in ShooterConfig's `static` block and redeploy:

```java
// EXAMPLES ONLY: replace with the positions found on the robot.
POLLEN.gateClosedPosition = 0.0;
POLLEN.gateOpenPosition = 0.5;
NECTAR.gateClosedPosition = 0.0;
NECTAR.gateOpenPosition = 0.5;
```

Positions range from 0 to 1; open may be numerically lower than closed. Both gates are commanded closed at INIT. Gate positions always come from ShooterConfig, are not inferred from shots, and are not overridden by saved calibration. Existing saved shot profiles remain usable when gate positions change. The shoot command opens the gate and runs the feeders together; there is no separate gate-opening delay.

### 2. Set up Turret Tuner

1. Mark the floor directly below the practice target's aim point. This can be a temporary target; it does not have to be an alliance hive.
2. Put the robot's **tracked odometry reference point** over the mark. Do not use a bumper, the shooter exit, or the Pinpoint enclosure as the reference.
3. Select **Turret Tuner**, press INIT, and keep still during Pinpoint initialization. Press Play while still over the mark. This establishes local `(0, 0)` at the target; the initial heading may be any direction.
4. Drive away using G1's sticks. If the target would obstruct the robot, install it over the mark after moving away. Do not restart the OpMode or move the target during a calibration.
5. Select pollen with **G2 A** or nectar with **G2 B**. Use G2 D-pad left/right to select **Height**, then up/down to enter the measured vertical difference. Each shooter has its own setting.

Only height and basic shooter geometry are measured by hand; Pedro tracks all subsequent horizontal distances. After restarting the OpMode, repeat the floor-mark setup and enter the target height again. FieldConfig and the four alliance starts are not used here.

**Turret Tuner controls** — these differ from Shooter TeleOp:

| Control | Action |
| --- | --- |
| **G1 sticks** | Slow, robot-centric drive/turn. Release hood, spin, feed, and reverse controls to drive. |
| **G1 LB** | In Verify mode, stop translation and aim at the practice target. Aim before holding spin-up. |
| **G1 Y** | Switch Manual/Verify when a fitted or saved calibration is available. |
| **G2 A / B** | Select pollen / nectar; the other flywheel stops. |
| **G2 D-pad left/right** | Select the value to edit: hood (or k in Verify), RPM, or target height. |
| **G2 D-pad up/down** | Change that value. Hold to repeat. Steps: servo 0.005, RPM 50, height 0.5 inch, k 1%. |
| **Hold G2 LB** | Apply the hood position without spinning. **This is not intake in the tuner.** |
| **Hold G2 RB** | Apply the hood and spin the selected flywheel. |
| **Hold G2 RB + RT** | Open the selected gate and feed through both paths once stationary and RPM has settled. The other gate stays closed. RT alone does not spin or feed. |
| **Hold G2 X** | Stop flywheels and reverse both feed paths. |
| **G2 Y**, after releasing RT | Accept the last fed shot **only if it hit the aim point**. |
| **G2 Back** | Undo one accepted observation and return to Manual. At zero observations, starts Manual tuning. Saved data remains intact. |
| **G2 Start**, controls released | Save the selected profile from Verify mode. |
| **G2 Back + Start together**, controls released | Delete only the selected saved profile and return to source defaults. |

Outputs stop on shooter/mode changes and when an observation is accepted; release the hood/spin/feed controls before holding them again. Hood positions only move while LB or RB is held. Adjust within the mechanism's free travel; the manual tuner intentionally allows positions across 0–1 so you can find the usable range.

### 3. Record four successful shots per shooter

Begin in **Manual**. A saved profile starts in Verify; press G1 Y to switch to Manual when retuning. The screen tells you which observation is next.

1. **Low hood, first distance:** select Hood and hold G2 LB while adjusting to a visibly low, usable launch angle. Select RPM and adjust it. Aim manually with G1, stop, and hold G2 RB. Wait for **READY**, then add RT for a shot. If it misses, adjust RPM/aim and try again. Once hits are repeatable, release RT and press G2 Y to accept a representative hit.
2. **Same hood, second distance:** release controls and drive at least **12 inches nearer or farther** in horizontal distance from the target. More separation improves the fit. Keep the hood fixed; the tuner locks it for this pair. Find a hit by changing RPM and aim, then accept it with Y. Moving sideways the same distance from the target does not count as a different distance.
3. **High hood, first distance:** the tuner now asks for a visibly **higher launch angle**. Select and apply a suitable hood position with LB, find repeatable hits, and accept one. A higher angle can mean a lower servo number, depending on the linkage.
4. **Same high hood, second distance:** again move at least 12 inches nearer/farther, leave the hood fixed, adjust RPM/aim, and accept a hit.

The first feed command captures the current pose and measured RPM. Pressing Y accepts that captured observation, not the robot's later location. A fresh RT press replaces a missed shot with a new observation. Feed one ball per trial; keep the unused feed path empty because both paths run together.

For feeding, the tuner waits for no drive command, little pose movement, and RPM within **3% or 50 RPM**, whichever is larger. It requires a settling interval before showing READY. This confirms stable speed, not accurate aim or a detected hit. Wait for the hood itself to settle too.

After four accepted hits, the tuner checks the fit and enters **Verify**. It rejects pairs with too little distance separation, inconsistent launch angles/corrections, or inconsistent horizontal aim. Read the displayed reason, use Back to undo observations as needed, and retry. Target height is locked after the first accepted hit; undo all observations to correct it.

### 4. Verify and save

In **Verify**, the existing shooter math chooses hood angle and RPM from the live Pedro distance. The fitted two hood positions define the usable angle range; they do not need to be the mechanism's hard stops.

1. Release G2 controls, move to a different shooting distance, and hold G1 LB to align. Then hold G2 RB, wait for READY, and add RT.
2. Check several shots at near, middle, and far distances. Verify an intermediate hood setting too: the code still assumes angle changes linearly with servo position.
3. If hits consistently fall short while RPM follows its command, select **k** and increase it a few 1% steps. If they consistently go long, decrease it. These edits take effect live. If the error changes strongly with distance, revisit height, odometry, the original observations, or the hood linkage instead of correcting just one distance.
4. Release hood/spin/feed controls and press **G2 Start**. Wait for **SAVED**. Repeat the process for the other shooter.

Saving does **not** rewrite Java. It stores both independent profiles in **`SillyServosShooterCalibration.json`** in the Robot Controller's FTC settings directory. Normal code deployment keeps this file; back it up before replacing/resetting the controller. Observations and practice-target height are session-only and are not saved.

**Shooter TeleOp automatically loads the saved profiles at its next INIT**, with a status message for each shooter. No rebuild is needed after a calibration save. Saved angles, servo endpoints, k, and heading offset override the corresponding ShooterConfig defaults. Changing those defaults alone will not override a saved profile: delete that profile in the tuner if you want to use the source values instead.

If wheel geometry, encoder gearing, launch position, hood name, or motor direction changes in code, the loader ignores the old profile and asks for retuning. If the physical robot changes without corresponding code changes, retune it yourself. Invalid/unreadable profiles fall back to source defaults with a message; those example defaults are not evidence of a calibrated shooter.

### 5. Check feeding and chassis aim

Set feed directions and powers in [RobotConstants][robot-constants]: `INTAKE_DIRECTION`, `POLLEN_TRANSFER_DIRECTION`, `TRANSFER_LEFT_SERVO_DIRECTION`, and the three feed-power magnitudes. Positive motion must feed toward the shooters; the right transfer servo is automatically opposite the left. Verify with reduced power before loading balls.

The SDK's **Test Hardware** utility can check individual motors before field measurements are available: Driver Station **Settings → Utility OpModes → Test Hardware**. Follow its on-screen controls. Unlike these TeleOps, releasing RB in that utility does not zero a motor; apply zero or press Stop before changing devices. [FTC utility instructions](https://ftc-docs.firstinspires.org/en/latest/utility_opmodes/index.html).

If measured flywheel RPM will not follow the command, check motor model, encoder wiring, gearing, and resolution first. The tuner does not tune the SDK velocity PIDF controller, automatically find a safe maximum RPM, or fix a nonlinear hood linkage. Do not use k to hide an unreachable speed.

In Shooter TeleOp, hold G1 LB and watch **Aim error**. Reduce `AIM_KP` or `MAX_AIM_POWER` if it overshoots; increase cautiously if it barely turns. Increase `AIM_TOLERANCE_RADIANS` slightly if it keeps making tiny corrections after aligning. Turret Tuner's Verify mode uses the same gain/tolerance but caps turning power at 0.3. These settings are independent of Foresight.

**Ready-to-use checkpoint:** verify the saved profiles loaded, then check real hive shots from several positions with correct FieldConfig measurements. Shooter TeleOp still feeds immediately on RT; the tuner's settling gate only applies in **Turret Tuner**.

## Shooting Auto: four starts, two shooting positions

**Shooting Auto** drives to a shooting point and fires **pollen first, then nectar**. Both shooters use the same robot X/Y location for that alliance; the chassis turns between them if their aim points or mounting angles differ.

| Selected start | Robot shooting destination | Targets |
| --- | --- | --- |
| Red Right | Red shooting position | Red pollen hive, then red nectar hive |
| Red Left | The same red shooting position | The same red hives |
| Blue Right | Blue shooting position | Blue pollen hive, then blue nectar hive |
| Blue Left | The same blue shooting position | The same blue hives |

### Configure the two destinations

In [FieldConfig.java][field-config], each `Alliance` entry starts with a **`ShootingPosition(xInches, yInches)`**, followed by the upward-facing cell `Target` shared by pollen and nectar. Replace the two `UNMEASURED` values in each ShootingPosition with the location where the **robot's odometry reference point** should stop. This is different from the hive's location. Both coordinates use the same bottom-left-origin Pedro field frame as the starts and hives.

Only two shooting locations are needed: one in `Alliance.RED` and one in `Alliance.BLUE`. No additional shooting-heading measurement is required. The code calculates each shooter's chassis heading using its fixed mounting angle and forward/left launch offsets.

The route from each start is initially a **straight line** to its alliance's shooting point, with heading changing toward the pollen aim heading during travel. Check clearance for all four routes, including the robot's width and rotation. There is no obstacle detection or automatic detour planning. If a route crosses a field obstacle, change the path in [ShootingAuto.java][shooting-auto] before running it.

### Run the routine

1. Apply the Mecanum, Pinpoint, and **Foresight** tuning results. `Constants.foresightConfig` must be filled in.
2. Measure the selected start, that alliance's shooting position, and both of its hive aim points. Calibrate both shooters and gate endpoints. Saved turret profiles load at INIT, just as in Shooter TeleOp; manually calibrated source defaults also work.
3. Deploy, choose **Shooting Auto** from the autonomous menu, and press INIT. Keep the robot still during Pinpoint initialization.
4. Use **G1 D-pad left/right** to select Red Right, Red Left, Blue Right, or Blue Left. Read the setup/calibration status. Place the robot at that exact start and heading and load the intended balls.
5. Press Play. The selection locks and the robot follows its route. If required measurements, Foresight, or a valid shot for either shooter are missing, it stays stopped and reports the reason.

At the destination, Pedro holds the robot in position while the pollen hood/flywheel spin up. The routine waits until position, aim, motion, and RPM remain within tolerance, opens the pollen gate, waits for the gate to rotate, then feeds. Next it closes pollen, stops that flywheel, turns toward the nectar aim point while holding the same X/Y, and repeats for nectar. Both feed motors and transfer servos run during either feed period; only the active shooter's gate opens.

If aim, speed, position, or RPM falls out of tolerance, feeding pauses and the gate closes. Once ready again, it repeats the settling/gate delay and resumes the remaining feed time. After both feed periods finish, all motor/feed commands stop and both gates close. Driver Station Stop, a timeout, or invalid odometry/shot geometry also stops the routine.

### Tune timing and arrival tolerances

Edit [AutonomousConfig.java][auto-config] and redeploy. These are starting settings to verify on the robot:

| Setting | Default | Meaning |
| --- | --- | --- |
| `MAX_PATH_SPEED_INCHES_PER_SECOND` | 24 in/s | Pedro path velocity limit during travel. |
| `POLLEN_FEED_SECONDS`, `NECTAR_FEED_SECONDS` | 2 s each | Accumulated time actually feeding each shooter. Tune for the preload and mechanism. |
| `READY_SETTLE_SECONDS` | 0.5 s | Continuous ready time before opening a gate; also allows the hood to settle. |
| `GATE_OPEN_SECONDS` | 0.25 s | Time between commanding the gate open and starting the feeders. |
| `POSITION_TOLERANCE_INCHES` | 1 in | Maximum shooting-position error while firing. |
| `AIM_TOLERANCE_RADIANS` | 2° in radians | Maximum actual shot heading error. |
| `MAX_SHOOT_SPEED_INCHES_PER_SECOND` | 1 in/s | Maximum robot translation speed while firing. |
| `MAX_SHOOT_TURN_RATE_RADIANS_PER_SECOND` | 5°/s in radians | Maximum robot turn rate while firing. |
| `RPM_TOLERANCE_FRACTION`, `RPM_TOLERANCE_MIN` | 5%, 75 RPM | Allowed speed error is the larger of the two. |
| `DRIVE_TIMEOUT_SECONDS` | 12 s | Maximum time to reach the shooting point. |
| `SHOOTER_TIMEOUT_SECONDS` | 8 s per shooter | Maximum time including aiming, spin-up, recovery, and feeding. |
| `TOTAL_TIMEOUT_SECONDS` | 28 s | Overall runtime limit. |

There is no ball-count sensor: completing a feed period does not prove that every ball fired. Observe a loaded run and adjust each feed duration. Time spent waiting for readiness is not counted as feed time, but it does count toward the timeouts. A timeout aborts the routine rather than firing from the wrong position or skipping a failed shooter.

These gates apply only to Shooting Auto; the manual TeleOp controls are unchanged. The autonomous follows Pedro 3's [path creation](https://pedropathing.com/docs/pathing/guide/path-creation) and [Follower APIs](https://pedropathing.com/docs/pathing/guide/path-following) directly, without an additional command-library dependency.

## Tune Foresight for autonomous paths

Foresight learns how the drivetrain accelerates, brakes, and corrects position. **It is required for Shooting Auto, and optional for the TeleOps and timed Template Auto.** Complete the Mecanum and Pinpoint steps before running it.

1. Open AutoTune and choose **Foresight Tuner**. Use the normal robot setup and a representative charged battery, with space ahead, behind, and beside the robot.
2. Follow each prompt and reposition between steps when instructed. The sequence measures forward/strafe speed and deceleration, turning response, braking, and translation response. Some runs coast beyond the entered distance.
3. The initial distance defaults to **48 inches**. The deceleration-speed prompt suggests a value below both measured maximum speeds; it must remain below both. The later braking distance defaults to **36 inches** and is clamped to at least **15 inches**.
4. Let automatic measurements finish. Stop cancels them without accepting partial calibration. If a step reports invalid measurements, correct the cause and rerun rather than copying a failed result.
5. Replace `foresightConfig = null` in [Constants.java][pedro-constants] with the generated Java declaration, add the indicated imports, and deploy.

The [official Foresight guide](https://pedropathing.com/docs/pathing/tuning/foresight) explains the individual movements. The [Pedro setup reference](docs/PEDRO_SETUP.md) lists the imports and configuration details.

Shooting Auto uses `Constants.create(hardwareMap)` for paths and position holding. The Pedro TeleOps and timed Template Auto use `createManual()`; Simple TeleOp uses the motors directly. Applying Foresight results does not turn Template Auto into a path-following autonomous. Pedro's optional [Tests procedure](https://pedropathing.com/docs/pathing/tuning/test) can verify paths after tuning; that procedure is **not currently registered in this project**.

## Troubleshooting

| What you see | What to check first |
| --- | --- |
| Shooting Auto will not start | Read Setup: selected start, alliance ShootingPosition, both hive targets, Foresight, and a valid shot for both shooters are required. |
| Shooting Auto times out before/during firing | Check odometry, path clearance, calibrated heading/hood/RPM, readiness tolerances, and actual feed duration before increasing the timeout. |
| Hardware not found during INIT | Exact configuration names, device types, active configuration, and connected ports. Shooter TeleOp initializes both shooters and all feed hardware. |
| Turret Tuner stays waiting instead of READY | Hold RB, release drive inputs, move at least 12 inches from the target, enter height ≥6 inches, and check measured RPM reaches the request. |
| Turret fit is rejected | Verify the vertical height difference, accept only central hits, separate each pair's distances by ≥12 inches, and keep its hood fixed. Back undoes observations. |
| Calibration changes seem to disappear | G2 Start saves from Verify. Check for SAVED, confirm the same Robot Controller is in use, and read Shooter TeleOp's calibration status at INIT. |
| AutoTune page will not open | Computer on robot Wi-Fi, correct robot IP, `http` rather than `https`, port `10158`, and the latest app deployed/running. |
| Shooter TeleOp stops as soon as Play is pressed | Selected start is still `UNMEASURED`, or an alliance target is invalid. Read the initialization telemetry. |
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
| [ShootingAuto.java][shooting-auto] | Four-start selector, alliance route, hold/aim, pollen and nectar sequence, and timeouts. |
| [AutonomousConfig.java][auto-config] | Autonomous travel speed, readiness limits, gate delay, and feed durations. |
| [TimedShot.java][timed-shot] | Readiness dwell, gate delay, and feed-time accumulation for one autonomous shot period. |
| [SimpleTeleOp.java][simple-teleop] | Manual gamepad operation without Pedro or field measurements. |
| [MecanumDrive.java][mecanum-drive] | Four-motor, robot-centric mecanum mixing, normalization, and braking. |
| [ShooterTeleOp.java][shooter-teleop] | Gamepad controls, start selection, saved calibration loading, and coordination of driving/shooting/feeding. |
| [TurretTuningTeleOp.java][turret-tuner] | Guided manual trials, calculated verification shots, and save/undo controls. |
| [TurretCalibration.java][turret-calibration] | Fits launch angles, k, and horizontal mounting angle from successful shots. |
| [ShooterCalibrationStore.java][calibration-store] | Saves/loads per-shooter calibration and detects changes to source geometry. |
| [HardwareNames.java][hardware-names] | Hardware configuration names. |
| [RobotConstants.java][robot-constants] | Feed directions/powers, manual-drive limits, chassis aim settings, and initial odometry defaults. |
| [FieldConfig.java][field-config] | Four starting poses, two alliance shooting positions, and fixed red/blue hive aim points. |
| [ShooterConfig.java][shooter-config] | Separate pollen/nectar geometry, hood mapping, encoder/gearing data, RPM limits, and k. |
| [Turret.java][turret] | One flywheel, hood, and blocking gate; instantiated once per ball type. |
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

[turret-tuner]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/opmodes/teleop/TurretTuningTeleOp.java
[turret-calibration]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems/TurretCalibration.java
[calibration-store]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/config/ShooterCalibrationStore.java

[simple-teleop]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/opmodes/teleop/SimpleTeleOp.java
[mecanum-drive]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems/MecanumDrive.java

[shooting-auto]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/opmodes/auto/ShootingAuto.java
[auto-config]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/config/AutonomousConfig.java
[timed-shot]: TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems/TimedShot.java
