package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ColorableHierarchicalModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.TropicalFish;

@Environment(EnvType.CLIENT)
public class TropicalFishRenderer extends MobRenderer<TropicalFish, ColorableHierarchicalModel<TropicalFish>> {
   private final ColorableHierarchicalModel<TropicalFish> modelA = (ColorableHierarchicalModel)this.getModel();
   private final ColorableHierarchicalModel<TropicalFish> modelB;
   private static final ResourceLocation MODEL_A_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fish/tropical_a.png");
   private static final ResourceLocation MODEL_B_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fish/tropical_b.png");

   public TropicalFishRenderer(EntityRendererProvider.Context context) {
      super(context, new TropicalFishModelA(context.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15F);
      this.modelB = new TropicalFishModelB(context.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
      this.addLayer(new TropicalFishPatternLayer(this, context.getModelSet()));
   }

   public ResourceLocation getTextureLocation(TropicalFish tropicalFish) {
      ResourceLocation var10000;
      switch(tropicalFish.getVariant().base()) {
      case SMALL:
         var10000 = MODEL_A_TEXTURE;
         break;
      case LARGE:
         var10000 = MODEL_B_TEXTURE;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public void render(TropicalFish tropicalFish, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
      ColorableHierarchicalModel var10000;
      switch(tropicalFish.getVariant().base()) {
      case SMALL:
         var10000 = this.modelA;
         break;
      case LARGE:
         var10000 = this.modelB;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      ColorableHierarchicalModel<TropicalFish> colorableHierarchicalModel = var10000;
      this.model = colorableHierarchicalModel;
      colorableHierarchicalModel.setColor(tropicalFish.getBaseColor().getTextureDiffuseColor());
      super.render(tropicalFish, f, g, poseStack, multiBufferSource, i);
      colorableHierarchicalModel.setColor(-1);
   }

   protected void setupRotations(TropicalFish tropicalFish, PoseStack poseStack, float f, float g, float h, float i) {
      super.setupRotations(tropicalFish, poseStack, f, g, h, i);
      float j = 4.3F * Mth.sin(0.6F * f);
      poseStack.mulPose(Axis.YP.rotationDegrees(j));
      if (!tropicalFish.isInWater()) {
         poseStack.translate(0.2F, 0.1F, 0.0F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }

   }
}
