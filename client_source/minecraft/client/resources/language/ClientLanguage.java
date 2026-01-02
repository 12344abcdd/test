package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientLanguage extends Language {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, String> storage;
   private final boolean defaultRightToLeft;

   private ClientLanguage(Map<String, String> map, boolean bl) {
      this.storage = map;
      this.defaultRightToLeft = bl;
   }

   public static ClientLanguage loadFrom(ResourceManager resourceManager, List<String> list, boolean bl) {
      Map<String, String> map = Maps.newHashMap();
      Iterator var4 = list.iterator();

      while(var4.hasNext()) {
         String string = (String)var4.next();
         String string2 = String.format(Locale.ROOT, "lang/%s.json", string);
         Iterator var7 = resourceManager.getNamespaces().iterator();

         while(var7.hasNext()) {
            String string3 = (String)var7.next();

            try {
               ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(string3, string2);
               appendFrom(string, resourceManager.getResourceStack(resourceLocation), map);
            } catch (Exception var10) {
               LOGGER.warn("Skipped language file: {}:{} ({})", new Object[]{string3, string2, var10.toString()});
            }
         }
      }

      return new ClientLanguage(ImmutableMap.copyOf(map), bl);
   }

   private static void appendFrom(String string, List<Resource> list, Map<String, String> map) {
      Iterator var3 = list.iterator();

      while(var3.hasNext()) {
         Resource resource = (Resource)var3.next();

         try {
            InputStream inputStream = resource.open();

            try {
               Objects.requireNonNull(map);
               Language.loadFromJson(inputStream, map::put);
            } catch (Throwable var9) {
               if (inputStream != null) {
                  try {
                     inputStream.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (inputStream != null) {
               inputStream.close();
            }
         } catch (IOException var10) {
            LOGGER.warn("Failed to load translations for {} from pack {}", new Object[]{string, resource.sourcePackId(), var10});
         }
      }

   }

   public String getOrDefault(String string, String string2) {
      return (String)this.storage.getOrDefault(string, string2);
   }

   public boolean has(String string) {
      return this.storage.containsKey(string);
   }

   public boolean isDefaultRightToLeft() {
      return this.defaultRightToLeft;
   }

   public FormattedCharSequence getVisualOrder(FormattedText formattedText) {
      return FormattedBidiReorder.reorder(formattedText, this.defaultRightToLeft);
   }
}
