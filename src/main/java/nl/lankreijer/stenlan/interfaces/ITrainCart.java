package nl.lankreijer.stenlan.interfaces;


import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public interface ITrainCart {
    void addWagon(MinecartEntity e);
    boolean isWagon();
    boolean isLocomotive();
    public ArrayList<MinecartEntity> getWagons();
    void setWagon(boolean isWagon);
    BlockPos getRailPos();
    void track();
    ITrainCart getLocomotive();
    void setLocomotive(ITrainCart loc);
    void semiTick();
    void updateWagons();
    void setLastWagon(boolean isLastWagon);
}
