package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.axolotl.Axolotl.Variant;

@Environment(EnvType.CLIENT)
public class AxolotlRenderer extends MobRenderer<Axolotl, AxolotlModel<Axolotl>> {
   private static final Map<Variant, ResourceLocation> TEXTURE_BY_TYPE = (Map)Util.make(Maps.newHashMap(), (hashMap) -> {
      Variant[] var1 = Variant.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         Variant variant = var1[var3];
         hashMap.put(variant, ResourceLocation.withDefaultNamespace(String.format(Locale.ROOT, "textures/entity/axolotl/axolotl_%s.png", variant.getName())));
      }

   });

   public AxolotlRenderer(EntityRendererProvider.Context context) {
      super(context, new AxolotlModel(context.bakeLayer(ModelLayers.AXOLOTL)), 0.5F);
   }

   public ResourceLocation getTextureLocation(Axolotl axolotl) {
      return (ResourceLocation)TEXTURE_BY_TYPE.get(axolotl.getVariant());
   }
}
