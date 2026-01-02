package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AnimalArmorItem.BodyType;
import net.minecraft.world.item.component.DyedItemColor;

@Environment(EnvType.CLIENT)
public class HorseArmorLayer extends RenderLayer<Horse, HorseModel<Horse>> {
   private final HorseModel<Horse> model;

   public HorseArmorLayer(RenderLayerParent<Horse, HorseModel<Horse>> renderLayerParent, EntityModelSet entityModelSet) {
      super(renderLayerParent);
      this.model = new HorseModel(entityModelSet.bakeLayer(ModelLayers.HORSE_ARMOR));
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Horse horse, float f, float g, float h, float j, float k, float l) {
      ItemStack itemStack = horse.getBodyArmorItem();
      Item var13 = itemStack.getItem();
      if (var13 instanceof AnimalArmorItem) {
         AnimalArmorItem animalArmorItem = (AnimalArmorItem)var13;
         if (animalArmorItem.getBodyType() == BodyType.EQUESTRIAN) {
            ((HorseModel)this.getParentModel()).copyPropertiesTo(this.model);
            this.model.prepareMobModel((AbstractHorse)horse, f, g, h);
            this.model.setupAnim((AbstractHorse)horse, f, g, j, k, l);
            int m;
            if (itemStack.is(ItemTags.DYEABLE)) {
               m = ARGB32.opaque(DyedItemColor.getOrDefault(itemStack, -6265536));
            } else {
               m = -1;
            }

            VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(animalArmorItem.getTexture()));
            this.model.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, m);
            return;
         }
      }

   }
}
