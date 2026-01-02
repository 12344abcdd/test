package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Ravager;

@Environment(EnvType.CLIENT)
public class RavagerModel extends HierarchicalModel<Ravager> {
   private final ModelPart root;
   private final ModelPart head;
   private final ModelPart mouth;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart neck;

   public RavagerModel(ModelPart modelPart) {
      this.root = modelPart;
      this.neck = modelPart.getChild("neck");
      this.head = this.neck.getChild("head");
      this.mouth = this.head.getChild("mouth");
      this.rightHindLeg = modelPart.getChild("right_hind_leg");
      this.leftHindLeg = modelPart.getChild("left_hind_leg");
      this.rightFrontLeg = modelPart.getChild("right_front_leg");
      this.leftFrontLeg = modelPart.getChild("left_front_leg");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      int i = true;
      PartDefinition partDefinition2 = partDefinition.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(68, 73).addBox(-5.0F, -1.0F, -18.0F, 10.0F, 10.0F, 18.0F), PartPose.offset(0.0F, -7.0F, 5.5F));
      PartDefinition partDefinition3 = partDefinition2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -20.0F, -14.0F, 16.0F, 20.0F, 16.0F).texOffs(0, 0).addBox(-2.0F, -6.0F, -18.0F, 4.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 16.0F, -17.0F));
      partDefinition3.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(74, 55).addBox(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F), PartPose.offsetAndRotation(-10.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F));
      partDefinition3.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(74, 55).mirror().addBox(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F), PartPose.offsetAndRotation(8.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F));
      partDefinition3.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(0, 36).addBox(-8.0F, 0.0F, -16.0F, 16.0F, 3.0F, 16.0F), PartPose.offset(0.0F, -2.0F, 2.0F));
      partDefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 55).addBox(-7.0F, -10.0F, -7.0F, 14.0F, 16.0F, 20.0F).texOffs(0, 91).addBox(-6.0F, 6.0F, -7.0F, 12.0F, 13.0F, 18.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 1.5707964F, 0.0F, 0.0F));
      partDefinition.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(96, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(-8.0F, -13.0F, 18.0F));
      partDefinition.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(96, 0).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(8.0F, -13.0F, 18.0F));
      partDefinition.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(-8.0F, -13.0F, -5.0F));
      partDefinition.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(8.0F, -13.0F, -5.0F));
      return LayerDefinition.create(meshDefinition, 128, 128);
   }

   public ModelPart root() {
      return this.root;
   }

   public void setupAnim(Ravager ravager, float f, float g, float h, float i, float j) {
      this.head.xRot = j * 0.017453292F;
      this.head.yRot = i * 0.017453292F;
      float k = 0.4F * g;
      this.rightHindLeg.xRot = Mth.cos(f * 0.6662F) * k;
      this.leftHindLeg.xRot = Mth.cos(f * 0.6662F + 3.1415927F) * k;
      this.rightFrontLeg.xRot = Mth.cos(f * 0.6662F + 3.1415927F) * k;
      this.leftFrontLeg.xRot = Mth.cos(f * 0.6662F) * k;
   }

   public void prepareMobModel(Ravager ravager, float f, float g, float h) {
      super.prepareMobModel(ravager, f, g, h);
      int i = ravager.getStunnedTick();
      int j = ravager.getRoarTick();
      int k = true;
      int l = ravager.getAttackTick();
      int m = true;
      float n;
      float o;
      float q;
      if (l > 0) {
         n = Mth.triangleWave((float)l - h, 10.0F);
         o = (1.0F + n) * 0.5F;
         float p = o * o * o * 12.0F;
         q = p * Mth.sin(this.neck.xRot);
         this.neck.z = -6.5F + p;
         this.neck.y = -7.0F - q;
         float r = Mth.sin(((float)l - h) / 10.0F * 3.1415927F * 0.25F);
         this.mouth.xRot = 1.5707964F * r;
         if (l > 5) {
            this.mouth.xRot = Mth.sin(((float)(-4 + l) - h) / 4.0F) * 3.1415927F * 0.4F;
         } else {
            this.mouth.xRot = 0.15707964F * Mth.sin(3.1415927F * ((float)l - h) / 10.0F);
         }
      } else {
         n = -1.0F;
         o = -1.0F * Mth.sin(this.neck.xRot);
         this.neck.x = 0.0F;
         this.neck.y = -7.0F - o;
         this.neck.z = 5.5F;
         boolean bl = i > 0;
         this.neck.xRot = bl ? 0.21991149F : 0.0F;
         this.mouth.xRot = 3.1415927F * (bl ? 0.05F : 0.01F);
         if (bl) {
            double d = (double)i / 40.0D;
            this.neck.x = (float)Math.sin(d * 10.0D) * 3.0F;
         } else if (j > 0) {
            q = Mth.sin(((float)(20 - j) - h) / 20.0F * 3.1415927F * 0.25F);
            this.mouth.xRot = 1.5707964F * q;
         }
      }

   }
}
