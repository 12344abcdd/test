package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WeighedSoundEvents implements Weighted<Sound> {
   private final List<Weighted<Sound>> list = Lists.newArrayList();
   @Nullable
   private final Component subtitle;

   public WeighedSoundEvents(ResourceLocation resourceLocation, @Nullable String string) {
      this.subtitle = string == null ? null : Component.translatable(string);
   }

   public int getWeight() {
      int i = 0;

      Weighted weighted;
      for(Iterator var2 = this.list.iterator(); var2.hasNext(); i += weighted.getWeight()) {
         weighted = (Weighted)var2.next();
      }

      return i;
   }

   public Sound getSound(RandomSource randomSource) {
      int i = this.getWeight();
      if (!this.list.isEmpty() && i != 0) {
         int j = randomSource.nextInt(i);
         Iterator var4 = this.list.iterator();

         Weighted weighted;
         do {
            if (!var4.hasNext()) {
               return SoundManager.EMPTY_SOUND;
            }

            weighted = (Weighted)var4.next();
            j -= weighted.getWeight();
         } while(j >= 0);

         return (Sound)weighted.getSound(randomSource);
      } else {
         return SoundManager.EMPTY_SOUND;
      }
   }

   public void addSound(Weighted<Sound> weighted) {
      this.list.add(weighted);
   }

   @Nullable
   public Component getSubtitle() {
      return this.subtitle;
   }

   public void preloadIfRequired(SoundEngine soundEngine) {
      Iterator var2 = this.list.iterator();

      while(var2.hasNext()) {
         Weighted<Sound> weighted = (Weighted)var2.next();
         weighted.preloadIfRequired(soundEngine);
      }

   }

   // $FF: synthetic method
   public Object getSound(final RandomSource randomSource) {
      return this.getSound(randomSource);
   }
}
