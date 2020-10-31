package nl.lankreijer.stenlan.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import nl.lankreijer.stenlan.interfaces.IMinecartMixin;
import nl.lankreijer.stenlan.wagons.CartLogic;
import nl.lankreijer.stenlan.wagons.LocomotiveLogic;
import nl.lankreijer.stenlan.wagons.MinecartType;
import nl.lankreijer.stenlan.wagons.WagonLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements IMinecartMixin {
    private CartLogic cartLogic = new CartLogic((AbstractMinecartEntity)(Object)this);
    private MinecartType cartType = MinecartType.NORMAL;

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
        if (this.cartLogic.shouldCancelTick()) {
            ci.cancel();
        }
    }

    @Inject(method="tick()V", at=@At(value="INVOKE", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;setRotation(FF)V", ordinal=0)) // Order-dependent
    public void tickInject(CallbackInfo ci) {
        this.cartLogic.afterTick();
    }

    @Inject(method="tick()V", at=@At(value="INVOKE", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;setRotation(FF)V", ordinal=1), cancellable = true)
    public void tickInject2(CallbackInfo ci) {
        tickInject(ci);
    }

    @Inject(method="tick()V", at=@At(value="FIELD", target="Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;firstUpdate:Z"), cancellable = true)
    public void tickInject3(CallbackInfo ci) {
        tickInject(ci);
    }

    @Override
    public MinecartType getLogicType() {
        return this.cartType;
    }

    @Override
    public CartLogic getLogic() {
        return this.cartLogic;
    }


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

    @Override
    public void changeLogic(CartLogic l) {
        this.cartLogic = l;
        this.cartType = l.isLocomotive() ? MinecartType.LOCOMOTIVE : l.isWagon() ? MinecartType.WAGON : MinecartType.NORMAL;
    }

    @Override
    public void onLink() {
        switch (this.cartType) {
            case NORMAL:
                this.changeLogic(new LocomotiveLogic(this));
                if (!((LocomotiveLogic)this.cartLogic).addWagon()) {
                    this.changeLogic(new CartLogic(this));
                }
                break;
            case LOCOMOTIVE:
                ((LocomotiveLogic)this.cartLogic).addWagon();
                break;
            case WAGON:
                ((WagonLogic)this.cartLogic).getLocomotive().addWagon(); // blergh
                break;
        }
    }

    @Override
    public void tickRiding() {
        this.setVelocity(Vec3d.ZERO);
        this.tick();
        if (this.hasVehicle()) {
            this.cartLogic.tickRiding();
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.cartLogic.canAddPassenger();
    }


    @Override
    public void refreshPositionAfterTeleport(double x, double y, double z) { // TODO: catch this
        System.out.println("AAARGGHH");
        super.refreshPositionAfterTeleport(x, y, z);
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
}
