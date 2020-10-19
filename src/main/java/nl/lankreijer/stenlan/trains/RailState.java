package nl.lankreijer.stenlan.trains;

import net.minecraft.block.enums.RailShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.NotImplementedException;

public class RailState {
    public BlockPos pos;
    public Direction dir; // direction in which the minecart ENTERED this block
    public RailShape shape;

    public RailState(BlockPos pos, Direction dir, RailShape shape) {
        this.pos = pos;
        this.dir = dir;
        this.shape = shape;
    }

    private Vec3d toPos(Direction dir) {
        switch(dir) {
            case NORTH:
                return new Vec3d(this.pos.getX() + 0.5, this.pos.getY(), this.pos.getZ());
            case SOUTH:
                return new Vec3d(this.pos.getX() + 0.5, this.pos.getY(), this.pos.getZ() + 1);
            case WEST:
                return new Vec3d(this.pos.getX(), this.pos.getY(), this.pos.getZ() + 0.5);
            case EAST:
                return new Vec3d(this.pos.getX() + 1, this.pos.getY(), this.pos.getZ() + 0.5);
            default:
                throw new NotImplementedException("Invalid direction");
        }
    }

    public Vec3d startPos() {
        Vec3d startPos = this.toPos(this.dir.getOpposite());
        switch (shape) {
            case ASCENDING_EAST:
            case ASCENDING_WEST:
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
                if (dir != RailHelper.toEntryDirs(this.shape).getLeft()) { // not going uphill, TODO: smooth this out?
                    startPos = startPos.add(0, 1, 0);
                }
            default:
                break;
        }
        return startPos;
    }

    public Vec3d endPos() {
        Direction endDir = RailHelper.entryToExit(this.shape, this.dir);
        Vec3d endPos = toPos(endDir);
        switch (shape) {
            case ASCENDING_EAST:
            case ASCENDING_WEST:
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
                if (dir == RailHelper.toEntryDirs(this.shape).getLeft()) { // going uphill
                    endPos = endPos.add(0, 1, 0);
                }
            default:
                break;
        }
        return endPos;
    }

    public Vec3d calcPos(double progress) {
        Vec3d startPos = startPos();
        Vec3d diff = endPos().subtract(startPos);
        return startPos.add(diff.multiply(progress));
    }
}
