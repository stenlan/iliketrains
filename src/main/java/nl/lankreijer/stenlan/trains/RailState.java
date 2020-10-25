package nl.lankreijer.stenlan.trains;

import net.minecraft.block.enums.RailShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.NotImplementedException;

public class RailState {
    private static final double SQRT_2 = Math.sqrt(2.0);
    private static final double SQRT_0_5 = Math.sqrt(0.5);

    public BlockPos pos;
    public Direction dir; // direction in which the minecart ENTERED this block
    public RailShape shape;

    public RailState(BlockPos pos, Direction dir, RailShape shape) {
        this.pos = pos;
        this.dir = dir;
        this.shape = shape;
    }

    public double railLength() {
        switch (shape) {
            case ASCENDING_EAST:
            case ASCENDING_WEST:
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
                return SQRT_2;
            case SOUTH_EAST:
            case SOUTH_WEST:
            case NORTH_WEST:
            case NORTH_EAST:
                return SQRT_0_5;
            default:
                return 1;
        }
    }

    private Vec3d toPos(Direction dir) {
        switch(dir) {
            case NORTH:
                return new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 0.0625, this.pos.getZ());
            case SOUTH:
                return new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 0.0625, this.pos.getZ() + 1);
            case WEST:
                return new Vec3d(this.pos.getX(), this.pos.getY() + 0.0625, this.pos.getZ() + 0.5);
            case EAST:
                return new Vec3d(this.pos.getX() + 1, this.pos.getY() + 0.0625, this.pos.getZ() + 0.5);
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
        Direction endDir = RailHelper.entryToExitDir(this.shape, this.dir);
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

    public Direction exitDir() {
        return RailHelper.entryToExitDir(this.shape, this.dir);
    }

    public Vec3d calcPos(double progress) {
        Vec3d startPos = startPos();
        Vec3d diff = endPos().subtract(startPos);
        return startPos.add(diff.multiply(progress));
    }

    public double calcProgress(Vec3d pos) {
        Vec3d startPos = startPos();
        Vec3d endDiff = endPos().subtract(startPos);
        Vec3d currDiff = pos.subtract(startPos);
        return Math.sqrt(currDiff.lengthSquared() / endDiff.lengthSquared()); // assumes pos is on line
    }
}
