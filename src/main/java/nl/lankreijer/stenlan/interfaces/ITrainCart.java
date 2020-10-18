package nl.lankreijer.stenlan.interfaces;

import net.minecraft.entity.vehicle.MinecartEntity;

public interface ITrainCart {
    void addWagon(MinecartEntity e);
    boolean isWagon();
    void setWagon(boolean isWagon);
}
