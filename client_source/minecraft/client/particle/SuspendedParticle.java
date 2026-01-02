package net.minecraft.client.particle;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class SuspendedParticle extends TextureSheetParticle {
   SuspendedParticle(ClientLevel clientLevel, SpriteSet spriteSet, double d, double e, double f) {
      super(clientLevel, d, e - 0.125D, f);
      this.setSize(0.01F, 0.01F);
      this.pickSprite(spriteSet);
      this.quadSize *= this.random.nextFloat() * 0.6F + 0.2F;
      this.lifetime = (int)(16.0D / (Math.random() * 0.8D + 0.2D));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   SuspendedParticle(ClientLevel clientLevel, SpriteSet spriteSet, double d, double e, double f, double g, double h, double i) {
      super(clientLevel, d, e - 0.125D, f, g, h, i);
      this.setSize(0.01F, 0.01F);
      this.pickSprite(spriteSet);
      this.quadSize *= this.random.nextFloat() * 0.6F + 0.6F;
      this.lifetime = (int)(16.0D / (Math.random() * 0.8D + 0.2D));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Environment(EnvType.CLIENT)
   public static class WarpedSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WarpedSporeProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         double j = (double)clientLevel.random.nextFloat() * -1.9D * (double)clientLevel.random.nextFloat() * 0.1D;
         SuspendedParticle suspendedParticle = new SuspendedParticle(clientLevel, this.sprite, d, e, f, 0.0D, j, 0.0D);
         suspendedParticle.setColor(0.1F, 0.1F, 0.3F);
         suspendedParticle.setSize(0.001F, 0.001F);
         return suspendedParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class CrimsonSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public CrimsonSporeProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         RandomSource randomSource = clientLevel.random;
         double j = randomSource.nextGaussian() * 9.999999974752427E-7D;
         double k = randomSource.nextGaussian() * 9.999999747378752E-5D;
         double l = randomSource.nextGaussian() * 9.999999974752427E-7D;
         SuspendedParticle suspendedParticle = new SuspendedParticle(clientLevel, this.sprite, d, e, f, j, k, l);
         suspendedParticle.setColor(0.9F, 0.4F, 0.5F);
         return suspendedParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class SporeBlossomAirProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SporeBlossomAirProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         SuspendedParticle suspendedParticle = new SuspendedParticle(this, clientLevel, this.sprite, d, e, f, 0.0D, -0.800000011920929D, 0.0D) {
            {
               super(clientLevel, spriteSet, d, e, f, g, h, i);
            }

            public Optional<ParticleGroup> getParticleGroup() {
               return Optional.of(ParticleGroup.SPORE_BLOSSOM);
            }
         };
         suspendedParticle.lifetime = Mth.randomBetweenInclusive(clientLevel.random, 500, 1000);
         suspendedParticle.gravity = 0.01F;
         suspendedParticle.setColor(0.32F, 0.5F, 0.22F);
         return suspendedParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class UnderwaterProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public UnderwaterProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         SuspendedParticle suspendedParticle = new SuspendedParticle(clientLevel, this.sprite, d, e, f);
         suspendedParticle.setColor(0.4F, 0.4F, 0.7F);
         return suspendedParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
