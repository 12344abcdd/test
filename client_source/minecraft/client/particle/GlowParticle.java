package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class GlowParticle extends TextureSheetParticle {
   static final RandomSource RANDOM = RandomSource.create();
   private final SpriteSet sprites;

   GlowParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, SpriteSet spriteSet) {
      super(clientLevel, d, e, f, g, h, i);
      this.friction = 0.96F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = spriteSet;
      this.quadSize *= 0.75F;
      this.hasPhysics = false;
      this.setSpriteFromAge(spriteSet);
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public int getLightColor(float f) {
      float g = ((float)this.age + f) / (float)this.lifetime;
      g = Mth.clamp(g, 0.0F, 1.0F);
      int i = super.getLightColor(f);
      int j = i & 255;
      int k = i >> 16 & 255;
      j += (int)(g * 15.0F * 16.0F);
      if (j > 240) {
         j = 240;
      }

      return j | k << 16;
   }

   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   @Environment(EnvType.CLIENT)
   public static class ScrapeProvider implements ParticleProvider<SimpleParticleType> {
      private final double SPEED_FACTOR = 0.01D;
      private final SpriteSet sprite;

      public ScrapeProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D, this.sprite);
         if (clientLevel.random.nextBoolean()) {
            glowParticle.setColor(0.29F, 0.58F, 0.51F);
         } else {
            glowParticle.setColor(0.43F, 0.77F, 0.62F);
         }

         glowParticle.setParticleSpeed(g * 0.01D, h * 0.01D, i * 0.01D);
         int j = true;
         int k = true;
         glowParticle.setLifetime(clientLevel.random.nextInt(30) + 10);
         return glowParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ElectricSparkProvider implements ParticleProvider<SimpleParticleType> {
      private final double SPEED_FACTOR = 0.25D;
      private final SpriteSet sprite;

      public ElectricSparkProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D, this.sprite);
         glowParticle.setColor(1.0F, 0.9F, 1.0F);
         glowParticle.setParticleSpeed(g * 0.25D, h * 0.25D, i * 0.25D);
         int j = true;
         int k = true;
         glowParticle.setLifetime(clientLevel.random.nextInt(2) + 2);
         return glowParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class WaxOffProvider implements ParticleProvider<SimpleParticleType> {
      private final double SPEED_FACTOR = 0.01D;
      private final SpriteSet sprite;

      public WaxOffProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D, this.sprite);
         glowParticle.setColor(1.0F, 0.9F, 1.0F);
         glowParticle.setParticleSpeed(g * 0.01D / 2.0D, h * 0.01D, i * 0.01D / 2.0D);
         int j = true;
         int k = true;
         glowParticle.setLifetime(clientLevel.random.nextInt(30) + 10);
         return glowParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class WaxOnProvider implements ParticleProvider<SimpleParticleType> {
      private final double SPEED_FACTOR = 0.01D;
      private final SpriteSet sprite;

      public WaxOnProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D, this.sprite);
         glowParticle.setColor(0.91F, 0.55F, 0.08F);
         glowParticle.setParticleSpeed(g * 0.01D / 2.0D, h * 0.01D, i * 0.01D / 2.0D);
         int j = true;
         int k = true;
         glowParticle.setLifetime(clientLevel.random.nextInt(30) + 10);
         return glowParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class GlowSquidProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public GlowSquidProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         GlowParticle glowParticle = new GlowParticle(clientLevel, d, e, f, 0.5D - GlowParticle.RANDOM.nextDouble(), h, 0.5D - GlowParticle.RANDOM.nextDouble(), this.sprite);
         if (clientLevel.random.nextBoolean()) {
            glowParticle.setColor(0.6F, 1.0F, 0.8F);
         } else {
            glowParticle.setColor(0.08F, 0.4F, 0.4F);
         }

         glowParticle.yd *= 0.20000000298023224D;
         if (g == 0.0D && i == 0.0D) {
            glowParticle.xd *= 0.10000000149011612D;
            glowParticle.zd *= 0.10000000149011612D;
         }

         glowParticle.setLifetime((int)(8.0D / (clientLevel.random.nextDouble() * 0.8D + 0.2D)));
         return glowParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
