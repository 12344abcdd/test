package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Parrot.Variant;

@Environment(EnvType.CLIENT)
public class ParrotRenderer extends MobRenderer<Parrot, ParrotModel> {
   private static final ResourceLocation RED_BLUE = ResourceLocation.withDefaultNamespace("textures/entity/parrot/parrot_red_blue.png");
   private static final ResourceLocation BLUE = ResourceLocation.withDefaultNamespace("textures/entity/parrot/parrot_blue.png");
   private static final ResourceLocation GREEN = ResourceLocation.withDefaultNamespace("textures/entity/parrot/parrot_green.png");
   private static final ResourceLocation YELLOW_BLUE = ResourceLocation.withDefaultNamespace("textures/entity/parrot/parrot_yellow_blue.png");
   private static final ResourceLocation GREY = ResourceLocation.withDefaultNamespace("textures/entity/parrot/parrot_grey.png");

   public ParrotRenderer(EntityRendererProvider.Context context) {
      super(context, new ParrotModel(context.bakeLayer(ModelLayers.PARROT)), 0.3F);
   }

   public ResourceLocation getTextureLocation(Parrot parrot) {
      return getVariantTexture(parrot.getVariant());
   }

   public static ResourceLocation getVariantTexture(Variant variant) {
      ResourceLocation var10000;
      switch(variant) {
      case RED_BLUE:
         var10000 = RED_BLUE;
         break;
      case BLUE:
         var10000 = BLUE;
         break;
      case GREEN:
         var10000 = GREEN;
         break;
      case YELLOW_BLUE:
         var10000 = YELLOW_BLUE;
         break;
      case GRAY:
         var10000 = GREY;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public float getBob(Parrot parrot, float f) {
      float g = Mth.lerp(f, parrot.oFlap, parrot.flap);
      float h = Mth.lerp(f, parrot.oFlapSpeed, parrot.flapSpeed);
      return (Mth.sin(g) + 1.0F) * h;
   }
}
