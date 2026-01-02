package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.audio.ListenerTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SubtitleOverlay implements SoundEventListener {
   private static final long DISPLAY_TIME = 3000L;
   private final Minecraft minecraft;
   private final List<SubtitleOverlay.Subtitle> subtitles = Lists.newArrayList();
   private boolean isListening;
   private final List<SubtitleOverlay.Subtitle> audibleSubtitles = new ArrayList();

   public SubtitleOverlay(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(GuiGraphics guiGraphics) {
      SoundManager soundManager = this.minecraft.getSoundManager();
      if (!this.isListening && (Boolean)this.minecraft.options.showSubtitles().get()) {
         soundManager.addListener(this);
         this.isListening = true;
      } else if (this.isListening && !(Boolean)this.minecraft.options.showSubtitles().get()) {
         soundManager.removeListener(this);
         this.isListening = false;
      }

      if (this.isListening) {
         ListenerTransform listenerTransform = soundManager.getListenerTransform();
         Vec3 vec3 = listenerTransform.position();
         Vec3 vec32 = listenerTransform.forward();
         Vec3 vec33 = listenerTransform.right();
         this.audibleSubtitles.clear();
         Iterator var7 = this.subtitles.iterator();

         while(var7.hasNext()) {
            SubtitleOverlay.Subtitle subtitle = (SubtitleOverlay.Subtitle)var7.next();
            if (subtitle.isAudibleFrom(vec3)) {
               this.audibleSubtitles.add(subtitle);
            }
         }

         if (!this.audibleSubtitles.isEmpty()) {
            int i = 0;
            int j = 0;
            double d = (Double)this.minecraft.options.notificationDisplayTime().get();
            Iterator iterator = this.audibleSubtitles.iterator();

            SubtitleOverlay.Subtitle subtitle2;
            while(iterator.hasNext()) {
               subtitle2 = (SubtitleOverlay.Subtitle)iterator.next();
               subtitle2.purgeOldInstances(3000.0D * d);
               if (!subtitle2.isStillActive()) {
                  iterator.remove();
               } else {
                  j = Math.max(j, this.minecraft.font.width((FormattedText)subtitle2.getText()));
               }
            }

            j += this.minecraft.font.width("<") + this.minecraft.font.width(" ") + this.minecraft.font.width(">") + this.minecraft.font.width(" ");
            iterator = this.audibleSubtitles.iterator();

            while(iterator.hasNext()) {
               subtitle2 = (SubtitleOverlay.Subtitle)iterator.next();
               int k = true;
               Component component = subtitle2.getText();
               SubtitleOverlay.SoundPlayedAt soundPlayedAt = subtitle2.getClosest(vec3);
               if (soundPlayedAt != null) {
                  Vec3 vec34 = soundPlayedAt.location.subtract(vec3).normalize();
                  double e = vec33.dot(vec34);
                  double f = vec32.dot(vec34);
                  boolean bl = f > 0.5D;
                  int l = j / 2;
                  Objects.requireNonNull(this.minecraft.font);
                  int m = 9;
                  int n = m / 2;
                  float g = 1.0F;
                  int o = this.minecraft.font.width((FormattedText)component);
                  int p = Mth.floor(Mth.clampedLerp(255.0F, 75.0F, (float)(Util.getMillis() - soundPlayedAt.time) / (float)(3000.0D * d)));
                  int q = p << 16 | p << 8 | p;
                  guiGraphics.pose().pushPose();
                  guiGraphics.pose().translate((float)guiGraphics.guiWidth() - (float)l * 1.0F - 2.0F, (float)(guiGraphics.guiHeight() - 35) - (float)(i * (m + 1)) * 1.0F, 0.0F);
                  guiGraphics.pose().scale(1.0F, 1.0F, 1.0F);
                  guiGraphics.fill(-l - 1, -n - 1, l + 1, n + 1, this.minecraft.options.getBackgroundColor(0.8F));
                  int r = q + -16777216;
                  if (!bl) {
                     if (e > 0.0D) {
                        guiGraphics.drawString(this.minecraft.font, ">", l - this.minecraft.font.width(">"), -n, r);
                     } else if (e < 0.0D) {
                        guiGraphics.drawString(this.minecraft.font, "<", -l, -n, r);
                     }
                  }

                  guiGraphics.drawString(this.minecraft.font, component, -o / 2, -n, r);
                  guiGraphics.pose().popPose();
                  ++i;
               }
            }

         }
      }
   }

   public void onPlaySound(SoundInstance soundInstance, WeighedSoundEvents weighedSoundEvents, float f) {
      if (weighedSoundEvents.getSubtitle() != null) {
         Component component = weighedSoundEvents.getSubtitle();
         if (!this.subtitles.isEmpty()) {
            Iterator var5 = this.subtitles.iterator();

            while(var5.hasNext()) {
               SubtitleOverlay.Subtitle subtitle = (SubtitleOverlay.Subtitle)var5.next();
               if (subtitle.getText().equals(component)) {
                  subtitle.refresh(new Vec3(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ()));
                  return;
               }
            }
         }

         this.subtitles.add(new SubtitleOverlay.Subtitle(component, f, new Vec3(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ())));
      }
   }

   @Environment(EnvType.CLIENT)
   static class Subtitle {
      private final Component text;
      private final float range;
      private final List<SubtitleOverlay.SoundPlayedAt> playedAt = new ArrayList();

      public Subtitle(Component component, float f, Vec3 vec3) {
         this.text = component;
         this.range = f;
         this.playedAt.add(new SubtitleOverlay.SoundPlayedAt(vec3, Util.getMillis()));
      }

      public Component getText() {
         return this.text;
      }

      @Nullable
      public SubtitleOverlay.SoundPlayedAt getClosest(Vec3 vec3) {
         if (this.playedAt.isEmpty()) {
            return null;
         } else {
            return this.playedAt.size() == 1 ? (SubtitleOverlay.SoundPlayedAt)this.playedAt.getFirst() : (SubtitleOverlay.SoundPlayedAt)this.playedAt.stream().min(Comparator.comparingDouble((soundPlayedAt) -> {
               return soundPlayedAt.location().distanceTo(vec3);
            })).orElse((Object)null);
         }
      }

      public void refresh(Vec3 vec3) {
         this.playedAt.removeIf((soundPlayedAt) -> {
            return vec3.equals(soundPlayedAt.location());
         });
         this.playedAt.add(new SubtitleOverlay.SoundPlayedAt(vec3, Util.getMillis()));
      }

      public boolean isAudibleFrom(Vec3 vec3) {
         if (Float.isInfinite(this.range)) {
            return true;
         } else if (this.playedAt.isEmpty()) {
            return false;
         } else {
            SubtitleOverlay.SoundPlayedAt soundPlayedAt = this.getClosest(vec3);
            return soundPlayedAt == null ? false : vec3.closerThan(soundPlayedAt.location, (double)this.range);
         }
      }

      public void purgeOldInstances(double d) {
         long l = Util.getMillis();
         this.playedAt.removeIf((soundPlayedAt) -> {
            return (double)(l - soundPlayedAt.time()) > d;
         });
      }

      public boolean isStillActive() {
         return !this.playedAt.isEmpty();
      }
   }

   @Environment(EnvType.CLIENT)
   static record SoundPlayedAt(Vec3 location, long time) {
      final Vec3 location;
      final long time;

      SoundPlayedAt(Vec3 vec3, long l) {
         this.location = vec3;
         this.time = l;
      }

      public Vec3 location() {
         return this.location;
      }

      public long time() {
         return this.time;
      }
   }
}
