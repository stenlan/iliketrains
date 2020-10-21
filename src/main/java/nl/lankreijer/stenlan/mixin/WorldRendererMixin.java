package nl.lankreijer.stenlan.mixin;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import nl.lankreijer.stenlan.interfaces.ITrainCart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Iterator;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    private long lastLimitTime = 0;
    private HashMap<Integer, Integer> cartCount = new HashMap<>(); // contains locomotive id -> num wagons encountered

    @Redirect(method="render", at=@At(value="INVOKE", target="Ljava/util/Iterator;next()Ljava/lang/Object;", ordinal=0))
    private <E> E nextRedirect(Iterator<E> i, MatrixStack matrices, float tickDelta, long limitTime) {
        if (limitTime != lastLimitTime) {
            cartCount.clear();
            lastLimitTime = limitTime;
        }
        E e = i.next();
        if (e instanceof AbstractMinecartEntity) {
            // System.out.println("Minecart!");
            Entity entity = (Entity) e;
            ITrainCart tc = (ITrainCart) entity;
            boolean isWagon =  tc.isWagon();
            boolean isLocomotive = tc.isLocomotive();
            if (isWagon || isLocomotive) {
                Entity loc;
                int locId;
                ITrainCart locCart;
                if (tc.isWagon()) {
                    loc = tc.getLocomotive();
                } else {
                    loc = entity;
                }
                locId = loc.getEntityId();

                cartCount.put(locId, cartCount.getOrDefault(loc.getEntityId(), 0) + 1); // increase amount of carts we rendered

                locCart = (ITrainCart)loc;
                int count = cartCount.get(locId);
                int cartCount = locCart.getWagons().size() + 1;
                if (count == cartCount) { // time to render locomotive TODO: implement quick key find
                    return (E) loc;
                } else {
                    return (E) locCart.getWagons().get(cartCount - 1 - count);
                }
            }
        }
        return e;
    }
}
