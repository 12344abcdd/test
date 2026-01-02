package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.TurtleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Turtle;

@Environment(EnvType.CLIENT)
public class TurtleRenderer extends MobRenderer<Turtle, TurtleModel<Turtle>> {
   private static final ResourceLocation TURTLE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/turtle/big_sea_turtle.png");

   public TurtleRenderer(EntityRendererProvider.Context context) {
      super(context, new TurtleModel(context.bakeLayer(ModelLayers.TURTLE)), 0.7F);
   }

   protected float getShadowRadius(Turtle turtle) {
      float f = super.getShadowRadius((Mob)turtle);
      return turtle.isBaby() ? f * 0.83F : f;
   }

   public ResourceLocation getTextureLocation(Turtle turtle) {
      return TURTLE_LOCATION;
   }

   // $FF: synthetic method
   protected float getShadowRadius(final Mob mob) {
      return this.getShadowRadius((Turtle)mob);
   }

   // $FF: synthetic method
   protected float getShadowRadius(final LivingEntity livingEntity) {
      return this.getShadowRadius((Turtle)livingEntity);
   }

   // $FF: synthetic method
   public ResourceLocation getTextureLocation(final Entity entity) {
      return this.getTextureLocation((Turtle)entity);
   }

   // $FF: synthetic method
   protected float getShadowRadius(final Entity entity) {
      return this.getShadowRadius((Turtle)entity);
   }
}
