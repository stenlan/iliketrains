package nl.lankreijer.stenlan.trains;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class RailHelper {
    public static Direction fromVector(Vec3d vec) {
        boolean useX = Math.abs(vec.x) > Math.abs(vec.z);
        return Direction.fromVector(useX ? vec.x > 0 ? 1 : -1 : 0, 0, !useX ? vec.z > 0 ? 1 : -1 : 0);
    }
}
