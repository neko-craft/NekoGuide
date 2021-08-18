package cn.apisium.nekoguide;

import org.bukkit.Location;
import org.bukkit.entity.Player;

final class FourPoints {
    Location p0;
    Location p1;
    Location p2;
    Location p3;

    private final int max = 20;
    private final int min = 8;
    private final int maxH = 3;
    private final int minH = -6;
    private int index = 0;

    FourPoints(final Location l) {
        double r0 = random(min, max);
        double r1 = random(min, max);
        double r2 = random(min, max);
        double r3 = random(min, max);
        double a0 = random(0, 0.5) * Math.PI;
        double a1 = random(0.5, 1) * Math.PI;
        double a2 = random(1, 1.5) * Math.PI;
        double a3 = random(1.5, 2) * Math.PI;
        p0 = l.clone().add(r0 * Math.cos(a0), random(minH, maxH), r0 * Math.sin(a0));
        p1 = l.clone().add(r1 * Math.cos(a1), random(minH, maxH), r1 * Math.sin(a1));
        p2 = l.clone().add(r2 * Math.cos(a2), random(minH, maxH), r2 * Math.sin(a2));
        p3 = l.clone().add(r3 * Math.cos(a3), random(minH, maxH), r3 * Math.sin(a3));
    }


    FourPoints(final Player p) {
        p0 = p1 = p2 = p3 = p.getLocation();
    }

    void next(Location l) {
        double r = random(min, max);
        double a = random(index * 0.5, (index + 1) * 0.5) * Math.PI;
        nextAbs(l.clone().add(r * Math.cos(a), random(minH, maxH), r * Math.sin(a)));
        index++;
        if (index > 3) index = 0;
    }

    void nextAbs(Location l) {
        p0 = p1;
        p1 = p2;
        p2 = p3;
        p3 = l;
    }

    private static double random(double min, double max) {
        return min + Math.random() * (max - min);
    }
}
