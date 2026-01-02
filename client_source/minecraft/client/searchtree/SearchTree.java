package net.minecraft.client.searchtree;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface SearchTree<T> {
   static <T> SearchTree<T> empty() {
      return (string) -> {
         return List.of();
      };
   }

   static <T> SearchTree<T> plainText(List<T> list, Function<T, Stream<String>> function) {
      if (list.isEmpty()) {
         return empty();
      } else {
         SuffixArray<T> suffixArray = new SuffixArray();
         Iterator var3 = list.iterator();

         while(var3.hasNext()) {
            T object = var3.next();
            ((Stream)function.apply(object)).forEach((string) -> {
               suffixArray.add(object, string.toLowerCase(Locale.ROOT));
            });
         }

         suffixArray.generate();
         Objects.requireNonNull(suffixArray);
         return suffixArray::search;
      }
   }

   List<T> search(String string);
}
