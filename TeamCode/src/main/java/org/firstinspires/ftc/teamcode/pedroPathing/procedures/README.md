These robot calibration procedures come from the official
[Pedro Quickstart](https://github.com/Pedro-Pathing/Quickstart/tree/master/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures),
retrieved November 3, 2026. Their Java package has been changed to fit this project.
Local fixes preserve Pinpoint readings across Stop, reject invalid calibration values,
bound braking travel, and stop tuning motors on cancellation or failure. The Mecanum
direction check explicitly starts from a forward, open-loop motor configuration.

The upstream Quickstart uses the FIRST license included in this repository's root
`LICENSE` (Copyright (c) 2014-2022 FIRST. All rights reserved.).

`Tuning.java` registers the Mecanum, Pinpoint, and Foresight procedures with AutoTune.
