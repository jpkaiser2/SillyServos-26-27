package org.firstinspires.ftc.teamcode.verydumbpathing;

public enum Units {
    Tile(60.95), FeildLength(365.75);

    public double Cm;
    public double Mm;
    Units(double cm) {
        this.Cm = cm;
        this.Mm = cm*10;
    }
}
