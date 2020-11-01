package nl.lankreijer.stenlan.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import nl.lankreijer.stenlan.interfaces.IMinecartMixin;
import nl.lankreijer.stenlan.wagons.MinecartType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    @Inject(method="raycast", at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;getRootVehicle()Lnet/minecraft/entity/Entity;", ordinal=1, shift=At.Shift.BY, by=3), cancellable=true, locals=LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void doItemUseInject(Entity entity, Vec3d vec3d, Vec3d vec3d2, Box box, Predicate<Entity> predicate, double d, CallbackInfoReturnable<@Nullable EntityHitResult> cir, World world, double e, Entity entity2, Vec3d vec3d3, Iterator var12, Entity entity3, Box box2, Optional optional, Vec3d vec3d4, double f) {
        if (entity3 instanceof AbstractMinecartEntity) {
            if (((IMinecartMixin) entity3).getLogicType() != MinecartType.NORMAL) { // TODO: Maybe add a check if the player is actually sitting in there or not
                cir.setReturnValue(new EntityHitResult(entity3, vec3d4));
            }
        }
    }
}
