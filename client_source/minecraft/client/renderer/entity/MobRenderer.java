package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

@Environment(EnvType.CLIENT)
public abstract class MobRenderer<T extends Mob, M extends EntityModel<T>> extends LivingEntityRenderer<T, M> {
   public MobRenderer(EntityRendererProvider.Context context, M entityModel, float f) {
      super(context, entityModel, f);
   }

   protected boolean shouldShowName(T mob) {
      return super.shouldShowName((LivingEntity)mob) && (mob.shouldShowName() || mob.hasCustomName() && mob == this.entityRenderDispatcher.crosshairPickEntity);
   }

   protected float getShadowRadius(T mob) {
      return super.getShadowRadius((LivingEntity)mob) * mob.getAgeScale();
   }

   // $FF: synthetic method
   protected float getShadowRadius(final LivingEntity livingEntity) {
      return this.getShadowRadius((Mob)livingEntity);
   }

   // $FF: synthetic method
   protected boolean shouldShowName(final LivingEntity livingEntity) {
      return this.shouldShowName((Mob)livingEntity);
   }

   // $FF: synthetic method
   protected float getShadowRadius(final Entity entity) {
      return this.getShadowRadius((Mob)entity);
   }

   // $FF: synthetic method
   protected boolean shouldShowName(final Entity entity) {
      return this.shouldShowName((Mob)entity);
   }
}
