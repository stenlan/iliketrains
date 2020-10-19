package nl.lankreijer.stenlan.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import nl.lankreijer.stenlan.interfaces.ITrainCart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(MinecartEntity.class)
public abstract class MinecartEntityMixin {
    MinecartEntity cThis = ((MinecartEntity)(Object)this);

    @Inject(method="interact", at=@At(value="HEAD"), cancellable=true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> ci) {
        if(player.getStackInHand(hand).getItem() == Items.WOODEN_HOE) {
            System.out.println("Making train...");
            AtomicReference<MinecartEntity> closestEntity = new AtomicReference<>();
            AtomicReference<Double> closestDistance = new AtomicReference<>(Double.MAX_VALUE);
            cThis.world.getOtherEntities(cThis, cThis.getBoundingBox().expand(4.0D), (e) -> {
                double newDist;
                if ((e instanceof MinecartEntity) && (newDist = cThis.squaredDistanceTo(e)) < closestDistance.get() && !((ITrainCart)e).isWagon()) {
                    closestEntity.set((MinecartEntity) e);
                    closestDistance.set(newDist);
                }
                return false;
            });
            MinecartEntity closestEntityC = closestEntity.get();
            if(closestEntityC != null) {
                ((ITrainCart)cThis).addWagon(closestEntityC);
            }
//            ((ITrainCart)this).track();
            ci.setReturnValue(ActionResult.CONSUME);
            ci.cancel();
        }
    }
}
