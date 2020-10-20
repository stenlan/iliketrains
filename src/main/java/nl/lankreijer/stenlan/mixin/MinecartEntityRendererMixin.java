package nl.lankreijer.stenlan.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.MinecartEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import nl.lankreijer.stenlan.interfaces.ITrainCart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartEntityRenderer.class)
public abstract class MinecartEntityRendererMixin<T extends AbstractMinecartEntity> extends EntityRenderer<T> {
    // Camera c = MinecraftClient.getInstance().gameRenderer.getCamera();
    protected MinecartEntityRendererMixin(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Inject(method="render", at=@At("HEAD"), cancellable = true)
    public void renderHeadMixin(T abstractMinecartEntity, float f, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        ITrainCart cart = (ITrainCart)abstractMinecartEntity;
        if(cart.isWagon()) {
            ci.cancel();
        } else if (cart.isLocomotive()) {
            Vec3d vec3d = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
            for(MinecartEntity me : cart.getWagons()) {
                float yaw = MathHelper.lerp(tickDelta, me.prevYaw, me.yaw);
                MinecraftClient.getInstance().getEntityRenderDispatcher().render(me, vec3d.getX(), vec3d.getY(), vec3d.getZ(), yaw, tickDelta, matrixStack, vertexConsumerProvider, this.dispatcher.getLight(me, tickDelta));
            }
        }
    }
}
