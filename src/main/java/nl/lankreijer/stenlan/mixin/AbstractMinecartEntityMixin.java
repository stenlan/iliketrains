package nl.lankreijer.stenlan.mixin;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import nl.lankreijer.stenlan.interfaces.ITrainCart;
import nl.lankreijer.stenlan.trains.RailHelper;
import nl.lankreijer.stenlan.trains.RailState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements ITrainCart {
    private static double cartDistance = 1.5d; // at least 1
    private ArrayList<MinecartEntity> wagons = new ArrayList<>();
    private ITrainCart locomotive;
    ArrayDeque<RailState> prevRails = new ArrayDeque<>();
    private boolean isWagon = false;
    private boolean isLastWagon = false;
    private boolean isLocomotive = false;

    @Shadow
    private double clientX;
    @Shadow
    private double clientY;
    @Shadow
    private double clientZ;

    @Shadow
    private int clientInterpolationSteps;

    @Shadow
    private double clientYaw;

    @Shadow
    private double clientPitch;

    @Shadow
    private boolean yawFlipped;


    public AbstractMinecartEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow public abstract Direction getMovementDirection();

    @Inject(method="tick()V", at=@At("HEAD"), cancellable=true)
    public void tickInjectHead(CallbackInfo ci) {
        if (this.isWagon) {
            ci.cancel();
        }
    }

    @Inject(method="tick()V", at=@At(value="INVOKE", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;setRotation(FF)V", ordinal=0)) // Order-dependent
    public void tickInject(CallbackInfo ci) {
        if (this.isLocomotive) {
            BlockPos railPos = this.getRailPos();

            BlockState blockState = this.world.getBlockState(railPos);
            if (!AbstractRailBlock.isRail(blockState)) {
                System.out.println("Block " + railPos.toString() + " is not a rail :(. My pos: " + this.getPos().toString());
                return;
            }
            if(prevRails.peekFirst() == null) {
                System.out.println("First is null!");
                return;
            }

            RailState firstRailState = prevRails.peekFirst();
            BlockPos trackingPos = firstRailState.pos;
            Direction trackingDir = firstRailState.dir;
            RailShape trackingShape = firstRailState.shape;
            for (int i = 0; i < 3 && !trackingPos.equals(railPos); i++) {
                Direction oldExitDir = RailHelper.entryToExitDir(trackingShape, trackingDir);
                trackingPos = trackingPos.add(RailHelper.entryToExitOffset(trackingShape, trackingDir));
                BlockState state = this.world.getBlockState(trackingPos);
                if (!AbstractRailBlock.isRail(state)) {
                    trackingPos = trackingPos.down();
                    state = this.world.getBlockState(trackingPos);
                    if (!AbstractRailBlock.isRail(state)) { // error
                        break;
                    }
                }
                // assertion: blockState is rail
                trackingDir = oldExitDir;
                trackingShape = state.get(((AbstractRailBlock)state.getBlock()).getShapeProperty());
                prevRails.addFirst(new RailState(trackingPos, trackingDir, trackingShape));
            }
            if (!trackingPos.equals(railPos)) {
                System.out.println("Warning: failed to track rails");
            }

            while (prevRails.size() > 1.42 * cartDistance * wagons.size() + 2) {
                prevRails.removeLast();
            }
        }
    }

    @Inject(method="tick()V", at=@At(value="INVOKE", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;setRotation(FF)V", ordinal=1), cancellable = true)
    public void tickInject2(CallbackInfo ci) {
        tickInject(ci);
    }

    @Inject(method="tick()V", at=@At(value="FIELD", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;firstUpdate:Z"), cancellable = true)
    public void tickInject3(CallbackInfo ci) {
        tickInject(ci);
    }

    private void placeBehind(MinecartEntity me, RailState railState, double progress) {
        Vec3d pos = railState.calcPos(progress);

        // Vec3d target = pos.subtract(me.getPos());

        me.updatePosition(pos.getX(), pos.getY(), pos.getZ()); // TODO: ?? clientX/clientXVelocity???
        // ((ITrainCart)me).semiTick();
    }

    @Override
    public void semiTick(){
        if (this.world.isClient) {
            if (this.clientInterpolationSteps > 0) {
                double d = this.getX() + (this.clientX - this.getX()) / (double)this.clientInterpolationSteps;
                double e = this.getY() + (this.clientY - this.getY()) / (double)this.clientInterpolationSteps;
                double f = this.getZ() + (this.clientZ - this.getZ()) / (double)this.clientInterpolationSteps;
                double g = MathHelper.wrapDegrees(this.clientYaw - (double)this.yaw);
                this.yaw = (float)((double)this.yaw + g / (double)this.clientInterpolationSteps);
                this.pitch = (float)((double)this.pitch + (this.clientPitch - (double)this.pitch) / (double)this.clientInterpolationSteps);
                --this.clientInterpolationSteps;
                this.updatePosition(d, e, f);
                this.setRotation(this.yaw, this.pitch);
            } else {
                this.refreshPosition();
                this.setRotation(this.yaw, this.pitch);
            }
        } else {
            int i = MathHelper.floor(this.getX());
            int j = MathHelper.floor(this.getY());
            int k = MathHelper.floor(this.getZ());
            if (this.world.getBlockState(new BlockPos(i, j - 1, k)).isIn(BlockTags.RAILS)) {
                --j;
            }

            BlockPos blockPos = new BlockPos(i, j, k);
            BlockState blockState = this.world.getBlockState(blockPos);
            if (AbstractRailBlock.isRail(blockState)) {
                this.moveOnRail(blockPos, blockState);
            } else {
                this.moveOffRail();
            }

            this.pitch = 0.0F;
            double h = this.prevX - this.getX();
            double l = this.prevZ - this.getZ();
            if (h * h + l * l > 0.001D) {
                this.yaw = (float) (MathHelper.atan2(l, h) * 180.0D / 3.141592653589793D);
                if (this.yawFlipped) {
                    this.yaw += 180.0F;
                }
            }

            double m = (double) MathHelper.wrapDegrees(this.yaw - this.prevYaw);
            if (m < -170.0D || m >= 170.0D) {
                this.yaw += 180.0F;
                this.yawFlipped = !this.yawFlipped;
            }

            this.setRotation(this.yaw, this.pitch);
        }
    }

    @Shadow
    protected abstract void moveOnRail(BlockPos blockPos, BlockState blockState);

    @Shadow
    protected abstract void moveOffRail();

    @Override
    public BlockPos getRailPos() {
        Vec3d pos = this.getPos();
        int p = MathHelper.floor(pos.x);
        int q = (int) Math.round(pos.y);
        int r = MathHelper.floor(pos.z);

        if (this.world.getBlockState(new BlockPos(p, q - 1, r)).isIn(BlockTags.RAILS)) {
            --q;
        }

        return new BlockPos(p, q, r);
    }

    private static Direction toDir(BlockPos from, BlockPos to) {
        Vec3i diff = to.subtract(from);
        return Direction.fromVector(MathHelper.clamp(diff.getX(), -1, 1), 0, MathHelper.clamp(diff.getZ(), -1, 1));
    }

    @Override
    public void tickRiding() {
        this.setVelocity(Vec3d.ZERO);
        this.tick();
        if (this.isWagon && this.isLastWagon) {
            this.locomotive.updateWagons();
        } else {
            this.getVehicle().updatePassengerPosition(this);
        }
    }

    @Override
    public void updateWagons() {
        Iterator<RailState> it = prevRails.iterator();
        RailState state = it.next(); // we always have the first cart

        if(state.pos.getX() == 11) {
            state.pos.getX();
        }

//            Vec3d pos;
//            if (this.world.isClient) {
//                System.out.println("yup");
//                double d = this.clientX;
//                pos = new Vec3d(this.clientX, this.clientY, this.clientZ).add(this.getVelocity());
//            } else {
//                pos = this.getPos();
//            }

        double progress = state.calcProgress(this.getPos());

        for (int i = 0; i < this.wagons.size(); i++) {
            double distance = progress * state.railLength(); // distance to start of current rail
            while (distance < cartDistance) { // can't be placed on current rail
                state = it.next(); // consider the next block
                distance += state.railLength(); // distance becomes longer by current rail length
            }
            // distance > cartDistance && distance - state.railLength < cartDistance
            // distance - cartDistance < state.railLength
            progress = (distance - cartDistance) / state.railLength(); // TODO: change meaning of progress to actual length?
            MinecartEntity e = wagons.get(i);
            placeBehind(e, state, progress);
            // MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(e).render();
            // ((ITrainCart)e).wagonTick();
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        System.out.println("canAddPassenger?");
        return this.getPassengerList().size() < (this.isLocomotive ? this.wagons.size() + 1 : 1);
    }

    @Override
    public void track() {
        this.isLocomotive = true;
        BlockPos railPos = getRailPos();
        Direction dir = this.getMovementDirection();
        BlockState blockState = this.world.getBlockState(railPos);
        this.prevRails.addLast(new RailState(railPos, dir, blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty())));
    }

    private boolean containsPos(BlockPos pos) {
        for (RailState state : this.prevRails) {
            if (state.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void refreshPositionAfterTeleport(double x, double y, double z) {
        System.out.println("AAARGGHH");
        super.refreshPositionAfterTeleport(x, y, z);
    }

    @Override
    public void addWagon(MinecartEntity e) { // TODO: improve finding the other cart
        ITrainCart cart = (ITrainCart) e;

        ITrainCart secondLast = this.wagons.size() <= 0 ? this : (ITrainCart) this.wagons.get(this.wagons.size() - 1);
        BlockPos railPos = secondLast.getRailPos();

        if(!this.isLocomotive) {
            this.prevRails.addLast(new RailState(railPos, null, null));
        }

        ArrayList<RailState> newStates = new ArrayList<>();

        BlockPos trackingPos = cart.getRailPos();
        Direction trackingDir = toDir(trackingPos, railPos);
        BlockState state = this.world.getBlockState(trackingPos);
        if (!AbstractRailBlock.isRail(state)) {
            System.out.println("Desired cart is not on rail");
            if (!this.isLocomotive) {
                this.prevRails.clear();
            }
            return;
        }
        RailShape trackingShape = state.get(((AbstractRailBlock)state.getBlock()).getShapeProperty());

        newStates.add(new RailState(trackingPos, trackingDir, trackingShape));

        for (int i = 0; i < 10 && !containsPos(trackingPos); i++) {
            Direction oldExitDir = RailHelper.entryToExitDir(trackingShape, trackingDir);
            trackingPos = trackingPos.add(RailHelper.entryToExitOffset(trackingShape, trackingDir));
            state = this.world.getBlockState(trackingPos);
            if (!AbstractRailBlock.isRail(state)) {
                trackingPos = trackingPos.down();
                state = this.world.getBlockState(trackingPos);
                if (!AbstractRailBlock.isRail(state)) { // error
                    System.out.println("Couldn't follow rail when making train");
                    if (!this.isLocomotive) {
                        this.prevRails.clear();
                    }
                    return;
                }
            }
            // assertion: blockState is rail
            trackingDir = oldExitDir;
            trackingShape = state.get(((AbstractRailBlock)state.getBlock()).getShapeProperty());
            newStates.add(new RailState(trackingPos, trackingDir, trackingShape));
        }

        if (!containsPos(trackingPos)) {
            System.out.println("Couldn't make train");
            if (!this.isLocomotive) {
                this.prevRails.clear();
            }
            return;
        }

        int initialI = newStates.size() - 2;

        if (!this.isLocomotive) {
            this.prevRails.clear();
            initialI++;
        }

        for (int i = initialI; i >= 0; i--) {
            this.prevRails.addLast(newStates.get(i));
        }

        e.noClip = true;
        e.setNoGravity(true);

        if(this.isLocomotive) {
            ((ITrainCart)this.wagons.get(this.wagons.size() - 1)).setLastWagon(false);
        }

        cart.setLastWagon(true);

        this.isLocomotive = true;
        this.wagons.add(e);
        cart.setLocomotive(this);
        e.startRiding(this);
    }

    @Redirect(method="snapPositionToRail", at=@At(value= "INVOKE", target="Lnet/minecraft/util/math/MathHelper;floor(D)I", ordinal = 1))
    private int yCoordFix1(double yCoord) {
        return yCoordFix(yCoord);
    }

    @Redirect(method="snapPositionToRailWithOffset", at=@At(value= "INVOKE", target="Lnet/minecraft/util/math/MathHelper;floor(D)I", ordinal = 1))
    private int yCoordFix2(double yCoord) {
        return yCoordFix(yCoord);
    }

    @Redirect(method="tick", at=@At(value= "INVOKE", target="Lnet/minecraft/util/math/MathHelper;floor(D)I", ordinal = 1))
    private int yCoordFix3(double yCoord) {
        return yCoordFix(yCoord);
    }


    private int yCoordFix(double yCoord){
        return (int) Math.round(yCoord);
    }

    @Override
    public boolean isWagon() {
        return isWagon;
    }

    @Override
    public void setLastWagon(boolean isLastWagon) {
        this.isLastWagon = isLastWagon;
    }

    @Override
    public void setWagon(boolean wagon) {
        isWagon = wagon;
    }

    @Override
    public boolean isLocomotive() {
        return isLocomotive;
    }

    @Override
    public void setLocomotive(ITrainCart e){
        this.locomotive = e;
        this.isWagon = true;
    }

    @Override
    public ITrainCart getLocomotive() {
        return this.locomotive;
    }

    @Override
    public ArrayList<MinecartEntity> getWagons() {
        return this.wagons;
    }
}
