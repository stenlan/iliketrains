package nl.lankreijer.stenlan.interfaces;

import nl.lankreijer.stenlan.wagons.CartLogic;
import nl.lankreijer.stenlan.wagons.MinecartType;

public interface IMinecartMixin {
    void onLink();
    void changeLogic(CartLogic c);
    CartLogic getLogic();
    MinecartType getLogicType();
}
