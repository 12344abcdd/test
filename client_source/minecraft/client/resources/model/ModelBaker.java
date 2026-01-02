package net.minecraft.client.resources.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ModelBaker {
   UnbakedModel getModel(ResourceLocation resourceLocation);

   @Nullable
   BakedModel bake(ResourceLocation resourceLocation, ModelState modelState);
}
