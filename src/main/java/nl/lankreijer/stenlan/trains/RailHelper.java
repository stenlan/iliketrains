package nl.lankreijer.stenlan.trains;

import net.minecraft.block.enums.RailShape;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RailHelper {
    public static Direction fromVector(Vec3d vec) {
        boolean useX = Math.abs(vec.x) > Math.abs(vec.z);
        return Direction.fromVector(useX ? vec.x > 0 ? 1 : -1 : 0, 0, !useX ? vec.z > 0 ? 1 : -1 : 0);
    }

    private static Map<RailShape, Pair<Direction, Direction>> DIR_MAP = Arrays.stream(RailShape.values()).collect(Collectors.toMap(d -> d, (shape) -> {
        switch (shape) {
            case NORTH_SOUTH:
            case ASCENDING_SOUTH:
                return new Pair<>(Direction.NORTH, Direction.SOUTH);
            case ASCENDING_NORTH:
                return new Pair<>(Direction.SOUTH, Direction.NORTH);
            case EAST_WEST:
            case ASCENDING_WEST:
                return new Pair<>(Direction.EAST, Direction.WEST);
            case ASCENDING_EAST:
                return new Pair<>(Direction.WEST, Direction.EAST);
            case SOUTH_EAST:
                return new Pair<>(Direction.SOUTH, Direction.EAST);
            case SOUTH_WEST:
                return new Pair<>(Direction.SOUTH, Direction.WEST);
            case NORTH_WEST:
                return new Pair<>(Direction.NORTH, Direction.WEST);
            case NORTH_EAST:
                return new Pair<>(Direction.NORTH, Direction.EAST);
            default:
                throw new NotImplementedException("Invalid railshape");
        }
    }));

    private static Map<RailShape, Pair<Direction, Direction>> DIR_MAP_EXIT = Arrays.stream(RailShape.values()).collect(Collectors.toMap(d -> d, (shape) -> {
        Pair<Direction, Direction> entrances = DIR_MAP.get(shape);
        return new Pair<>(entrances.getLeft().getOpposite(), entrances.getRight().getOpposite());
    }));

    public static Pair<Direction, Direction> toEntryDirs(RailShape shape) {
        return DIR_MAP.get(shape);
    }

    public static Pair<Direction, Direction> toExitDirs(RailShape shape) {
        return DIR_MAP_EXIT.get(shape);
    }

    public static Direction entryToExit(RailShape shape, Direction entryDir) {
        Pair<Direction, Direction> entryDirs = toEntryDirs(shape);
        Pair<Direction, Direction> exitDirs = toExitDirs(shape);
        if (entryDirs.getLeft() == entryDir) { // if we can properly follow the rail
            return exitDirs.getRight();
        } else if (entryDirs.getRight() == entryDir) { // if we can properly follow the rail the other way around
            return exitDirs.getLeft();
        } else { // we don't enter the rail at one of its "openings"
            switch (shape) { // TODO: optimize this
                case ASCENDING_NORTH:
                case NORTH_SOUTH: // weird behavior that happens when a cart enters a straight rail perpendicularly
                    return Direction.SOUTH;
                case ASCENDING_WEST:
                case EAST_WEST: // same here
                    return Direction.EAST;
                case ASCENDING_EAST:
                    return Direction.WEST;
                case ASCENDING_SOUTH:
                    return Direction.NORTH;
                default: // for corner rails
                    return entryDir;
            }
        }
    }
}
