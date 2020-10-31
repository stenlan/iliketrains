package nl.lankreijer.stenlan.trains;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;

public class RailIterator implements Iterator<RailState> {
    private World world;
    private BlockPos trackingPos;
    private Direction trackingDir;
    private RailShape trackingShape;
    private boolean track = false;
    private boolean trackReverse = false;

    private ArrayList<RailState> prevRails = new ArrayList<>();
    private ArrayDeque<RailState> prevRailsReverse = new ArrayDeque<>();

    public RailIterator(RailState startingState, World world) {
        this.world = world;
        this.trackingPos = startingState.pos;
        this.trackingDir = startingState.dir;
        this.trackingShape = startingState.shape;
    }

    public RailIterator(RailState startingState, World world, boolean track) {
        this(startingState, world);
        this.track = track;
        if (track) prevRails.add(startingState);
    }

    public RailIterator(RailState startingState, World world, boolean track, boolean trackReverse) {
        this(startingState, world, track);
        this.trackReverse = trackReverse;
        if (trackReverse) prevRailsReverse.addFirst(startingState.reverse());
    }

    @Override
    public boolean hasNext() {
        BlockPos trackingPos = this.trackingPos.add(RailHelper.entryToExitOffset(trackingShape, trackingDir));
        BlockState state = world.getBlockState(trackingPos);
        if (!AbstractRailBlock.isRail(state)) {
            trackingPos = trackingPos.down();
            state = world.getBlockState(trackingPos);
            if (!AbstractRailBlock.isRail(state)) { // error
                return false;
            }
        }
        return true;
    }

    @Override
    public RailState next() {
        Direction oldExitDir = RailHelper.entryToExitDir(trackingShape, trackingDir);
        trackingPos = trackingPos.add(RailHelper.entryToExitOffset(trackingShape, trackingDir));
        BlockState state = world.getBlockState(trackingPos);
        if (!AbstractRailBlock.isRail(state)) {
            trackingPos = trackingPos.down();
            state = world.getBlockState(trackingPos);
        }
        // assertion: blockState is rail
        trackingDir = oldExitDir;
        trackingShape = state.get(((AbstractRailBlock)state.getBlock()).getShapeProperty());
        RailState newState = new RailState(trackingPos, trackingDir, trackingShape);
        if (track) {
            this.prevRails.add(newState);
        }

        if (trackReverse) {
            this.prevRailsReverse.addFirst(newState.reverse());
        }
        return newState;
    }

    public ArrayDeque<RailState> reversedRails() {
        return this.prevRailsReverse;
    }
}
