# Pollen and nectar shooters

Select **Shooter TeleOp** on the Driver Station. It uses the same Pedro stick controls as
TemplateTeleOp. `Turret` owns one flywheel and one hood; the TeleOp creates a separate
instance for each ball type. As in SillyServosShooterMath.pdf, the chassis aims horizontally.

| Control | Action |
| --- | --- |
| Gamepad 1 left stick | Drive / strafe |
| Gamepad 1 right stick X | Turn |
| Hold gamepad 1 left bumper | Stop translation and turn toward the selected shooter's target |
| Gamepad 2 A | Select pollen |
| Gamepad 2 B | Select nectar |
| Hold gamepad 2 right bumper | Set selected hood angle and spin selected flywheel |
| Release gamepad 2 right bumper | Stop flywheel |

Selecting the other shooter stops the previous one. Stop stops both flywheels and the drive.
There is no feeder/intake control in this TeleOp. Wait for the chassis and hood to settle and
the measured RPM to reach the command before feeding a ball. The PDF assumes a stationary
launch; this implementation does not compensate for robot motion or automatically feed balls.
The hood stays at its last position when the flywheel stops. Telemetry shows calculated angle,
distance, heading error, and both commanded and encoder-measured wheel RPM.

## Robot configuration

Add these devices to the Robot Controller's hardware configuration, using the actual motor
types so encoder velocity control has appropriate SDK motor parameters:

| Device | Type | Configuration name |
| --- | --- | --- |
| Pollen flywheel | Encoder-equipped motor | `flywheelPollen` |
| Nectar flywheel | Encoder-equipped motor | `flywheelNectar` |
| Pollen hood | Positional servo | `hoodPollen` |
| Nectar hood | Positional servo | `hoodNectar` |
| Drive | Four motors | `leftFront`, `rightFront`, `leftBack`, `rightBack` |
| Odometry | goBILDA Pinpoint | `pinpoint` |

Names live in `config/HardwareNames.java`. Servo ports must be assigned on the Robot Controller;
source code names do not create a controller hardware configuration.

## Measurements and tuning

**The supplied numbers are examples, not measured robot or field dimensions.**

Edit `config/ShooterConfig.java`. `POLLEN` and `NECTAR` are independent configurations.
Their default geometry reproduces the PDF's example. Assign measured values separately in
the static block, for example `POLLEN.wheelDiameter = ...;` and `NECTAR.rpmCorrection = ...;`.

- `targetX`, `targetY`, `targetHeight`, `launchHeight`, `wheelDiameter`, `forwardOffset`, and
  `leftOffset` are **meters**. Offsets are measured from the odometry reference point to the
  ball's launch point, forward and left positive. Targets share the odometry field frame.
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
installed pod type, verify encoder and drive directions, and enter the initial field pose
(inches, inches, radians). Place the robot at that pose before Init. Tune `AIM_KP`,
`MAX_AIM_POWER`, and the heading tolerance on the robot. Pedro defaults still need robot-specific
drivetrain/path-following tuning before using autonomous paths.

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

