package nl.lankreijer.stenlan.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientWorld.class)
public interface IClientWorldAccessorMixin {
    @Accessor("regularEntities")
    Int2ObjectMap<Entity> getRegularEntities();
}
