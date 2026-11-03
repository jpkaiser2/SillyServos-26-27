# Pollen and nectar shooters

For guided calibration with minimal measuring, use **Turret Tuner** and follow the
[README walkthrough](../README.md#calibrate-the-shooters-and-feeding). It uses a local
practice target and tuned Pedro odometry to fit hood angles, RPM correction, and horizontal
mounting angle from four successful shots per shooter. It does not require FieldConfig.
Its stationary/RPM feed gate and gamepad controls are specific to the tuner; the controls
below describe Shooter TeleOp.

Shooter TeleOp loads robot-local profiles from `SillyServosShooterCalibration.json` at INIT.
Saved hood angles/endpoints, `rpmCorrection`, and `headingOffset` override ShooterConfig
defaults. The other hardware measurements still come from ShooterConfig. Initialization
reports whether a saved profile loaded or was ignored; changed source geometry requires
retuning. Save/delete profiles from the tuner as described in the README.

Select **Shooter TeleOp** on the Driver Station. It uses the same Pedro stick controls as
TemplateTeleOp. `Turret` owns one flywheel and one hood; the TeleOp creates a separate
instance for each ball type. As in SillyServosShooterMath.pdf, the chassis aims horizontally.

Before Play, press **gamepad 1 D-pad left/right** to cycle through **Red Right, Red Left,
Blue Right, Blue Left**. Place the robot at the selected position and heading. The selection
is locked when Play is pressed; selecting a pollen/nectar shooter during the match keeps the
same alliance.

| Control | Action |
| --- | --- |
| Gamepad 1 D-pad left/right, before Play | Select one of the four starting positions |
| Gamepad 1 left stick | Drive / strafe |
| Gamepad 1 right stick X | Turn |
| Hold gamepad 1 left bumper | Stop translation and turn toward the selected shooter's target |
| Gamepad 2 A | Select pollen |
| Gamepad 2 B | Select nectar |
| Hold gamepad 2 right bumper | Set selected hood angle and spin selected flywheel |
| Hold gamepad 2 left bumper | Run intake and pollen transfer together |
| Hold gamepad 2 right trigger | Spin selected flywheel and run both intake and transfer to feed |
| Hold gamepad 2 X | Reverse intake, transfer, and transfer servos; stop flywheels |
| Release gamepad 2 bumpers, trigger, and X | Stop flywheel, intake, transfer, and transfer servos |

Selecting the other shooter stops the previous one. Stop stops both flywheels, both feed
motors, both transfer servos, and the drive. `IntakeTransfer` owns the two feed motors and
the two continuous-rotation servos. The intake feeds nectar; the separate transfer feeds
pollen. Both motors run during intake and shooting regardless of which shooter is selected.
The pollen transfer servos run in opposite directions whenever its motor is commanded to run,
and both reverse with the motor. X takes priority over intake and shooting.

Hold RB to spin up, wait for the chassis and hood to settle and measured RPM to reach the
command, then hold RT to feed. RT also spins up the selected flywheel if RB is released.
There is no automatic spin-up delay: RT feeds immediately for a valid shot. An invalid shot
does not feed from RT; LB still runs ordinary intake. The PDF assumes a stationary launch;
this implementation does not compensate for robot motion.
The hood stays at its last position when the flywheel stops. Telemetry shows calculated angle,
distance, heading error, and both commanded and encoder-measured wheel RPM.

## Field coordinates and starting positions

Enter the measurements in `config/FieldConfig.java`. They are currently `UNMEASURED`;
the TeleOp shows which values are missing and refuses to start until the selected start
and its alliance's two aim points are filled in. The other alliance can be measured later.

Use the [official Pedro coordinate frame](https://pedropathing.com/docs/pathing/reference/coordinates)
for **every start and hive**, with directions viewed as in Pedro's field diagram:

- Origin `(0, 0)` is the **bottom-left corner**; field center is `(72, 72)` inches.
- `+X` points **right** and `+Y` points **up** in the diagram.
- X/Y coordinates and hive heights in `FieldConfig` are **inches**. Heights are above the floor.
- Start headings are entered in **degrees**: 0 faces +X, 90 faces +Y, 180 faces -X,
  and 270 (or -90) faces -Y. Positive rotation is counterclockwise.

`StartPosition` contains independent `(x, y, heading)` measurements for all four starts,
measured at the **odometry reference point**, with left/right named from that alliance's
view into the field. `Alliance.RED` and `Alliance.BLUE` contain fixed pollen and nectar hive
aim points as `(x, y, height)` in inches. If pollen and nectar aim at the same location, enter the same
coordinates for both. Measure the intended scoring point, not just the hive's outside edge.

At Play, the selected X/Y are passed directly to Pedro, the heading is converted from degrees
to radians, and odometry is initialized once. No origin shift or alliance rotation is applied.
During the match, odometry tracks the robot in this field
frame and each shooter recalculates its distance and aim from that pose to its alliance's
fixed hive. Changing starts never moves, mirrors, or offsets the hive coordinates. Both red
starts use the same red targets, and both blue starts use the same blue targets.

The shooter equations convert both robot and hive positions to meters while preserving this
origin and axis orientation. Shooter hardware measurements in `ShooterConfig` remain in meters.

Telemetry shows the selected start/alliance, fixed hive coordinates in inches, robot-to-hive horizontal
distance, and the launch-point distance used by a valid shot. Launch-point distance includes
the individual shooter's mounting offsets. Position accuracy depends on measured starts,
headings, and correctly calibrated odometry. This TeleOp expects the robot at the selected
start when Play is pressed; it does not inherit a pose after driving an autonomous routine.

## Robot configuration

Add these devices to the Robot Controller's hardware configuration, using the actual motor
types so encoder velocity control has appropriate SDK motor parameters:

| Device | Type | Configuration name |
| --- | --- | --- |
| Pollen flywheel | Encoder-equipped motor | `flywheelPollen` |
| Nectar flywheel | Encoder-equipped motor | `flywheelNectar` |
| Pollen hood | Positional servo | `hoodPollen` |
| Nectar hood | Positional servo | `hoodNectar` |
| Intake / nectar feeder | Motor | `intakeMain` |
| Pollen transfer | Motor | `intakeSmall` |
| Pollen transfer left | Continuous-rotation servo | `pollenTransferLeft` |
| Pollen transfer right | Continuous-rotation servo | `pollenTransferRight` |
| Drive | Four motors | `leftFront`, `rightFront`, `leftBack`, `rightBack` |
| Odometry | goBILDA Pinpoint | `pinpoint` |

Names live in `config/HardwareNames.java`. Servo ports must be assigned on the Robot Controller;
source code names do not create a controller hardware configuration.

Set both transfer servos to continuous-rotation mode/type (not positional hood servos).
In `RobotConstants.java`, adjust `INTAKE_DIRECTION` and `POLLEN_TRANSFER_DIRECTION` so positive
power feeds toward the shooters. `TRANSFER_LEFT_SERVO_DIRECTION` sets the left servo direction;
the right is always the opposite. Tune `INTAKE_POWER`, `POLLEN_TRANSFER_POWER`, and
`TRANSFER_SERVO_POWER` as magnitudes from 0 to 1 for the mechanism.

## Measurements and tuning

**The supplied numbers are examples, not measured robot or field dimensions.**

The steps below also describe manual configuration. Turret Tuner can infer the two hood
angles, their mapping, k, and the horizontal mounting angle, so those do not have to be
measured by hand. Its estimates still require verification at several distances and
assume the hood's angle/servo mapping is linear.

Edit `config/ShooterConfig.java`. `POLLEN` and `NECTAR` are independent configurations.
The robot geometry still uses example values. Assign measured values separately in
the static block, for example `POLLEN.wheelDiameter = ...;` and `NECTAR.rpmCorrection = ...;`.

- `launchHeight`, `wheelDiameter`, `forwardOffset`, and `leftOffset` are **meters**.
  Offsets are measured from the odometry reference point to the ball's launch point, forward
  and left positive. Hive positions and heights are configured separately in `FieldConfig`.
- `headingOffset` is the fixed horizontal mounting angle in radians, counterclockwise positive.
- `minAngle` / `maxAngle` are measured ball launch angles in radians; `servoAtMinAngle` /
  `servoAtMaxAngle` are the corresponding servo positions (0–1). The two-point mapping assumes
  a linear linkage and accepts reversed servo endpoints. Calibrate actual ball launch angles;
  a nonlinear linkage needs a different mapping in `ShooterMath.hoodPosition`.
- `wheelToMotorRatio` is wheel revolutions per motor output-shaft revolution.
  `ticksPerMotorRevolution` includes the motor's internal gearbox but not external gearing.
- Set `motorDirection` so positive encoder velocity launches forward, and set `maxWheelRpm`
  to a measured achievable wheel speed. The class uses `RUN_USING_ENCODER` / `setVelocity`
  with the configured motor's SDK velocity controller. If measured RPM does not track the
  command, verify motor type, encoder wiring, gearing, and velocity PIDF tuning.
- Tune `rpmCorrection` (`k`) separately for pollen and nectar. For a successful shot:
  `k = PI * wheelDiameter * measuredWheelRPM / (60 * theoreticalBallSpeed)` (PDF Eq. 34).
  Do not treat the PDF's illustrative `k = 1.25` as measured calibration.

In `config/RobotConstants.java`, measure the Pinpoint pod offsets in **inches**, select the
installed pod type, and verify encoder and drive directions. Configure starting poses in
`FieldConfig` as described above. Tune `AIM_KP`,
`MAX_AIM_POWER`, and the heading tolerance on the robot. See [Pedro 3 setup](PEDRO_SETUP.md)
for AutoTune and applying its results to `pedroPathing/Constants.java` before using autonomous paths.

## Math implementation

`ShooterMath.solve` rotates each shooter's mounting offsets into field coordinates (Eqs. 27–28),
finds horizontal distance and wrapped chassis heading error (Eqs. 25–29), and chooses
`theta = PI/4 + atan2(h, d)/2` (Eq. 21). It limits this angle to hood travel, then uses Eq. 13:

```text
v = sqrt(g*d*d / (2*cos(theta)^2 * (d*tan(theta) - h)))
wheelRPM = 60*k*v / (PI*D)
motorTicksPerSecond = wheelRPM / wheelToMotorRatio * ticksPerMotorRevolution / 60
```

At the optimal angle this is equivalent to `v = sqrt(g*(h + sqrt(d*d + h*h)))` (Eq. 22).
Using Eq. 13 also preserves the trajectory when the hood limits the angle. A zero-distance,
non-finite, unreachable, or over-RPM-limit shot stops the flywheel and reports no valid shot;
it does not silently reduce RPM and miss the calculated trajectory. `Turret` converts Pedro
inches to meters once before calling the math.

