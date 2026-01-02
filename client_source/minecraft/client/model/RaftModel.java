package net.minecraft.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;

@Environment(EnvType.CLIENT)
public class RaftModel extends ListModel<Boat> {
   private static final String LEFT_PADDLE = "left_paddle";
   private static final String RIGHT_PADDLE = "right_paddle";
   private static final String BOTTOM = "bottom";
   private final ModelPart leftPaddle;
   private final ModelPart rightPaddle;
   private final ImmutableList<ModelPart> parts;

   public RaftModel(ModelPart modelPart) {
      this.leftPaddle = modelPart.getChild("left_paddle");
      this.rightPaddle = modelPart.getChild("right_paddle");
      this.parts = this.createPartsBuilder(modelPart).build();
   }

   protected Builder<ModelPart> createPartsBuilder(ModelPart modelPart) {
      Builder<ModelPart> builder = new Builder();
      builder.add(new ModelPart[]{modelPart.getChild("bottom"), this.leftPaddle, this.rightPaddle});
      return builder;
   }

   public static void createChildren(PartDefinition partDefinition) {
      partDefinition.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -11.0F, -4.0F, 28.0F, 20.0F, 4.0F).texOffs(0, 0).addBox(-14.0F, -9.0F, -8.0F, 28.0F, 16.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.1F, 1.0F, 1.5708F, 0.0F, 0.0F));
      int i = true;
      int j = true;
      int k = true;
      float f = -5.0F;
      partDefinition.addOrReplaceChild("left_paddle", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F).addBox(-1.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F), PartPose.offsetAndRotation(3.0F, -4.0F, 9.0F, 0.0F, 0.0F, 0.19634955F));
      partDefinition.addOrReplaceChild("right_paddle", CubeListBuilder.create().texOffs(40, 24).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F).addBox(0.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F), PartPose.offsetAndRotation(3.0F, -4.0F, -9.0F, 0.0F, 3.1415927F, 0.19634955F));
   }

   public static LayerDefinition createBodyModel() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      createChildren(partDefinition);
      return LayerDefinition.create(meshDefinition, 128, 64);
   }

   public void setupAnim(Boat boat, float f, float g, float h, float i, float j) {
      animatePaddle(boat, 0, this.leftPaddle, f);
      animatePaddle(boat, 1, this.rightPaddle, f);
   }

   public ImmutableList<ModelPart> parts() {
      return this.parts;
   }

   private static void animatePaddle(Boat boat, int i, ModelPart modelPart, float f) {
      float g = boat.getRowingTime(i, f);
      modelPart.xRot = Mth.clampedLerp(-1.0471976F, -0.2617994F, (Mth.sin(-g) + 1.0F) / 2.0F);
      modelPart.yRot = Mth.clampedLerp(-0.7853982F, 0.7853982F, (Mth.sin(-g + 1.0F) + 1.0F) / 2.0F);
      if (i == 1) {
         modelPart.yRot = 3.1415927F - modelPart.yRot;
      }

   }

   // $FF: synthetic method
   public Iterable parts() {
      return this.parts();
   }
}
