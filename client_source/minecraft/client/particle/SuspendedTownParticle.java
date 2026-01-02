package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class SuspendedTownParticle extends TextureSheetParticle {
   SuspendedTownParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
      super(clientLevel, d, e, f, g, h, i);
      float j = this.random.nextFloat() * 0.1F + 0.2F;
      this.rCol = j;
      this.gCol = j;
      this.bCol = j;
      this.setSize(0.02F, 0.02F);
      this.quadSize *= this.random.nextFloat() * 0.6F + 0.5F;
      this.xd *= 0.019999999552965164D;
      this.yd *= 0.019999999552965164D;
      this.zd *= 0.019999999552965164D;
      this.lifetime = (int)(20.0D / (Math.random() * 0.8D + 0.2D));
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public void move(double d, double e, double f) {
      this.setBoundingBox(this.getBoundingBox().move(d, e, f));
      this.setLocationFromBoundingbox();
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.lifetime-- <= 0) {
         this.remove();
      } else {
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 0.99D;
         this.yd *= 0.99D;
         this.zd *= 0.99D;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class EggCrackProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public EggCrackProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         SuspendedTownParticle suspendedTownParticle = new SuspendedTownParticle(clientLevel, d, e, f, g, h, i);
         suspendedTownParticle.pickSprite(this.sprite);
         suspendedTownParticle.setColor(1.0F, 1.0F, 1.0F);
         return suspendedTownParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class DolphinSpeedProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DolphinSpeedProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         SuspendedTownParticle suspendedTownParticle = new SuspendedTownParticle(clientLevel, d, e, f, g, h, i);
         suspendedTownParticle.setColor(0.3F, 0.5F, 1.0F);
         suspendedTownParticle.pickSprite(this.sprite);
         suspendedTownParticle.setAlpha(1.0F - clientLevel.random.nextFloat() * 0.7F);
         suspendedTownParticle.setLifetime(suspendedTownParticle.getLifetime() / 2);
         return suspendedTownParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ComposterFillProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ComposterFillProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         SuspendedTownParticle suspendedTownParticle = new SuspendedTownParticle(clientLevel, d, e, f, g, h, i);
         suspendedTownParticle.pickSprite(this.sprite);
         suspendedTownParticle.setColor(1.0F, 1.0F, 1.0F);
         suspendedTownParticle.setLifetime(3 + clientLevel.getRandom().nextInt(5));
         return suspendedTownParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class HappyVillagerProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public HappyVillagerProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         SuspendedTownParticle suspendedTownParticle = new SuspendedTownParticle(clientLevel, d, e, f, g, h, i);
         suspendedTownParticle.pickSprite(this.sprite);
         suspendedTownParticle.setColor(1.0F, 1.0F, 1.0F);
         return suspendedTownParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         SuspendedTownParticle suspendedTownParticle = new SuspendedTownParticle(clientLevel, d, e, f, g, h, i);
         suspendedTownParticle.pickSprite(this.sprite);
         return suspendedTownParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
