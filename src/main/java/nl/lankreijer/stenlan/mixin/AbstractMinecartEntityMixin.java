package nl.lankreijer.stenlan.mixin;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import nl.lankreijer.stenlan.interfaces.ITrainCart;
import nl.lankreijer.stenlan.trains.RailState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements ITrainCart {
    private static double cartDistance = 1.2d; // at least 1
    private ArrayList<MinecartEntity> wagons = new ArrayList<>();
    private Entity locomotive;
    private AbstractMinecartEntity cThis = ((AbstractMinecartEntity)(Object)this);
    ArrayDeque<RailState> prevRails = new ArrayDeque<>();
    private boolean isWagon = false;
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

            BlockState blockState = cThis.world.getBlockState(railPos);
            if (!AbstractRailBlock.isRail(blockState)) {
                System.out.println("Block " + railPos.toString() + " is not a rail :(. My pos: " + cThis.getPos().toString());
                return;
            }
            if(prevRails.peekFirst() == null) {
                System.out.println("First is null!");
                return;
            }

            Direction dir;
            if (!prevRails.peekFirst().pos.equals(railPos)) {
                dir = prevRails.peekFirst().exitDir();
                if (dir == null) {
                    System.out.println("Dir is null!");
                    return;
                }
                System.out.println("Adding new rail state at " + railPos.toString());
                prevRails.addFirst(new RailState(railPos, dir, blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty())));
            }

            while (prevRails.size() > 1.42 * cartDistance * wagons.size() + 2) {
                prevRails.removeLast();
            }

            Iterator<RailState> it = prevRails.iterator();
            RailState state = it.next(); // we always have the first cart

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
    }

    @Inject(method="tick()V", at=@At(value="INVOKE", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;setRotation(FF)V", ordinal=1), cancellable = true)
    public void tickInject2(CallbackInfo ci) {
        tickInject(ci);
    }

    @Inject(method="tick()V", at=@At(value="FIELD", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;firstUpdate:Z"), cancellable = true)
    public void tickInject3(CallbackInfo ci) {
        tickInject(ci);
    }


    @Inject(method="moveOffRail", at=@At("HEAD"))
    private void moveOffRailInject(CallbackInfo ci) {
        System.out.println("moving off rail!!");
    }

    private void placeBehind(MinecartEntity me, RailState railState, double progress) {
        Vec3d pos = railState.calcPos(progress);

        Vec3d target = pos.subtract(me.getPos());

        me.setVelocity(target); // TODO: ?? clientX/clientXVelocity???
        ((ITrainCart)me).semiTick();
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
        Vec3d pos = cThis.getPos();
        int p = MathHelper.floor(pos.x);
        int q = MathHelper.floor(pos.y);
        int r = MathHelper.floor(pos.z);

        if (cThis.world.getBlockState(new BlockPos(p, q - 1, r)).isIn(BlockTags.RAILS)) {
            --q;
        }

        return new BlockPos(p, q, r);
    }

    private static Direction toDir(BlockPos from, BlockPos to) {
        Vec3i diff = to.subtract(from);
        return Direction.fromVector(MathHelper.clamp(diff.getX(), -1, 1), 0, MathHelper.clamp(diff.getZ(), -1, 1));
    }

    @Override
    public void track() {
        this.isLocomotive = true;
        BlockPos railPos = getRailPos();
        Direction dir = this.getMovementDirection();
        BlockState blockState = cThis.world.getBlockState(railPos);
        this.prevRails.addLast(new RailState(railPos, dir, blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty())));
    }

    @Override
    public void addWagon(MinecartEntity e) { // TODO: add new previous pos and direction
        ITrainCart cart = (ITrainCart)e;
        BlockPos railPos = getRailPos();
        BlockPos otherPos = cart.getRailPos();

        Direction dir = toDir(otherPos, railPos);

        BlockPos blockPos = railPos.subtract(dir.getVector());
        BlockState blockState = cThis.world.getBlockState(blockPos);
        System.out.println(dir.toString());
        if(!AbstractRailBlock.isRail(blockState)) {
            System.out.println("Not a rail adjacent!");
            return;
        }
        assert prevRails.peekFirst() != null;

        e.noClip = true;
        e.setNoGravity(true);

        if(!this.isLocomotive) {
            this.prevRails.addLast(new RailState(blockPos, dir, blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty())));
        }

        if(this.getEntityId() > e.getEntityId()) {
            // IClientWorldAccessorMixin world = (IClientWorldAccessorMixin);
            System.out.println("yup");
        }


        this.isLocomotive = true;
        this.wagons.add(e);
        cart.setLocomotive(this);
        this.prevRails.addLast(new RailState(blockPos, dir, blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty())));
        ((ITrainCart)e).setWagon(true);
    }

    @Override
    public boolean isWagon() {
        return isWagon;
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
    public void setLocomotive(Entity e){
        this.locomotive = e;
    }

    @Override
    public Entity getLocomotive() {
        return this.locomotive;
    }

    @Override
    public ArrayList<MinecartEntity> getWagons() {
        return this.wagons;
    }
}
