package nl.lankreijer.stenlan.interfaces;

import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.BlockPos;

public interface ITrainCart {
    void addWagon(MinecartEntity e);
    boolean isWagon();
    void setWagon(boolean isWagon);
    BlockPos getRailPos();
    void track();
}
