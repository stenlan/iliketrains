package nl.lankreijer.stenlan.wagons;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;

public class WagonLogic extends CartLogic {
    private boolean lastWagon = false;
    private LocomotiveLogic locomotive;

    public WagonLogic(AbstractMinecartEntity minecart) {
        super(minecart);
    }

    public WagonLogic(AbstractMinecartEntity minecart, LocomotiveLogic locomotive) {
        this(minecart);
        this.setLocomotive(locomotive);
    }

    @Override
    public boolean isWagon() {
        return true;
    }
    @Override
    public boolean shouldCancelTick() {
        return true;
    }
    @Override
    public void tickRiding() {
        if (this.lastWagon && this.locomotive != null) {
            this.locomotive.updateWagons();
        }
    }

    public void setLocomotive(LocomotiveLogic l){
        this.locomotive = l;
    }

    public AbstractMinecartEntity getLocomotiveEntity() {
        return this.locomotive.minecart;
    }

    public LocomotiveLogic getLocomotive() {
        return this.locomotive;
    }

    public void setLastWagon(boolean lastWagon) {
        this.lastWagon = lastWagon;
    }
}
