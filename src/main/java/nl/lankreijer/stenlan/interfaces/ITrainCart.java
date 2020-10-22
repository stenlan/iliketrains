package nl.lankreijer.stenlan.interfaces;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
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
    Entity getLocomotive();
    void setLocomotive(Entity loc);
    void semiTick();
}
