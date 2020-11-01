package nl.lankreijer.stenlan.wagons;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import nl.lankreijer.stenlan.interfaces.IMinecartMixin;
import nl.lankreijer.stenlan.trains.RailFollower;
import nl.lankreijer.stenlan.trains.RailHelper;
import nl.lankreijer.stenlan.trains.RailIterator;
import nl.lankreijer.stenlan.trains.RailState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LocomotiveLogic extends CartLogic {
    private ArrayDeque<RailState> prevRails = new ArrayDeque<>();
    private ArrayList<WagonLogic> wagons = new ArrayList<>();
    private static double cartDistance = 1.5d; // at least 1


    public LocomotiveLogic(AbstractMinecartEntity minecart) {
        super(minecart);
    }

    public LocomotiveLogic (IMinecartMixin minecart) {
        super(minecart);
    }

    @Override
    public boolean isLocomotive() {
        return true;
    }

    @Override
    public void afterTick() {
        BlockPos railPos = this.getRailPos();

        BlockState blockState = this.minecart.world.getBlockState(railPos);
        if (!AbstractRailBlock.isRail(blockState)) {
            System.out.println("Block " + railPos.toString() + " is not a rail :(. My pos: " + this.minecart.getPos().toString());
            return;
        }
        if(prevRails.peekFirst() == null) {
            System.out.println("First is null!");
            return;
        }

        RailState firstRailState = prevRails.peekFirst();
        BlockPos latestPos = null;
        RailIterator it = new RailIterator(firstRailState, this.minecart.world);
        for (int i = 0; i < 3 && it.hasNext(); i++) {
            RailState newState = it.next();
            prevRails.addFirst(newState);
            latestPos = newState.pos;
        }
        if (!railPos.equals(latestPos)) {
            System.out.println("Warning: failed to track rails");
        }

        while (prevRails.size() > 1.42 * cartDistance * wagons.size() + 2) {
            prevRails.removeLast();
        }
    }


    private void placeBehind(CartLogic me, RailState railState, double progress) {
        Vec3d pos = railState.calcPos(progress);

        // Vec3d target = pos.subtract(me.getPos());

        me.getMinecart().updatePosition(pos.getX(), pos.getY(), pos.getZ()); // TODO: ?? clientX/clientXVelocity???
        // ((ITrainCart)me).semiTick();
    }

    public void updateWagons() {
        Iterator<RailState> it = prevRails.iterator();
        RailState state = it.next(); // we always have the first cart

        double progress = state.calcProgress(this.minecart.getPos());

        for (int i = 0; i < this.wagons.size(); i++) {
            double distance = progress * state.railLength(); // distance to start of current rail
            while (distance < cartDistance) { // can't be placed on current rail
                state = it.next(); // consider the next block
                distance += state.railLength(); // distance becomes longer by current rail length
            }
            // assertion: distance > cartDistance && distance - state.railLength < cartDistance
            // assertion: distance - cartDistance < state.railLength
            progress = (distance - cartDistance) / state.railLength(); // TODO: change meaning of progress to actual length?
            CartLogic e = wagons.get(i);
            placeBehind(e, state, progress);
            // MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(e).render();
            // ((ITrainCart)e).wagonTick();
        }
    }

    @Override
    public boolean canAddPassenger() {
        return this.minecart.getPassengerList().size() < this.wagons.size() + 1;
    }

    private boolean containsPos(BlockPos pos) {
        for (RailState state : this.prevRails) {
            if (state.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static Direction toDir(BlockPos from, BlockPos to) {
        Vec3i diff = to.subtract(from);
        return Direction.fromVector(MathHelper.clamp(diff.getX(), -1, 1), 0, MathHelper.clamp(diff.getZ(), -1, 1));
    }

    @Nullable
    private AbstractMinecartEntity findOnRail(RailState s, World w) {
        List<AbstractMinecartEntity> results = w.getEntitiesByClass(AbstractMinecartEntity.class,
                new Box(s.pos.getX(), s.pos.getY(), s.pos.getZ(),
                        s.pos.getX() + 1, s.pos.getY() + 2, s.pos.getZ() + 1),
                m -> !m.equals(minecart) && ((IMinecartMixin) m).getLogicType() == MinecartType.NORMAL);
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }


    public boolean addWagon() { // TODO: improve finding the other cart
        BlockState state = minecart.world.getBlockState(this.getRailPos());
        if (!AbstractRailBlock.isRail(state)) {
            return false;
        }

        World world = this.minecart.world;

        RailShape shape = state.get(((AbstractRailBlock)state.getBlock()).getShapeProperty());
        Pair<Direction, Direction> entryDirs = RailHelper.toEntryDirs(shape);

        boolean oneWay = this.hasWagons();

        RailIterator it1;
        if (oneWay) {
            it1 = new RailIterator(this.prevRails.peekLast().reverse(), world, false, true);
        } else {
            it1 = new RailIterator(new RailState(this.getRailPos(), entryDirs.getLeft(), shape), world, false, true);
        }

        RailIterator it2 = new RailIterator(new RailState(this.getRailPos(), entryDirs.getRight(), shape), world, false, true); // go both ways
        AbstractMinecartEntity targetME = null;
        ArrayDeque<RailState> trackToLocomotive = null;

        for (int i = 0; i < 10; i++) { // 10 is max dist
            boolean it1failed = false;
            if (it1.hasNext()) {
                AbstractMinecartEntity me = findOnRail(it1.next(), world);
                if (me != null) {
                    targetME = me;
                    trackToLocomotive = it1.reversedRails();
                    break;
                }
            } else {
                it1failed = true;
            }

            if (it2.hasNext() && !oneWay) {
                AbstractMinecartEntity me = findOnRail(it2.next(), world);
                if (me != null) {
                    targetME = me;
                    trackToLocomotive = it2.reversedRails();
                    break;
                }
            } else if (it1failed) { // both failed
                return false;
            }
        }

        if (targetME == null) { // no minecart found
            return false;
        }

        IMinecartMixin targetMEM = (IMinecartMixin) targetME;
        targetMEM.changeLogic(new WagonLogic(targetME, this));

        WagonLogic targetWagon = (WagonLogic) targetMEM.getLogic();

        if (this.hasWagons()) {
            this.wagons.get(this.wagons.size() - 1).setLastWagon(false);
            trackToLocomotive.removeLast();
        }

        for (Iterator<RailState> it = trackToLocomotive.descendingIterator(); it.hasNext(); ) {
            RailState s = it.next();
            this.prevRails.addFirst(s);
        }

        targetME.noClip = true;
        targetME.setNoGravity(true);
        targetWagon.setLastWagon(true);
        targetME.startRiding(this.minecart);

        this.wagons.add(targetWagon);


        return true;
    }


    private boolean hasWagons() {
        return this.wagons.size() > 0;
    }

    public ArrayList<WagonLogic> getWagons() {
        return this.wagons;
    }
}
