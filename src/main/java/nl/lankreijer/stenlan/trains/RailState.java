package nl.lankreijer.stenlan.trains;

import net.minecraft.block.enums.RailShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class RailState {
    public BlockPos pos;
    public Direction dir; // direction in which the minecart ENTERED this block
    public RailShape shape;

    public RailState(BlockPos pos, Direction dir, RailShape shape) {
        this.pos = pos;
        this.dir = dir;
        this.shape = shape;
    }

    public Vec3d calcPos(double progress) {
        Vec3d pos = new Vec3d(this.pos.getX(), this.pos.getY(), this.pos.getZ());
        switch (shape) {
            case NORTH_SOUTH:
                switch(dir) {
                    case NORTH:
                        return pos.add(0, 0, 1 - progress);
                    case SOUTH:
                        return pos.add(0, 0, progress);
                    default:
                        return pos.add(0.5, 0, 0.5);
                }
            case EAST_WEST:
                switch(dir) {
                    case EAST:
                        return pos.add(0, 0, progress);
                    case WEST:
                        return pos.add(0, 0, 1-progress);
                    default:
                        return pos.add(0.5, 0, 0.5);
                }
            default:
                return pos.add(0.5, 0, 0.5);
        }
    }
}
