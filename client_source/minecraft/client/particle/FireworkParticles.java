package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.FireworkExplosion.Shape;

@Environment(EnvType.CLIENT)
public class FireworkParticles {
   @Environment(EnvType.CLIENT)
   public static class SparkProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SparkProvider(SpriteSet spriteSet) {
         this.sprites = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         FireworkParticles.SparkParticle sparkParticle = new FireworkParticles.SparkParticle(clientLevel, d, e, f, g, h, i, Minecraft.getInstance().particleEngine, this.sprites);
         sparkParticle.setAlpha(0.99F);
         return sparkParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class FlashProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public FlashProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         FireworkParticles.OverlayParticle overlayParticle = new FireworkParticles.OverlayParticle(clientLevel, d, e, f);
         overlayParticle.pickSprite(this.sprite);
         return overlayParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class OverlayParticle extends TextureSheetParticle {
      OverlayParticle(ClientLevel clientLevel, double d, double e, double f) {
         super(clientLevel, d, e, f);
         this.lifetime = 4;
      }

      public ParticleRenderType getRenderType() {
         return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
      }

      public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
         this.setAlpha(0.6F - ((float)this.age + f - 1.0F) * 0.25F * 0.5F);
         super.render(vertexConsumer, camera, f);
      }

      public float getQuadSize(float f) {
         return 7.1F * Mth.sin(((float)this.age + f - 1.0F) * 0.25F * 3.1415927F);
      }
   }

   @Environment(EnvType.CLIENT)
   private static class SparkParticle extends SimpleAnimatedParticle {
      private boolean trail;
      private boolean twinkle;
      private final ParticleEngine engine;
      private float fadeR;
      private float fadeG;
      private float fadeB;
      private boolean hasFade;

      SparkParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, ParticleEngine particleEngine, SpriteSet spriteSet) {
         super(clientLevel, d, e, f, spriteSet, 0.1F);
         this.xd = g;
         this.yd = h;
         this.zd = i;
         this.engine = particleEngine;
         this.quadSize *= 0.75F;
         this.lifetime = 48 + this.random.nextInt(12);
         this.setSpriteFromAge(spriteSet);
      }

      public void setTrail(boolean bl) {
         this.trail = bl;
      }

      public void setTwinkle(boolean bl) {
         this.twinkle = bl;
      }

      public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
         if (!this.twinkle || this.age < this.lifetime / 3 || (this.age + this.lifetime) / 3 % 2 == 0) {
            super.render(vertexConsumer, camera, f);
         }

      }

      public void tick() {
         super.tick();
         if (this.trail && this.age < this.lifetime / 2 && (this.age + this.lifetime) % 2 == 0) {
            FireworkParticles.SparkParticle sparkParticle = new FireworkParticles.SparkParticle(this.level, this.x, this.y, this.z, 0.0D, 0.0D, 0.0D, this.engine, this.sprites);
            sparkParticle.setAlpha(0.99F);
            sparkParticle.setColor(this.rCol, this.gCol, this.bCol);
            sparkParticle.age = sparkParticle.lifetime / 2;
            if (this.hasFade) {
               sparkParticle.hasFade = true;
               sparkParticle.fadeR = this.fadeR;
               sparkParticle.fadeG = this.fadeG;
               sparkParticle.fadeB = this.fadeB;
            }

            sparkParticle.twinkle = this.twinkle;
            this.engine.add(sparkParticle);
         }

      }
   }

   @Environment(EnvType.CLIENT)
   public static class Starter extends NoRenderParticle {
      private static final double[][] CREEPER_PARTICLE_COORDS = new double[][]{{0.0D, 0.2D}, {0.2D, 0.2D}, {0.2D, 0.6D}, {0.6D, 0.6D}, {0.6D, 0.2D}, {0.2D, 0.2D}, {0.2D, 0.0D}, {0.4D, 0.0D}, {0.4D, -0.6D}, {0.2D, -0.6D}, {0.2D, -0.4D}, {0.0D, -0.4D}};
      private static final double[][] STAR_PARTICLE_COORDS = new double[][]{{0.0D, 1.0D}, {0.3455D, 0.309D}, {0.9511D, 0.309D}, {0.3795918367346939D, -0.12653061224489795D}, {0.6122448979591837D, -0.8040816326530612D}, {0.0D, -0.35918367346938773D}};
      private int life;
      private final ParticleEngine engine;
      private final List<FireworkExplosion> explosions;
      private boolean twinkleDelay;

      public Starter(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, ParticleEngine particleEngine, List<FireworkExplosion> list) {
         super(clientLevel, d, e, f);
         this.xd = g;
         this.yd = h;
         this.zd = i;
         this.engine = particleEngine;
         if (list.isEmpty()) {
            throw new IllegalArgumentException("Cannot create firework starter with no explosions");
         } else {
            this.explosions = list;
            this.lifetime = list.size() * 2 - 1;
            Iterator var16 = list.iterator();

            while(var16.hasNext()) {
               FireworkExplosion fireworkExplosion = (FireworkExplosion)var16.next();
               if (fireworkExplosion.hasTwinkle()) {
                  this.twinkleDelay = true;
                  this.lifetime += 15;
                  break;
               }
            }

         }
      }

      public void tick() {
         boolean bl;
         if (this.life == 0) {
            bl = this.isFarAwayFromCamera();
            boolean bl2 = false;
            if (this.explosions.size() >= 3) {
               bl2 = true;
            } else {
               Iterator var3 = this.explosions.iterator();

               while(var3.hasNext()) {
                  FireworkExplosion fireworkExplosion = (FireworkExplosion)var3.next();
                  if (fireworkExplosion.shape() == Shape.LARGE_BALL) {
                     bl2 = true;
                     break;
                  }
               }
            }

            SoundEvent soundEvent;
            if (bl2) {
               soundEvent = bl ? SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR : SoundEvents.FIREWORK_ROCKET_LARGE_BLAST;
            } else {
               soundEvent = bl ? SoundEvents.FIREWORK_ROCKET_BLAST_FAR : SoundEvents.FIREWORK_ROCKET_BLAST;
            }

            this.level.playLocalSound(this.x, this.y, this.z, soundEvent, SoundSource.AMBIENT, 20.0F, 0.95F + this.random.nextFloat() * 0.1F, true);
         }

         if (this.life % 2 == 0 && this.life / 2 < this.explosions.size()) {
            int i = this.life / 2;
            FireworkExplosion fireworkExplosion2 = (FireworkExplosion)this.explosions.get(i);
            boolean bl3 = fireworkExplosion2.hasTrail();
            boolean bl4 = fireworkExplosion2.hasTwinkle();
            IntList intList = fireworkExplosion2.colors();
            IntList intList2 = fireworkExplosion2.fadeColors();
            if (intList.isEmpty()) {
               intList = IntList.of(DyeColor.BLACK.getFireworkColor());
            }

            switch(fireworkExplosion2.shape()) {
            case SMALL_BALL:
               this.createParticleBall(0.25D, 2, intList, intList2, bl3, bl4);
               break;
            case LARGE_BALL:
               this.createParticleBall(0.5D, 4, intList, intList2, bl3, bl4);
               break;
            case STAR:
               this.createParticleShape(0.5D, STAR_PARTICLE_COORDS, intList, intList2, bl3, bl4, false);
               break;
            case CREEPER:
               this.createParticleShape(0.5D, CREEPER_PARTICLE_COORDS, intList, intList2, bl3, bl4, true);
               break;
            case BURST:
               this.createParticleBurst(intList, intList2, bl3, bl4);
            }

            int j = intList.getInt(0);
            Particle particle = this.engine.createParticle(ParticleTypes.FLASH, this.x, this.y, this.z, 0.0D, 0.0D, 0.0D);
            particle.setColor((float)ARGB32.red(j) / 255.0F, (float)ARGB32.green(j) / 255.0F, (float)ARGB32.blue(j) / 255.0F);
         }

         ++this.life;
         if (this.life > this.lifetime) {
            if (this.twinkleDelay) {
               bl = this.isFarAwayFromCamera();
               SoundEvent soundEvent2 = bl ? SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR : SoundEvents.FIREWORK_ROCKET_TWINKLE;
               this.level.playLocalSound(this.x, this.y, this.z, soundEvent2, SoundSource.AMBIENT, 20.0F, 0.9F + this.random.nextFloat() * 0.15F, true);
            }

            this.remove();
         }

      }

      private boolean isFarAwayFromCamera() {
         Minecraft minecraft = Minecraft.getInstance();
         return minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(this.x, this.y, this.z) >= 256.0D;
      }

      private void createParticle(double d, double e, double f, double g, double h, double i, IntList intList, IntList intList2, boolean bl, boolean bl2) {
         FireworkParticles.SparkParticle sparkParticle = (FireworkParticles.SparkParticle)this.engine.createParticle(ParticleTypes.FIREWORK, d, e, f, g, h, i);
         sparkParticle.setTrail(bl);
         sparkParticle.setTwinkle(bl2);
         sparkParticle.setAlpha(0.99F);
         sparkParticle.setColor((Integer)Util.getRandom(intList, this.random));
         if (!intList2.isEmpty()) {
            sparkParticle.setFadeColor((Integer)Util.getRandom(intList2, this.random));
         }

      }

      private void createParticleBall(double d, int i, IntList intList, IntList intList2, boolean bl, boolean bl2) {
         double e = this.x;
         double f = this.y;
         double g = this.z;

         for(int j = -i; j <= i; ++j) {
            for(int k = -i; k <= i; ++k) {
               for(int l = -i; l <= i; ++l) {
                  double h = (double)k + (this.random.nextDouble() - this.random.nextDouble()) * 0.5D;
                  double m = (double)j + (this.random.nextDouble() - this.random.nextDouble()) * 0.5D;
                  double n = (double)l + (this.random.nextDouble() - this.random.nextDouble()) * 0.5D;
                  double o = Math.sqrt(h * h + m * m + n * n) / d + this.random.nextGaussian() * 0.05D;
                  this.createParticle(e, f, g, h / o, m / o, n / o, intList, intList2, bl, bl2);
                  if (j != -i && j != i && k != -i && k != i) {
                     l += i * 2 - 1;
                  }
               }
            }
         }

      }

      private void createParticleShape(double d, double[][] ds, IntList intList, IntList intList2, boolean bl, boolean bl2, boolean bl3) {
         double e = ds[0][0];
         double f = ds[0][1];
         this.createParticle(this.x, this.y, this.z, e * d, f * d, 0.0D, intList, intList2, bl, bl2);
         float g = this.random.nextFloat() * 3.1415927F;
         double h = bl3 ? 0.034D : 0.34D;

         for(int i = 0; i < 3; ++i) {
            double j = (double)g + (double)((float)i * 3.1415927F) * h;
            double k = e;
            double l = f;

            for(int m = 1; m < ds.length; ++m) {
               double n = ds[m][0];
               double o = ds[m][1];

               for(double p = 0.25D; p <= 1.0D; p += 0.25D) {
                  double q = Mth.lerp(p, k, n) * d;
                  double r = Mth.lerp(p, l, o) * d;
                  double s = q * Math.sin(j);
                  q *= Math.cos(j);

                  for(double t = -1.0D; t <= 1.0D; t += 2.0D) {
                     this.createParticle(this.x, this.y, this.z, q * t, r, s * t, intList, intList2, bl, bl2);
                  }
               }

               k = n;
               l = o;
            }
         }

      }

      private void createParticleBurst(IntList intList, IntList intList2, boolean bl, boolean bl2) {
         double d = this.random.nextGaussian() * 0.05D;
         double e = this.random.nextGaussian() * 0.05D;

         for(int i = 0; i < 70; ++i) {
            double f = this.xd * 0.5D + this.random.nextGaussian() * 0.15D + d;
            double g = this.zd * 0.5D + this.random.nextGaussian() * 0.15D + e;
            double h = this.yd * 0.5D + this.random.nextDouble() * 0.5D;
            this.createParticle(this.x, this.y, this.z, f, h, g, intList, intList2, bl, bl2);
         }

      }
   }
}
