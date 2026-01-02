package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ChestRaftModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.RaftModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Boat.Type;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class BoatRenderer extends EntityRenderer<Boat> {
   private final Map<Type, Pair<ResourceLocation, ListModel<Boat>>> boatResources;

   public BoatRenderer(EntityRendererProvider.Context context, boolean bl) {
      super(context);
      this.shadowRadius = 0.8F;
      this.boatResources = (Map)Stream.of(Type.values()).collect(ImmutableMap.toImmutableMap((type) -> {
         return type;
      }, (type) -> {
         return Pair.of(getTextureLocation(type, bl), this.createBoatModel(context, type, bl));
      }));
   }

   private ListModel<Boat> createBoatModel(EntityRendererProvider.Context context, Type type, boolean bl) {
      ModelLayerLocation modelLayerLocation = bl ? ModelLayers.createChestBoatModelName(type) : ModelLayers.createBoatModelName(type);
      ModelPart modelPart = context.bakeLayer(modelLayerLocation);
      if (type == Type.BAMBOO) {
         return (ListModel)(bl ? new ChestRaftModel(modelPart) : new RaftModel(modelPart));
      } else {
         return (ListModel)(bl ? new ChestBoatModel(modelPart) : new BoatModel(modelPart));
      }
   }

   private static ResourceLocation getTextureLocation(Type type, boolean bl) {
      return bl ? ResourceLocation.withDefaultNamespace("textures/entity/chest_boat/" + type.getName() + ".png") : ResourceLocation.withDefaultNamespace("textures/entity/boat/" + type.getName() + ".png");
   }

   public void render(Boat boat, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.375F, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - f));
      float h = (float)boat.getHurtTime() - g;
      float j = boat.getDamage() - g;
      if (j < 0.0F) {
         j = 0.0F;
      }

      if (h > 0.0F) {
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(h) * h * j / 10.0F * (float)boat.getHurtDir()));
      }

      float k = boat.getBubbleAngle(g);
      if (!Mth.equal(k, 0.0F)) {
         poseStack.mulPose((new Quaternionf()).setAngleAxis(boat.getBubbleAngle(g) * 0.017453292F, 1.0F, 0.0F, 1.0F));
      }

      Pair<ResourceLocation, ListModel<Boat>> pair = (Pair)this.boatResources.get(boat.getVariant());
      ResourceLocation resourceLocation = (ResourceLocation)pair.getFirst();
      ListModel<Boat> listModel = (ListModel)pair.getSecond();
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
      listModel.setupAnim(boat, g, 0.0F, -0.1F, 0.0F, 0.0F);
      VertexConsumer vertexConsumer = multiBufferSource.getBuffer(listModel.renderType(resourceLocation));
      listModel.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY);
      if (!boat.isUnderWater()) {
         VertexConsumer vertexConsumer2 = multiBufferSource.getBuffer(RenderType.waterMask());
         if (listModel instanceof WaterPatchModel) {
            WaterPatchModel waterPatchModel = (WaterPatchModel)listModel;
            waterPatchModel.waterPatch().render(poseStack, vertexConsumer2, i, OverlayTexture.NO_OVERLAY);
         }
      }

      poseStack.popPose();
      super.render(boat, f, g, poseStack, multiBufferSource, i);
   }

   public ResourceLocation getTextureLocation(Boat boat) {
      return (ResourceLocation)((Pair)this.boatResources.get(boat.getVariant())).getFirst();
   }
}
