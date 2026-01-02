package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class NoteParticle extends TextureSheetParticle {
   NoteParticle(ClientLevel clientLevel, double d, double e, double f, double g) {
      super(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D);
      this.friction = 0.66F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.xd *= 0.009999999776482582D;
      this.yd *= 0.009999999776482582D;
      this.zd *= 0.009999999776482582D;
      this.yd += 0.2D;
      this.rCol = Math.max(0.0F, Mth.sin(((float)g + 0.0F) * 6.2831855F) * 0.65F + 0.35F);
      this.gCol = Math.max(0.0F, Mth.sin(((float)g + 0.33333334F) * 6.2831855F) * 0.65F + 0.35F);
      this.bCol = Math.max(0.0F, Mth.sin(((float)g + 0.6666667F) * 6.2831855F) * 0.65F + 0.35F);
      this.quadSize *= 1.5F;
      this.lifetime = 6;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public float getQuadSize(float f) {
      return this.quadSize * Mth.clamp(((float)this.age + f) / (float)this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         NoteParticle noteParticle = new NoteParticle(clientLevel, d, e, f, g);
         noteParticle.pickSprite(this.sprite);
         return noteParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
