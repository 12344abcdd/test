package net.minecraft.client.model;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class SquidModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart[] tentacles = new ModelPart[8];
   private final ModelPart root;

   public SquidModel(ModelPart modelPart) {
      this.root = modelPart;
      Arrays.setAll(this.tentacles, (i) -> {
         return modelPart.getChild(createTentacleName(i));
      });
   }

   private static String createTentacleName(int i) {
      return "tentacle" + i;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      CubeDeformation cubeDeformation = new CubeDeformation(0.02F);
      int i = true;
      partDefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 16.0F, 12.0F, cubeDeformation), PartPose.offset(0.0F, 8.0F, 0.0F));
      int j = true;
      CubeListBuilder cubeListBuilder = CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F);

      for(int k = 0; k < 8; ++k) {
         double d = (double)k * 3.141592653589793D * 2.0D / 8.0D;
         float f = (float)Math.cos(d) * 5.0F;
         float g = 15.0F;
         float h = (float)Math.sin(d) * 5.0F;
         d = (double)k * 3.141592653589793D * -2.0D / 8.0D + 1.5707963267948966D;
         float l = (float)d;
         partDefinition.addOrReplaceChild(createTentacleName(k), cubeListBuilder, PartPose.offsetAndRotation(f, 15.0F, h, 0.0F, l, 0.0F));
      }

      return LayerDefinition.create(meshDefinition, 64, 32);
   }

   public void setupAnim(T entity, float f, float g, float h, float i, float j) {
      ModelPart[] var7 = this.tentacles;
      int var8 = var7.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         ModelPart modelPart = var7[var9];
         modelPart.xRot = h;
      }

   }

   public ModelPart root() {
      return this.root;
   }
}
