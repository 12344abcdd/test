package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class WaterCurrentDownParticle extends TextureSheetParticle {
   private float angle;

   WaterCurrentDownParticle(ClientLevel clientLevel, double d, double e, double f) {
      super(clientLevel, d, e, f);
      this.lifetime = (int)(Math.random() * 60.0D) + 30;
      this.hasPhysics = false;
      this.xd = 0.0D;
      this.yd = -0.05D;
      this.zd = 0.0D;
      this.setSize(0.02F, 0.02F);
      this.quadSize *= this.random.nextFloat() * 0.6F + 0.2F;
      this.gravity = 0.002F;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float f = 0.6F;
         this.xd += (double)(0.6F * Mth.cos(this.angle));
         this.zd += (double)(0.6F * Mth.sin(this.angle));
         this.xd *= 0.07D;
         this.zd *= 0.07D;
         this.move(this.xd, this.yd, this.zd);
         if (!this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER) || this.onGround) {
            this.remove();
         }

         this.angle += 0.08F;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         WaterCurrentDownParticle waterCurrentDownParticle = new WaterCurrentDownParticle(clientLevel, d, e, f);
         waterCurrentDownParticle.pickSprite(this.sprite);
         return waterCurrentDownParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
