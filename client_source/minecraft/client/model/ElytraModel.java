package net.minecraft.client.model;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class ElytraModel<T extends LivingEntity> extends AgeableListModel<T> {
   private final ModelPart rightWing;
   private final ModelPart leftWing;

   public ElytraModel(ModelPart modelPart) {
      this.leftWing = modelPart.getChild("left_wing");
      this.rightWing = modelPart.getChild("right_wing");
   }

   public static LayerDefinition createLayer() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      CubeDeformation cubeDeformation = new CubeDeformation(1.0F);
      partDefinition.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(22, 0).addBox(-10.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, cubeDeformation), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, -0.2617994F));
      partDefinition.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, cubeDeformation), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, 0.2617994F));
      return LayerDefinition.create(meshDefinition, 64, 32);
   }

   protected Iterable<ModelPart> headParts() {
      return ImmutableList.of();
   }

   protected Iterable<ModelPart> bodyParts() {
      return ImmutableList.of(this.leftWing, this.rightWing);
   }

   public void setupAnim(T livingEntity, float f, float g, float h, float i, float j) {
      float k = 0.2617994F;
      float l = -0.2617994F;
      float m = 0.0F;
      float n = 0.0F;
      if (livingEntity.isFallFlying()) {
         float o = 1.0F;
         Vec3 vec3 = livingEntity.getDeltaMovement();
         if (vec3.y < 0.0D) {
            Vec3 vec32 = vec3.normalize();
            o = 1.0F - (float)Math.pow(-vec32.y, 1.5D);
         }

         k = o * 0.34906584F + (1.0F - o) * k;
         l = o * -1.5707964F + (1.0F - o) * l;
      } else if (livingEntity.isCrouching()) {
         k = 0.6981317F;
         l = -0.7853982F;
         m = 3.0F;
         n = 0.08726646F;
      }

      this.leftWing.y = m;
      if (livingEntity instanceof AbstractClientPlayer) {
         AbstractClientPlayer abstractClientPlayer = (AbstractClientPlayer)livingEntity;
         abstractClientPlayer.elytraRotX += (k - abstractClientPlayer.elytraRotX) * 0.1F;
         abstractClientPlayer.elytraRotY += (n - abstractClientPlayer.elytraRotY) * 0.1F;
         abstractClientPlayer.elytraRotZ += (l - abstractClientPlayer.elytraRotZ) * 0.1F;
         this.leftWing.xRot = abstractClientPlayer.elytraRotX;
         this.leftWing.yRot = abstractClientPlayer.elytraRotY;
         this.leftWing.zRot = abstractClientPlayer.elytraRotZ;
      } else {
         this.leftWing.xRot = k;
         this.leftWing.zRot = l;
         this.leftWing.yRot = n;
      }

      this.rightWing.yRot = -this.leftWing.yRot;
      this.rightWing.y = this.leftWing.y;
      this.rightWing.xRot = this.leftWing.xRot;
      this.rightWing.zRot = -this.leftWing.zRot;
   }
}
