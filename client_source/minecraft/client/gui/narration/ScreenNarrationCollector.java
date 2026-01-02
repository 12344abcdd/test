package net.minecraft.client.gui.narration;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ScreenNarrationCollector {
   int generation;
   final Map<ScreenNarrationCollector.EntryKey, ScreenNarrationCollector.NarrationEntry> entries = Maps.newTreeMap(Comparator.comparing((entryKey) -> {
      return entryKey.type;
   }).thenComparing((entryKey) -> {
      return entryKey.depth;
   }));

   public void update(Consumer<NarrationElementOutput> consumer) {
      ++this.generation;
      consumer.accept(new ScreenNarrationCollector.Output(0));
   }

   public String collectNarrationText(boolean bl) {
      final StringBuilder stringBuilder = new StringBuilder();
      Consumer<String> consumer = new Consumer<String>(this) {
         private boolean firstEntry = true;

         public void accept(String string) {
            if (!this.firstEntry) {
               stringBuilder.append(". ");
            }

            this.firstEntry = false;
            stringBuilder.append(string);
         }

         // $FF: synthetic method
         public void accept(final Object object) {
            this.accept((String)object);
         }
      };
      this.entries.forEach((entryKey, narrationEntry) -> {
         if (narrationEntry.generation == this.generation && (bl || !narrationEntry.alreadyNarrated)) {
            narrationEntry.contents.getText(consumer);
            narrationEntry.alreadyNarrated = true;
         }

      });
      return stringBuilder.toString();
   }

   @Environment(EnvType.CLIENT)
   private class Output implements NarrationElementOutput {
      private final int depth;

      Output(final int i) {
         this.depth = i;
      }

      public void add(NarratedElementType narratedElementType, NarrationThunk<?> narrationThunk) {
         ((ScreenNarrationCollector.NarrationEntry)ScreenNarrationCollector.this.entries.computeIfAbsent(new ScreenNarrationCollector.EntryKey(narratedElementType, this.depth), (entryKey) -> {
            return new ScreenNarrationCollector.NarrationEntry();
         })).update(ScreenNarrationCollector.this.generation, narrationThunk);
      }

      public NarrationElementOutput nest() {
         return ScreenNarrationCollector.this.new Output(this.depth + 1);
      }
   }

   @Environment(EnvType.CLIENT)
   private static class NarrationEntry {
      NarrationThunk<?> contents;
      int generation;
      boolean alreadyNarrated;

      NarrationEntry() {
         this.contents = NarrationThunk.EMPTY;
         this.generation = -1;
      }

      public ScreenNarrationCollector.NarrationEntry update(int i, NarrationThunk<?> narrationThunk) {
         if (!this.contents.equals(narrationThunk)) {
            this.contents = narrationThunk;
            this.alreadyNarrated = false;
         } else if (this.generation + 1 != i) {
            this.alreadyNarrated = false;
         }

         this.generation = i;
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class EntryKey {
      final NarratedElementType type;
      final int depth;

      EntryKey(NarratedElementType narratedElementType, int i) {
         this.type = narratedElementType;
         this.depth = i;
      }
   }
}
