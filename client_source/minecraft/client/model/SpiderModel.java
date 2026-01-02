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
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class SpiderModel<T extends Entity> extends HierarchicalModel<T> {
   private static final String BODY_0 = "body0";
   private static final String BODY_1 = "body1";
   private static final String RIGHT_MIDDLE_FRONT_LEG = "right_middle_front_leg";
   private static final String LEFT_MIDDLE_FRONT_LEG = "left_middle_front_leg";
   private static final String RIGHT_MIDDLE_HIND_LEG = "right_middle_hind_leg";
   private static final String LEFT_MIDDLE_HIND_LEG = "left_middle_hind_leg";
   private final ModelPart root;
   private final ModelPart head;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightMiddleHindLeg;
   private final ModelPart leftMiddleHindLeg;
   private final ModelPart rightMiddleFrontLeg;
   private final ModelPart leftMiddleFrontLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;

   public SpiderModel(ModelPart modelPart) {
      this.root = modelPart;
      this.head = modelPart.getChild("head");
      this.rightHindLeg = modelPart.getChild("right_hind_leg");
      this.leftHindLeg = modelPart.getChild("left_hind_leg");
      this.rightMiddleHindLeg = modelPart.getChild("right_middle_hind_leg");
      this.leftMiddleHindLeg = modelPart.getChild("left_middle_hind_leg");
      this.rightMiddleFrontLeg = modelPart.getChild("right_middle_front_leg");
      this.leftMiddleFrontLeg = modelPart.getChild("left_middle_front_leg");
      this.rightFrontLeg = modelPart.getChild("right_front_leg");
      this.leftFrontLeg = modelPart.getChild("left_front_leg");
   }

   public static LayerDefinition createSpiderBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      int i = true;
      partDefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 4).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 15.0F, -3.0F));
      partDefinition.addOrReplaceChild("body0", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
      partDefinition.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(0, 12).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 12.0F), PartPose.offset(0.0F, 15.0F, 9.0F));
      CubeListBuilder cubeListBuilder = CubeListBuilder.create().texOffs(18, 0).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F);
      CubeListBuilder cubeListBuilder2 = CubeListBuilder.create().texOffs(18, 0).mirror().addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F);
      partDefinition.addOrReplaceChild("right_hind_leg", cubeListBuilder, PartPose.offset(-4.0F, 15.0F, 2.0F));
      partDefinition.addOrReplaceChild("left_hind_leg", cubeListBuilder2, PartPose.offset(4.0F, 15.0F, 2.0F));
      partDefinition.addOrReplaceChild("right_middle_hind_leg", cubeListBuilder, PartPose.offset(-4.0F, 15.0F, 1.0F));
      partDefinition.addOrReplaceChild("left_middle_hind_leg", cubeListBuilder2, PartPose.offset(4.0F, 15.0F, 1.0F));
      partDefinition.addOrReplaceChild("right_middle_front_leg", cubeListBuilder, PartPose.offset(-4.0F, 15.0F, 0.0F));
      partDefinition.addOrReplaceChild("left_middle_front_leg", cubeListBuilder2, PartPose.offset(4.0F, 15.0F, 0.0F));
      partDefinition.addOrReplaceChild("right_front_leg", cubeListBuilder, PartPose.offset(-4.0F, 15.0F, -1.0F));
      partDefinition.addOrReplaceChild("left_front_leg", cubeListBuilder2, PartPose.offset(4.0F, 15.0F, -1.0F));
      return LayerDefinition.create(meshDefinition, 64, 32);
   }

   public ModelPart root() {
      return this.root;
   }

   public void setupAnim(T entity, float f, float g, float h, float i, float j) {
      this.head.yRot = i * 0.017453292F;
      this.head.xRot = j * 0.017453292F;
      float k = 0.7853982F;
      this.rightHindLeg.zRot = -0.7853982F;
      this.leftHindLeg.zRot = 0.7853982F;
      this.rightMiddleHindLeg.zRot = -0.58119464F;
      this.leftMiddleHindLeg.zRot = 0.58119464F;
      this.rightMiddleFrontLeg.zRot = -0.58119464F;
      this.leftMiddleFrontLeg.zRot = 0.58119464F;
      this.rightFrontLeg.zRot = -0.7853982F;
      this.leftFrontLeg.zRot = 0.7853982F;
      float l = -0.0F;
      float m = 0.3926991F;
      this.rightHindLeg.yRot = 0.7853982F;
      this.leftHindLeg.yRot = -0.7853982F;
      this.rightMiddleHindLeg.yRot = 0.3926991F;
      this.leftMiddleHindLeg.yRot = -0.3926991F;
      this.rightMiddleFrontLeg.yRot = -0.3926991F;
      this.leftMiddleFrontLeg.yRot = 0.3926991F;
      this.rightFrontLeg.yRot = -0.7853982F;
      this.leftFrontLeg.yRot = 0.7853982F;
      float n = -(Mth.cos(f * 0.6662F * 2.0F + 0.0F) * 0.4F) * g;
      float o = -(Mth.cos(f * 0.6662F * 2.0F + 3.1415927F) * 0.4F) * g;
      float p = -(Mth.cos(f * 0.6662F * 2.0F + 1.5707964F) * 0.4F) * g;
      float q = -(Mth.cos(f * 0.6662F * 2.0F + 4.712389F) * 0.4F) * g;
      float r = Math.abs(Mth.sin(f * 0.6662F + 0.0F) * 0.4F) * g;
      float s = Math.abs(Mth.sin(f * 0.6662F + 3.1415927F) * 0.4F) * g;
      float t = Math.abs(Mth.sin(f * 0.6662F + 1.5707964F) * 0.4F) * g;
      float u = Math.abs(Mth.sin(f * 0.6662F + 4.712389F) * 0.4F) * g;
      ModelPart var10000 = this.rightHindLeg;
      var10000.yRot += n;
      var10000 = this.leftHindLeg;
      var10000.yRot += -n;
      var10000 = this.rightMiddleHindLeg;
      var10000.yRot += o;
      var10000 = this.leftMiddleHindLeg;
      var10000.yRot += -o;
      var10000 = this.rightMiddleFrontLeg;
      var10000.yRot += p;
      var10000 = this.leftMiddleFrontLeg;
      var10000.yRot += -p;
      var10000 = this.rightFrontLeg;
      var10000.yRot += q;
      var10000 = this.leftFrontLeg;
      var10000.yRot += -q;
      var10000 = this.rightHindLeg;
      var10000.zRot += r;
      var10000 = this.leftHindLeg;
      var10000.zRot += -r;
      var10000 = this.rightMiddleHindLeg;
      var10000.zRot += s;
      var10000 = this.leftMiddleHindLeg;
      var10000.zRot += -s;
      var10000 = this.rightMiddleFrontLeg;
      var10000.zRot += t;
      var10000 = this.leftMiddleFrontLeg;
      var10000.zRot += -t;
      var10000 = this.rightFrontLeg;
      var10000.zRot += u;
      var10000 = this.leftFrontLeg;
      var10000.zRot += -u;
   }
}
