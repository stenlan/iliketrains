package nl.lankreijer.stenlan.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.network.MessageType;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import nl.lankreijer.stenlan.interfaces.ITrainCart;
import nl.lankreijer.stenlan.trains.RailHelper;
import nl.lankreijer.stenlan.trains.RailState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin implements ITrainCart {
    private ArrayList<MinecartEntity> wagons = new ArrayList<>();
    private AbstractMinecartEntity cThis = ((AbstractMinecartEntity)(Object)this);
    ArrayDeque<RailState> prevRails = new ArrayDeque<>();
    private boolean isWagon = false;
    private boolean isLocomotive = false;

    @Shadow
    private static Pair<Vec3i, Vec3i> getAdjacentRailPositionsByShape(RailShape shape){return null;}

	/*@ModifyConstant(method="tick()V", constant=@Constant(doubleValue = -0.04D))
	private double modifyGrav(double oldGrav) {
		return oldGrav;
	}*/

    @Shadow public abstract Direction getMovementDirection();



    @Inject(method="tick()V", at=@At(value="INVOKE", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;setRotation(FF)V", ordinal=0), cancellable = true)
    public void tickInject(CallbackInfo ci) {
        if (this.isWagon) {
            // ci.cancel();
        } else if (this.isLocomotive) {
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
            while (!prevRails.peekFirst().pos.equals(railPos)) {
                dir = prevRails.peekFirst().exitDir();
                if (dir == null) {
                    System.out.println("Dir is null!");
                    return;
                }
                System.out.println("Adding new rail state at " + railPos.toString());
                prevRails.addFirst(new RailState(railPos, dir, blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty())));
                prevRails.removeLast();
            }

            double progress = prevRails.peekFirst().calcProgress(cThis.getPos());

            Iterator<RailState> it = prevRails.iterator();
            it.next();
            for (int i = 0; i < this.wagons.size(); i++) {
                placeBehind(wagons.get(i), it.next(), progress);
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

    /*@Inject(method="pushAwayFrom", at=@At("HEAD"), cancellable = true, locals= LocalCapture.CAPTURE_FAILHARD)
    private void pushAwayFromInject(Entity entity, CallbackInfo ci) {
        if (this.isWagon || (this.isLocomotive && !(entity instanceof PlayerEntity))) {
            ci.cancel();
        }
    }*/

    private void placeBehind(MinecartEntity me, RailState railState, double progress) {
        Vec3d pos = railState.calcPos(progress);

        me.setVelocity(pos.subtract(me.getPos())); // TODO: ?? clientX/clientXVelocity???

        // me.updatePosition(pos.x, pos.y, pos.z);
    }

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
        BlockPos railPos = getRailPos();
        BlockPos otherPos = ((ITrainCart)e).getRailPos();

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

        this.isLocomotive = true;
        this.wagons.add(e);
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
}
