package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.Crackiness.Level;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AnimalArmorItem.BodyType;
import net.minecraft.world.item.component.DyedItemColor;

@Environment(EnvType.CLIENT)
public class WolfArmorLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
   private final WolfModel<Wolf> model;
   private static final Map<Level, ResourceLocation> ARMOR_CRACK_LOCATIONS;

   public WolfArmorLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> renderLayerParent, EntityModelSet entityModelSet) {
      super(renderLayerParent);
      this.model = new WolfModel(entityModelSet.bakeLayer(ModelLayers.WOLF_ARMOR));
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Wolf wolf, float f, float g, float h, float j, float k, float l) {
      if (wolf.hasArmor()) {
         ItemStack itemStack = wolf.getBodyArmorItem();
         Item var13 = itemStack.getItem();
         if (var13 instanceof AnimalArmorItem) {
            AnimalArmorItem animalArmorItem = (AnimalArmorItem)var13;
            if (animalArmorItem.getBodyType() == BodyType.CANINE) {
               ((WolfModel)this.getParentModel()).copyPropertiesTo(this.model);
               this.model.prepareMobModel(wolf, f, g, h);
               this.model.setupAnim(wolf, f, g, j, k, l);
               VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(animalArmorItem.getTexture()));
               this.model.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY);
               this.maybeRenderColoredLayer(poseStack, multiBufferSource, i, itemStack, animalArmorItem);
               this.maybeRenderCracks(poseStack, multiBufferSource, i, itemStack);
               return;
            }
         }

      }
   }

   private void maybeRenderColoredLayer(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, ItemStack itemStack, AnimalArmorItem animalArmorItem) {
      if (itemStack.is(ItemTags.DYEABLE)) {
         int j = DyedItemColor.getOrDefault(itemStack, 0);
         if (ARGB32.alpha(j) == 0) {
            return;
         }

         ResourceLocation resourceLocation = animalArmorItem.getOverlayTexture();
         if (resourceLocation == null) {
            return;
         }

         this.model.renderToBuffer(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(resourceLocation)), i, OverlayTexture.NO_OVERLAY, ARGB32.opaque(j));
      }

   }

   private void maybeRenderCracks(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, ItemStack itemStack) {
      Level level = Crackiness.WOLF_ARMOR.byDamage(itemStack);
      if (level != Level.NONE) {
         ResourceLocation resourceLocation = (ResourceLocation)ARMOR_CRACK_LOCATIONS.get(level);
         VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityTranslucent(resourceLocation));
         this.model.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY);
      }
   }

   static {
      ARMOR_CRACK_LOCATIONS = Map.of(Level.LOW, ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"), Level.MEDIUM, ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"), Level.HIGH, ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png"));
   }
}
