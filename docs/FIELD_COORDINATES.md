# BIOBUZZ hive coordinates

`FieldConfig` supplies both Shooter TeleOp and Shooting Auto with these nominal aim
points, in inches. Pollen and nectar use the same upward-facing cell for each alliance.

| Alliance | X | Y | Height above tiles |
| --- | ---: | ---: | ---: |
| Red | 58.00 | 54.9826 | 59.55 |
| Blue | 83.50 | 86.5174 | 59.55 |

The aim point is halfway between the top and bottom of the opening, on its width
centerline. It is a chosen scoring aim point, not the pivot or the cell's volume center.
These targets assume the initial orientation in Figure 10-2 (p. 81) of the supplied
`BIOBUZZ_Competition_Manual_TU07.pdf`: red's upward-facing cell is toward the audience
(-Y), and blue's is away from the audience (+Y). The targets stay fixed after a tip;
following subsequent tips would require tracking which cell is up.

## Field frame

Use the [Pedro BIOBUZZ diagram](https://pedropathing.com/docs/fieldcoordinates-light.png):
bottom-left origin, +X right, +Y away from the audience, red alliance left, blue right.
The diagram labels the field as 141.5 inches square, with center `(70.75, 70.75)`.
`FIELD_SIZE_INCHES` and `FIELD_CENTER_INCHES` record that convention explicitly.

The [Pedro FTC conversion example](https://pedropathing.com/docs/pathing/reference/coordinates#converting-ftc-coordinates)
uses a nominal 72-inch center offset, and the manual describes the field as approximately
144 inches square (p. 62). For this project's BIOBUZZ diagram frame, convert centered FTC
coordinates using `pedroX = ftcY + 70.75`, `pedroY = 70.75 - ftcX`, and
`pedroHeading = ftcHeading - pi/2`. This is a translation choice; it does not rescale inches.
Measure robot starts and autonomous shooting positions in this same frame. Coordinates
previously entered using a 72-inch center offset need 1.25 inches subtracted from both X
and Y. No conversion is applied to starts already entered in Pedro coordinates.

## Dimension derivation

Source: supplied BIOBUZZ TU07 manual, Figures 9-8 through 9-11, pp. 68-70.

| Dimension | Inches / degrees |
| --- | ---: |
| Red-to-blue hive center spacing | 25.5 in |
| Level hive outside span | 42.91 in |
| Pivot height | 43.95 in |
| Stable tilt | 30 degrees |
| Raised opening bottom / top height | 53.5 / 65.6 in |

The structure is centered on the field (Section 9.6). Each hive is therefore 12.75
inches to either side of field center. The aim height is `(53.5 + 65.6) / 2 = 59.55`.

To derive the horizontal distance `d` from the pivot to the raised opening center,
use the outer opening plane at half the level hive span, `r = 42.91 / 2`. Inverting
the 30-degree rotation gives:

```text
heightAbovePivot = 59.55 - 43.95 = 15.60
r = d*cos(30 degrees) + heightAbovePivot*sin(30 degrees)
d = (21.455 - 15.60*sin(30 degrees)) / cos(30 degrees)
  = 15.7674358516 inches

red  = (70.75 - 12.75, 70.75 - d, 59.55)
blue = (70.75 + 12.75, 70.75 + d, 59.55)
```

This uses the outside opening plane as the nominal target plane. The 18.84-inch
dimension is the gap between cells, not their opening-center separation. The code
keeps the calculation rather than rounding the resulting Y coordinates.

The manual's dimensions are nominal, generally with ±1-inch tolerances (Section 9.1,
p. 61); they cannot establish an exact measured target on every assembled field.
Verify the opening centers on your field and tune actual shots. Robot starting poses
and autonomous shooting positions remain unset because they depend on robot placement
and the team's chosen route.
