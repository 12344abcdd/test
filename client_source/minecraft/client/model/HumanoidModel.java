package net.minecraft.client.model;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public class HumanoidModel<T extends LivingEntity> extends AgeableListModel<T> implements ArmedModel, HeadedModel {
   public static final float OVERLAY_SCALE = 0.25F;
   public static final float HAT_OVERLAY_SCALE = 0.5F;
   public static final float LEGGINGS_OVERLAY_SCALE = -0.1F;
   private static final float DUCK_WALK_ROTATION = 0.005F;
   private static final float SPYGLASS_ARM_ROT_Y = 0.2617994F;
   private static final float SPYGLASS_ARM_ROT_X = 1.9198622F;
   private static final float SPYGLASS_ARM_CROUCH_ROT_X = 0.2617994F;
   private static final float HIGHEST_SHIELD_BLOCKING_ANGLE = -1.3962634F;
   private static final float LOWEST_SHIELD_BLOCKING_ANGLE = 0.43633232F;
   private static final float HORIZONTAL_SHIELD_MOVEMENT_LIMIT = 0.5235988F;
   public static final float TOOT_HORN_XROT_BASE = 1.4835298F;
   public static final float TOOT_HORN_YROT_BASE = 0.5235988F;
   public final ModelPart head;
   public final ModelPart hat;
   public final ModelPart body;
   public final ModelPart rightArm;
   public final ModelPart leftArm;
   public final ModelPart rightLeg;
   public final ModelPart leftLeg;
   public HumanoidModel.ArmPose leftArmPose;
   public HumanoidModel.ArmPose rightArmPose;
   public boolean crouching;
   public float swimAmount;

   public HumanoidModel(ModelPart modelPart) {
      this(modelPart, RenderType::entityCutoutNoCull);
   }

   public HumanoidModel(ModelPart modelPart, Function<ResourceLocation, RenderType> function) {
      super(function, true, 16.0F, 0.0F, 2.0F, 2.0F, 24.0F);
      this.leftArmPose = HumanoidModel.ArmPose.EMPTY;
      this.rightArmPose = HumanoidModel.ArmPose.EMPTY;
      this.head = modelPart.getChild("head");
      this.hat = modelPart.getChild("hat");
      this.body = modelPart.getChild("body");
      this.rightArm = modelPart.getChild("right_arm");
      this.leftArm = modelPart.getChild("left_arm");
      this.rightLeg = modelPart.getChild("right_leg");
      this.leftLeg = modelPart.getChild("left_leg");
   }

   public static MeshDefinition createMesh(CubeDeformation cubeDeformation, float f) {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      partDefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, cubeDeformation), PartPose.offset(0.0F, 0.0F + f, 0.0F));
      partDefinition.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, cubeDeformation.extend(0.5F)), PartPose.offset(0.0F, 0.0F + f, 0.0F));
      partDefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(0.0F, 0.0F + f, 0.0F));
      partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(-5.0F, 2.0F + f, 0.0F));
      partDefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(5.0F, 2.0F + f, 0.0F));
      partDefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(-1.9F, 12.0F + f, 0.0F));
      partDefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(1.9F, 12.0F + f, 0.0F));
      return meshDefinition;
   }

   protected Iterable<ModelPart> headParts() {
      return ImmutableList.of(this.head);
   }

   protected Iterable<ModelPart> bodyParts() {
      return ImmutableList.of(this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg, this.hat);
   }

   public void prepareMobModel(T livingEntity, float f, float g, float h) {
      this.swimAmount = livingEntity.getSwimAmount(h);
      super.prepareMobModel(livingEntity, f, g, h);
   }

   public void setupAnim(T livingEntity, float f, float g, float h, float i, float j) {
      boolean bl = livingEntity.getFallFlyingTicks() > 4;
      boolean bl2 = livingEntity.isVisuallySwimming();
      this.head.yRot = i * 0.017453292F;
      if (bl) {
         this.head.xRot = -0.7853982F;
      } else if (this.swimAmount > 0.0F) {
         if (bl2) {
            this.head.xRot = this.rotlerpRad(this.swimAmount, this.head.xRot, -0.7853982F);
         } else {
            this.head.xRot = this.rotlerpRad(this.swimAmount, this.head.xRot, j * 0.017453292F);
         }
      } else {
         this.head.xRot = j * 0.017453292F;
      }

      this.body.yRot = 0.0F;
      this.rightArm.z = 0.0F;
      this.rightArm.x = -5.0F;
      this.leftArm.z = 0.0F;
      this.leftArm.x = 5.0F;
      float k = 1.0F;
      if (bl) {
         k = (float)livingEntity.getDeltaMovement().lengthSqr();
         k /= 0.2F;
         k *= k * k;
      }

      if (k < 1.0F) {
         k = 1.0F;
      }

      this.rightArm.xRot = Mth.cos(f * 0.6662F + 3.1415927F) * 2.0F * g * 0.5F / k;
      this.leftArm.xRot = Mth.cos(f * 0.6662F) * 2.0F * g * 0.5F / k;
      this.rightArm.zRot = 0.0F;
      this.leftArm.zRot = 0.0F;
      this.rightLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g / k;
      this.leftLeg.xRot = Mth.cos(f * 0.6662F + 3.1415927F) * 1.4F * g / k;
      this.rightLeg.yRot = 0.005F;
      this.leftLeg.yRot = -0.005F;
      this.rightLeg.zRot = 0.005F;
      this.leftLeg.zRot = -0.005F;
      ModelPart var10000;
      if (this.riding) {
         var10000 = this.rightArm;
         var10000.xRot += -0.62831855F;
         var10000 = this.leftArm;
         var10000.xRot += -0.62831855F;
         this.rightLeg.xRot = -1.4137167F;
         this.rightLeg.yRot = 0.31415927F;
         this.rightLeg.zRot = 0.07853982F;
         this.leftLeg.xRot = -1.4137167F;
         this.leftLeg.yRot = -0.31415927F;
         this.leftLeg.zRot = -0.07853982F;
      }

      this.rightArm.yRot = 0.0F;
      this.leftArm.yRot = 0.0F;
      boolean bl3 = livingEntity.getMainArm() == HumanoidArm.RIGHT;
      boolean bl4;
      if (livingEntity.isUsingItem()) {
         bl4 = livingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND;
         if (bl4 == bl3) {
            this.poseRightArm(livingEntity);
         } else {
            this.poseLeftArm(livingEntity);
         }
      } else {
         bl4 = bl3 ? this.leftArmPose.isTwoHanded() : this.rightArmPose.isTwoHanded();
         if (bl3 != bl4) {
            this.poseLeftArm(livingEntity);
            this.poseRightArm(livingEntity);
         } else {
            this.poseRightArm(livingEntity);
            this.poseLeftArm(livingEntity);
         }
      }

      this.setupAttackAnimation(livingEntity, h);
      if (this.crouching) {
         this.body.xRot = 0.5F;
         var10000 = this.rightArm;
         var10000.xRot += 0.4F;
         var10000 = this.leftArm;
         var10000.xRot += 0.4F;
         this.rightLeg.z = 4.0F;
         this.leftLeg.z = 4.0F;
         this.rightLeg.y = 12.2F;
         this.leftLeg.y = 12.2F;
         this.head.y = 4.2F;
         this.body.y = 3.2F;
         this.leftArm.y = 5.2F;
         this.rightArm.y = 5.2F;
      } else {
         this.body.xRot = 0.0F;
         this.rightLeg.z = 0.0F;
         this.leftLeg.z = 0.0F;
         this.rightLeg.y = 12.0F;
         this.leftLeg.y = 12.0F;
         this.head.y = 0.0F;
         this.body.y = 0.0F;
         this.leftArm.y = 2.0F;
         this.rightArm.y = 2.0F;
      }

      if (this.rightArmPose != HumanoidModel.ArmPose.SPYGLASS) {
         AnimationUtils.bobModelPart(this.rightArm, h, 1.0F);
      }

      if (this.leftArmPose != HumanoidModel.ArmPose.SPYGLASS) {
         AnimationUtils.bobModelPart(this.leftArm, h, -1.0F);
      }

      if (this.swimAmount > 0.0F) {
         float l = f % 26.0F;
         HumanoidArm humanoidArm = this.getAttackArm(livingEntity);
         float m = humanoidArm == HumanoidArm.RIGHT && this.attackTime > 0.0F ? 0.0F : this.swimAmount;
         float n = humanoidArm == HumanoidArm.LEFT && this.attackTime > 0.0F ? 0.0F : this.swimAmount;
         float o;
         if (!livingEntity.isUsingItem()) {
            if (l < 14.0F) {
               this.leftArm.xRot = this.rotlerpRad(n, this.leftArm.xRot, 0.0F);
               this.rightArm.xRot = Mth.lerp(m, this.rightArm.xRot, 0.0F);
               this.leftArm.yRot = this.rotlerpRad(n, this.leftArm.yRot, 3.1415927F);
               this.rightArm.yRot = Mth.lerp(m, this.rightArm.yRot, 3.1415927F);
               this.leftArm.zRot = this.rotlerpRad(n, this.leftArm.zRot, 3.1415927F + 1.8707964F * this.quadraticArmUpdate(l) / this.quadraticArmUpdate(14.0F));
               this.rightArm.zRot = Mth.lerp(m, this.rightArm.zRot, 3.1415927F - 1.8707964F * this.quadraticArmUpdate(l) / this.quadraticArmUpdate(14.0F));
            } else if (l >= 14.0F && l < 22.0F) {
               o = (l - 14.0F) / 8.0F;
               this.leftArm.xRot = this.rotlerpRad(n, this.leftArm.xRot, 1.5707964F * o);
               this.rightArm.xRot = Mth.lerp(m, this.rightArm.xRot, 1.5707964F * o);
               this.leftArm.yRot = this.rotlerpRad(n, this.leftArm.yRot, 3.1415927F);
               this.rightArm.yRot = Mth.lerp(m, this.rightArm.yRot, 3.1415927F);
               this.leftArm.zRot = this.rotlerpRad(n, this.leftArm.zRot, 5.012389F - 1.8707964F * o);
               this.rightArm.zRot = Mth.lerp(m, this.rightArm.zRot, 1.2707963F + 1.8707964F * o);
            } else if (l >= 22.0F && l < 26.0F) {
               o = (l - 22.0F) / 4.0F;
               this.leftArm.xRot = this.rotlerpRad(n, this.leftArm.xRot, 1.5707964F - 1.5707964F * o);
               this.rightArm.xRot = Mth.lerp(m, this.rightArm.xRot, 1.5707964F - 1.5707964F * o);
               this.leftArm.yRot = this.rotlerpRad(n, this.leftArm.yRot, 3.1415927F);
               this.rightArm.yRot = Mth.lerp(m, this.rightArm.yRot, 3.1415927F);
               this.leftArm.zRot = this.rotlerpRad(n, this.leftArm.zRot, 3.1415927F);
               this.rightArm.zRot = Mth.lerp(m, this.rightArm.zRot, 3.1415927F);
            }
         }

         o = 0.3F;
         float p = 0.33333334F;
         this.leftLeg.xRot = Mth.lerp(this.swimAmount, this.leftLeg.xRot, 0.3F * Mth.cos(f * 0.33333334F + 3.1415927F));
         this.rightLeg.xRot = Mth.lerp(this.swimAmount, this.rightLeg.xRot, 0.3F * Mth.cos(f * 0.33333334F));
      }

      this.hat.copyFrom(this.head);
   }

   private void poseRightArm(T livingEntity) {
      switch(this.rightArmPose.ordinal()) {
      case 0:
         this.rightArm.yRot = 0.0F;
         break;
      case 1:
         this.rightArm.xRot = this.rightArm.xRot * 0.5F - 0.31415927F;
         this.rightArm.yRot = 0.0F;
         break;
      case 2:
         this.poseBlockingArm(this.rightArm, true);
         break;
      case 3:
         this.rightArm.yRot = -0.1F + this.head.yRot;
         this.leftArm.yRot = 0.1F + this.head.yRot + 0.4F;
         this.rightArm.xRot = -1.5707964F + this.head.xRot;
         this.leftArm.xRot = -1.5707964F + this.head.xRot;
         break;
      case 4:
         this.rightArm.xRot = this.rightArm.xRot * 0.5F - 3.1415927F;
         this.rightArm.yRot = 0.0F;
         break;
      case 5:
         AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, livingEntity, true);
         break;
      case 6:
         AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, true);
         break;
      case 7:
         this.rightArm.xRot = Mth.clamp(this.head.xRot - 1.9198622F - (livingEntity.isCrouching() ? 0.2617994F : 0.0F), -2.4F, 3.3F);
         this.rightArm.yRot = this.head.yRot - 0.2617994F;
         break;
      case 8:
         this.rightArm.xRot = Mth.clamp(this.head.xRot, -1.2F, 1.2F) - 1.4835298F;
         this.rightArm.yRot = this.head.yRot - 0.5235988F;
         break;
      case 9:
         this.rightArm.xRot = this.rightArm.xRot * 0.5F - 0.62831855F;
         this.rightArm.yRot = 0.0F;
      }

   }

   private void poseLeftArm(T livingEntity) {
      switch(this.leftArmPose.ordinal()) {
      case 0:
         this.leftArm.yRot = 0.0F;
         break;
      case 1:
         this.leftArm.xRot = this.leftArm.xRot * 0.5F - 0.31415927F;
         this.leftArm.yRot = 0.0F;
         break;
      case 2:
         this.poseBlockingArm(this.leftArm, false);
         break;
      case 3:
         this.rightArm.yRot = -0.1F + this.head.yRot - 0.4F;
         this.leftArm.yRot = 0.1F + this.head.yRot;
         this.rightArm.xRot = -1.5707964F + this.head.xRot;
         this.leftArm.xRot = -1.5707964F + this.head.xRot;
         break;
      case 4:
         this.leftArm.xRot = this.leftArm.xRot * 0.5F - 3.1415927F;
         this.leftArm.yRot = 0.0F;
         break;
      case 5:
         AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, livingEntity, false);
         break;
      case 6:
         AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, false);
         break;
      case 7:
         this.leftArm.xRot = Mth.clamp(this.head.xRot - 1.9198622F - (livingEntity.isCrouching() ? 0.2617994F : 0.0F), -2.4F, 3.3F);
         this.leftArm.yRot = this.head.yRot + 0.2617994F;
         break;
      case 8:
         this.leftArm.xRot = Mth.clamp(this.head.xRot, -1.2F, 1.2F) - 1.4835298F;
         this.leftArm.yRot = this.head.yRot + 0.5235988F;
         break;
      case 9:
         this.leftArm.xRot = this.leftArm.xRot * 0.5F - 0.62831855F;
         this.leftArm.yRot = 0.0F;
      }

   }

   private void poseBlockingArm(ModelPart modelPart, boolean bl) {
      modelPart.xRot = modelPart.xRot * 0.5F - 0.9424779F + Mth.clamp(this.head.xRot, -1.3962634F, 0.43633232F);
      modelPart.yRot = (bl ? -30.0F : 30.0F) * 0.017453292F + Mth.clamp(this.head.yRot, -0.5235988F, 0.5235988F);
   }

   protected void setupAttackAnimation(T livingEntity, float f) {
      if (!(this.attackTime <= 0.0F)) {
         HumanoidArm humanoidArm = this.getAttackArm(livingEntity);
         ModelPart modelPart = this.getArm(humanoidArm);
         float g = this.attackTime;
         this.body.yRot = Mth.sin(Mth.sqrt(g) * 6.2831855F) * 0.2F;
         ModelPart var10000;
         if (humanoidArm == HumanoidArm.LEFT) {
            var10000 = this.body;
            var10000.yRot *= -1.0F;
         }

         this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F;
         this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F;
         this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F;
         this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F;
         var10000 = this.rightArm;
         var10000.yRot += this.body.yRot;
         var10000 = this.leftArm;
         var10000.yRot += this.body.yRot;
         var10000 = this.leftArm;
         var10000.xRot += this.body.yRot;
         g = 1.0F - this.attackTime;
         g *= g;
         g *= g;
         g = 1.0F - g;
         float h = Mth.sin(g * 3.1415927F);
         float i = Mth.sin(this.attackTime * 3.1415927F) * -(this.head.xRot - 0.7F) * 0.75F;
         modelPart.xRot -= h * 1.2F + i;
         modelPart.yRot += this.body.yRot * 2.0F;
         modelPart.zRot += Mth.sin(this.attackTime * 3.1415927F) * -0.4F;
      }
   }

   protected float rotlerpRad(float f, float g, float h) {
      float i = (h - g) % 6.2831855F;
      if (i < -3.1415927F) {
         i += 6.2831855F;
      }

      if (i >= 3.1415927F) {
         i -= 6.2831855F;
      }

      return g + f * i;
   }

   private float quadraticArmUpdate(float f) {
      return -65.0F * f + f * f;
   }

   public void copyPropertiesTo(HumanoidModel<T> humanoidModel) {
      super.copyPropertiesTo(humanoidModel);
      humanoidModel.leftArmPose = this.leftArmPose;
      humanoidModel.rightArmPose = this.rightArmPose;
      humanoidModel.crouching = this.crouching;
      humanoidModel.head.copyFrom(this.head);
      humanoidModel.hat.copyFrom(this.hat);
      humanoidModel.body.copyFrom(this.body);
      humanoidModel.rightArm.copyFrom(this.rightArm);
      humanoidModel.leftArm.copyFrom(this.leftArm);
      humanoidModel.rightLeg.copyFrom(this.rightLeg);
      humanoidModel.leftLeg.copyFrom(this.leftLeg);
   }

   public void setAllVisible(boolean bl) {
      this.head.visible = bl;
      this.hat.visible = bl;
      this.body.visible = bl;
      this.rightArm.visible = bl;
      this.leftArm.visible = bl;
      this.rightLeg.visible = bl;
      this.leftLeg.visible = bl;
   }

   public void translateToHand(HumanoidArm humanoidArm, PoseStack poseStack) {
      this.getArm(humanoidArm).translateAndRotate(poseStack);
   }

   protected ModelPart getArm(HumanoidArm humanoidArm) {
      return humanoidArm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
   }

   public ModelPart getHead() {
      return this.head;
   }

   private HumanoidArm getAttackArm(T livingEntity) {
      HumanoidArm humanoidArm = livingEntity.getMainArm();
      return livingEntity.swingingArm == InteractionHand.MAIN_HAND ? humanoidArm : humanoidArm.getOpposite();
   }

   @Environment(EnvType.CLIENT)
   public static enum ArmPose {
      EMPTY(false),
      ITEM(false),
      BLOCK(false),
      BOW_AND_ARROW(true),
      THROW_SPEAR(false),
      CROSSBOW_CHARGE(true),
      CROSSBOW_HOLD(true),
      SPYGLASS(false),
      TOOT_HORN(false),
      BRUSH(false);

      private final boolean twoHanded;

      private ArmPose(final boolean bl) {
         this.twoHanded = bl;
      }

      public boolean isTwoHanded() {
         return this.twoHanded;
      }

      // $FF: synthetic method
      private static HumanoidModel.ArmPose[] $values() {
         return new HumanoidModel.ArmPose[]{EMPTY, ITEM, BLOCK, BOW_AND_ARROW, THROW_SPEAR, CROSSBOW_CHARGE, CROSSBOW_HOLD, SPYGLASS, TOOT_HORN, BRUSH};
      }
   }
}
