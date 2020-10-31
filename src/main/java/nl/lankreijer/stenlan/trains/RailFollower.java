package nl.lankreijer.stenlan.trains;

import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class RailFollower implements Iterable<RailState> {
    private RailState startingState;
    private World world;

    public RailFollower(RailState startingState, World world) {
        this.startingState = startingState;
        this.world = world;
    }

    @NotNull
    @Override
    public Iterator<RailState> iterator() {
        return new RailIterator(startingState, world);
    }
}