package net.minecraft.client.gui.font;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringRepresentable;

@Environment(EnvType.CLIENT)
public enum FontOption implements StringRepresentable {
   UNIFORM("uniform"),
   JAPANESE_VARIANTS("jp");

   public static final Codec<FontOption> CODEC = StringRepresentable.fromEnum(FontOption::values);
   private final String name;

   private FontOption(final String string2) {
      this.name = string2;
   }

   public String getSerializedName() {
      return this.name;
   }

   // $FF: synthetic method
   private static FontOption[] $values() {
      return new FontOption[]{UNIFORM, JAPANESE_VARIANTS};
   }

   @Environment(EnvType.CLIENT)
   public static class Filter {
      private final Map<FontOption, Boolean> values;
      public static final Codec<FontOption.Filter> CODEC;
      public static final FontOption.Filter ALWAYS_PASS;

      public Filter(Map<FontOption, Boolean> map) {
         this.values = map;
      }

      public boolean apply(Set<FontOption> set) {
         Iterator var2 = this.values.entrySet().iterator();

         Entry entry;
         do {
            if (!var2.hasNext()) {
               return true;
            }

            entry = (Entry)var2.next();
         } while(set.contains(entry.getKey()) == (Boolean)entry.getValue());

         return false;
      }

      public FontOption.Filter merge(FontOption.Filter filter) {
         Map<FontOption, Boolean> map = new HashMap(filter.values);
         map.putAll(this.values);
         return new FontOption.Filter(Map.copyOf(map));
      }

      static {
         CODEC = Codec.unboundedMap(FontOption.CODEC, Codec.BOOL).xmap(FontOption.Filter::new, (filter) -> {
            return filter.values;
         });
         ALWAYS_PASS = new FontOption.Filter(Map.of());
      }
   }
}
