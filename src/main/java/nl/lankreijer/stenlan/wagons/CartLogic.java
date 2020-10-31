package nl.lankreijer.stenlan.wagons;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import nl.lankreijer.stenlan.interfaces.IMinecartMixin;

public class CartLogic {
    protected AbstractMinecartEntity minecart;
    protected IMinecartMixin minecartMixin;

    public CartLogic(AbstractMinecartEntity minecart) {
        this.minecart = minecart;
        this.minecartMixin = (IMinecartMixin) minecart;
    }

    public CartLogic (IMinecartMixin minecart) {
        this.minecart = (AbstractMinecartEntity) minecart;
        this.minecartMixin = minecart;
    }

    public boolean isLocomotive() {
        return false;
    }

    public boolean isWagon() {
        return false;
    }

    public boolean shouldCancelTick() {
        return false;
    }

    public void afterTick() {
    }

    public void tickRiding() {
        this.minecart.getVehicle().updatePassengerPosition(this.minecart);
    }

    public boolean canAddPassenger() {
        return this.minecart.getPassengerList().size() < 1;
    }

    public BlockPos getRailPos() {
        Vec3d pos = this.minecart.getPos();
        int p = MathHelper.floor(pos.x);
        int q = (int) Math.round(pos.y);
        int r = MathHelper.floor(pos.z);

        if (this.minecart.world.getBlockState(new BlockPos(p, q - 1, r)).isIn(BlockTags.RAILS)) {
            --q;
        }
        return new BlockPos(p, q, r);
    }

    public AbstractMinecartEntity getMinecart() {
        return this.minecart;
    }


}
