package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;

@Environment(EnvType.CLIENT)
public class PiglinHeadModel extends SkullModelBase {
   private final ModelPart head;
   private final ModelPart leftEar;
   private final ModelPart rightEar;

   public PiglinHeadModel(ModelPart modelPart) {
      this.head = modelPart.getChild("head");
      this.leftEar = this.head.getChild("left_ear");
      this.rightEar = this.head.getChild("right_ear");
   }

   public static MeshDefinition createHeadModel() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PiglinModel.addHead(CubeDeformation.NONE, meshDefinition);
      return meshDefinition;
   }

   public void setupAnim(float f, float g, float h) {
      this.head.yRot = g * 0.017453292F;
      this.head.xRot = h * 0.017453292F;
      float i = 1.2F;
      this.leftEar.zRot = (float)(-(Math.cos((double)(f * 3.1415927F * 0.2F * 1.2F)) + 2.5D)) * 0.2F;
      this.rightEar.zRot = (float)(Math.cos((double)(f * 3.1415927F * 0.2F)) + 2.5D) * 0.2F;
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k) {
      this.head.render(poseStack, vertexConsumer, i, j, k);
   }
}
