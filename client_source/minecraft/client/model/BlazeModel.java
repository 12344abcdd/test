package net.minecraft.client.model;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class BlazeModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart root;
   private final ModelPart[] upperBodyParts;
   private final ModelPart head;

   public BlazeModel(ModelPart modelPart) {
      this.root = modelPart;
      this.head = modelPart.getChild("head");
      this.upperBodyParts = new ModelPart[12];
      Arrays.setAll(this.upperBodyParts, (i) -> {
         return modelPart.getChild(getPartName(i));
      });
   }

   private static String getPartName(int i) {
      return "part" + i;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      partDefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      float f = 0.0F;
      CubeListBuilder cubeListBuilder = CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F);

      int i;
      float g;
      float h;
      float j;
      for(i = 0; i < 4; ++i) {
         g = Mth.cos(f) * 9.0F;
         h = -2.0F + Mth.cos((float)(i * 2) * 0.25F);
         j = Mth.sin(f) * 9.0F;
         partDefinition.addOrReplaceChild(getPartName(i), cubeListBuilder, PartPose.offset(g, h, j));
         ++f;
      }

      f = 0.7853982F;

      for(i = 4; i < 8; ++i) {
         g = Mth.cos(f) * 7.0F;
         h = 2.0F + Mth.cos((float)(i * 2) * 0.25F);
         j = Mth.sin(f) * 7.0F;
         partDefinition.addOrReplaceChild(getPartName(i), cubeListBuilder, PartPose.offset(g, h, j));
         ++f;
      }

      f = 0.47123894F;

      for(i = 8; i < 12; ++i) {
         g = Mth.cos(f) * 5.0F;
         h = 11.0F + Mth.cos((float)i * 1.5F * 0.5F);
         j = Mth.sin(f) * 5.0F;
         partDefinition.addOrReplaceChild(getPartName(i), cubeListBuilder, PartPose.offset(g, h, j));
         ++f;
      }

      return LayerDefinition.create(meshDefinition, 64, 32);
   }

   public ModelPart root() {
      return this.root;
   }

   public void setupAnim(T entity, float f, float g, float h, float i, float j) {
      float k = h * 3.1415927F * -0.1F;

      int l;
      for(l = 0; l < 4; ++l) {
         this.upperBodyParts[l].y = -2.0F + Mth.cos(((float)(l * 2) + h) * 0.25F);
         this.upperBodyParts[l].x = Mth.cos(k) * 9.0F;
         this.upperBodyParts[l].z = Mth.sin(k) * 9.0F;
         ++k;
      }

      k = 0.7853982F + h * 3.1415927F * 0.03F;

      for(l = 4; l < 8; ++l) {
         this.upperBodyParts[l].y = 2.0F + Mth.cos(((float)(l * 2) + h) * 0.25F);
         this.upperBodyParts[l].x = Mth.cos(k) * 7.0F;
         this.upperBodyParts[l].z = Mth.sin(k) * 7.0F;
         ++k;
      }

      k = 0.47123894F + h * 3.1415927F * -0.05F;

      for(l = 8; l < 12; ++l) {
         this.upperBodyParts[l].y = 11.0F + Mth.cos(((float)l * 1.5F + h) * 0.5F);
         this.upperBodyParts[l].x = Mth.cos(k) * 5.0F;
         this.upperBodyParts[l].z = Mth.sin(k) * 5.0F;
         ++k;
      }

      this.head.yRot = i * 0.017453292F;
      this.head.xRot = j * 0.017453292F;
   }
}
