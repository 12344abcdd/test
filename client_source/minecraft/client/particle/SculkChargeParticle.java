package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SculkChargeParticleOptions;

@Environment(EnvType.CLIENT)
public class SculkChargeParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   SculkChargeParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, SpriteSet spriteSet) {
      super(clientLevel, d, e, f, g, h, i);
      this.friction = 0.96F;
      this.sprites = spriteSet;
      this.scale(1.5F);
      this.hasPhysics = false;
      this.setSpriteFromAge(spriteSet);
   }

   public int getLightColor(float f) {
      return 240;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
   }

   @Environment(EnvType.CLIENT)
   public static record Provider(SpriteSet sprite) implements ParticleProvider<SculkChargeParticleOptions> {
      public Provider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SculkChargeParticleOptions sculkChargeParticleOptions, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         SculkChargeParticle sculkChargeParticle = new SculkChargeParticle(clientLevel, d, e, f, g, h, i, this.sprite);
         sculkChargeParticle.setAlpha(1.0F);
         sculkChargeParticle.setParticleSpeed(g, h, i);
         sculkChargeParticle.oRoll = sculkChargeParticleOptions.roll();
         sculkChargeParticle.roll = sculkChargeParticleOptions.roll();
         sculkChargeParticle.setLifetime(clientLevel.random.nextInt(12) + 8);
         return sculkChargeParticle;
      }

      public SpriteSet sprite() {
         return this.sprite;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SculkChargeParticleOptions)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
