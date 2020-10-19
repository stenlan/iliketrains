package nl.lankreijer.stenlan.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.tag.BlockTags;
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

    @Inject(method="tick()V", at=@At("HEAD"), cancellable = true)
    public void tickInject(CallbackInfo ci) {
        if (this.isWagon) {
            // ci.cancel();
        } else if (this.isLocomotive) {
            Vec3d pos = cThis.getPos();
            BlockPos railPos = this.getRailPos();

            double progressX = pos.x - railPos.getX();
            double progressY = pos.y - railPos.getY();
            double progressZ = pos.z - railPos.getZ();

            Direction dir;

            BlockState blockState = cThis.world.getBlockState(railPos);
            if (!AbstractRailBlock.isRail(blockState)) {
                System.out.println("Block " + railPos.toString() + " is not a rail :(. My pos: " + cThis.getPos().toString());
                return;
            }
            if(prevRails.peekFirst() == null) {
                System.out.println("First is null!");
                return;
            }

            if (!prevRails.peekFirst().pos.equals(railPos)) {
                dir = toDir(railPos, prevRails.peekFirst().pos);
                if (dir == null) {
                    System.out.println("Dir is null!");
                    return;
                }
                prevRails.addFirst(new RailState(railPos, dir, blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty())));
                prevRails.removeLast();
            } else {
                dir = prevRails.peekFirst().dir;
            }

            double progress = 0;

            switch (dir) {
                case NORTH:
                    progress = 1d - progressZ;
                    break;
                case SOUTH:
                    progress = progressZ;
                    break;
                case WEST:
                    progress = 1d - progressX;
                    break;
                case EAST:
                    progress = progressX;
                    break;
                default:
                    progress = 0.5d;
            }

            Iterator<RailState> it = prevRails.iterator();
            it.next();
            for (int i = 0; i < this.wagons.size(); i++) {
                placeBehind(wagons.get(i), it.next(), progress);
            }
        }
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

        me.setVelocity(pos.subtract(me.getPos()));

//		BlockPos blockPos = new BlockPos(i, j, k);
//		BlockState blockState = cThis.world.getBlockState(blockPos);
//		if (AbstractRailBlock.isRail(blockState)) {
//			double d = x;
//			double e = y;
//			double f = z;
//			Vec3d vec3d = cThis.snapPositionToRail(d, e, f);
//			e = blockPos.getY();
//			AbstractRailBlock abstractRailBlock = (AbstractRailBlock)blockState.getBlock();
//
//			RailShape railShape = blockState.get(abstractRailBlock.getShapeProperty());
//
//			Pair<Vec3i, Vec3i> pair = getAdjacentRailPositionsByShape(railShape);
//			Vec3i target = pair.getFirst();
//			if(blockPos.getManhattanDistance(target) == 0) { // same position
//				target = pair.getSecond();
//			}
//			me.teleport(target.getX(), target.getY(), target.getZ());
//
//
//			if (blockState.isOf(Blocks.ACTIVATOR_RAIL)) {
//				// this.onActivatorRail(i, j, k, (Boolean)blockState.get(PoweredRailBlock.POWERED));
//			}
//		} else {
//			// this.moveOffRail();
//		}
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

        this.isLocomotive = true;
        this.wagons.add(e);
        this.prevRails.addLast(new RailState(blockPos, dir, blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty())));
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
