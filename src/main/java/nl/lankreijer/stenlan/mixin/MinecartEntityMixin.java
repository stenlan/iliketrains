package nl.lankreijer.stenlan.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import nl.lankreijer.stenlan.interfaces.IMinecartMixin;
import nl.lankreijer.stenlan.wagons.CartLogic;
import nl.lankreijer.stenlan.wagons.MinecartType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(MinecartEntity.class)
public abstract class MinecartEntityMixin extends AbstractMinecartEntity {
    protected MinecartEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }


    @Inject(method="interact", at=@At(value="HEAD"), cancellable=true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> ci) {
        System.out.println("Interact");
        if(player.getStackInHand(hand).getItem() == Items.WOODEN_HOE) {
            ((IMinecartMixin)this).onLink();
            ci.setReturnValue(ActionResult.CONSUME);
        }
    }

    @Redirect(method="interact", at=@At(value="INVOKE", target="Lnet/minecraft/entity/vehicle/MinecartEntity;hasPassengers()Z"))
    public boolean hasPassengers(MinecartEntity me) {
        return false;
    }
}
