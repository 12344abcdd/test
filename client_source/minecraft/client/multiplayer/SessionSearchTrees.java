package net.minecraft.client.multiplayer;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.Registry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag.Default;

@Environment(EnvType.CLIENT)
public class SessionSearchTrees {
   private static final SessionSearchTrees.Key RECIPE_COLLECTIONS = new SessionSearchTrees.Key();
   private static final SessionSearchTrees.Key CREATIVE_NAMES = new SessionSearchTrees.Key();
   private static final SessionSearchTrees.Key CREATIVE_TAGS = new SessionSearchTrees.Key();
   private CompletableFuture<SearchTree<ItemStack>> creativeByNameSearch = CompletableFuture.completedFuture(SearchTree.empty());
   private CompletableFuture<SearchTree<ItemStack>> creativeByTagSearch = CompletableFuture.completedFuture(SearchTree.empty());
   private CompletableFuture<SearchTree<RecipeCollection>> recipeSearch = CompletableFuture.completedFuture(SearchTree.empty());
   private final Map<SessionSearchTrees.Key, Runnable> reloaders = new IdentityHashMap();

   private void register(SessionSearchTrees.Key key, Runnable runnable) {
      runnable.run();
      this.reloaders.put(key, runnable);
   }

   public void rebuildAfterLanguageChange() {
      Iterator var1 = this.reloaders.values().iterator();

      while(var1.hasNext()) {
         Runnable runnable = (Runnable)var1.next();
         runnable.run();
      }

   }

   private static Stream<String> getTooltipLines(Stream<ItemStack> stream, TooltipContext tooltipContext, TooltipFlag tooltipFlag) {
      return stream.flatMap((itemStack) -> {
         return itemStack.getTooltipLines(tooltipContext, (Player)null, tooltipFlag).stream();
      }).map((component) -> {
         return ChatFormatting.stripFormatting(component.getString()).trim();
      }).filter((string) -> {
         return !string.isEmpty();
      });
   }

   public void updateRecipes(ClientRecipeBook clientRecipeBook, Frozen frozen) {
      this.register(RECIPE_COLLECTIONS, () -> {
         List<RecipeCollection> list = clientRecipeBook.getCollections();
         Registry<Item> registry = frozen.registryOrThrow(Registries.ITEM);
         TooltipContext tooltipContext = TooltipContext.of(frozen);
         TooltipFlag tooltipFlag = Default.NORMAL;
         CompletableFuture<?> completableFuture = this.recipeSearch;
         this.recipeSearch = CompletableFuture.supplyAsync(() -> {
            return new FullTextSearchTree((recipeCollection) -> {
               return getTooltipLines(recipeCollection.getRecipes().stream().map((recipeHolder) -> {
                  return recipeHolder.value().getResultItem(frozen);
               }), tooltipContext, tooltipFlag);
            }, (recipeCollection) -> {
               return recipeCollection.getRecipes().stream().map((recipeHolder) -> {
                  return registry.getKey(recipeHolder.value().getResultItem(frozen).getItem());
               });
            }, list);
         }, Util.backgroundExecutor());
         completableFuture.cancel(true);
      });
   }

   public SearchTree<RecipeCollection> recipes() {
      return (SearchTree)this.recipeSearch.join();
   }

   public void updateCreativeTags(List<ItemStack> list) {
      this.register(CREATIVE_TAGS, () -> {
         CompletableFuture<?> completableFuture = this.creativeByTagSearch;
         this.creativeByTagSearch = CompletableFuture.supplyAsync(() -> {
            return new IdSearchTree((itemStack) -> {
               return itemStack.getTags().map(TagKey::location);
            }, list);
         }, Util.backgroundExecutor());
         completableFuture.cancel(true);
      });
   }

   public SearchTree<ItemStack> creativeTagSearch() {
      return (SearchTree)this.creativeByTagSearch.join();
   }

   public void updateCreativeTooltips(Provider provider, List<ItemStack> list) {
      this.register(CREATIVE_NAMES, () -> {
         TooltipContext tooltipContext = TooltipContext.of(provider);
         TooltipFlag tooltipFlag = Default.NORMAL.asCreative();
         CompletableFuture<?> completableFuture = this.creativeByNameSearch;
         this.creativeByNameSearch = CompletableFuture.supplyAsync(() -> {
            return new FullTextSearchTree((itemStack) -> {
               return getTooltipLines(Stream.of(itemStack), tooltipContext, tooltipFlag);
            }, (itemStack) -> {
               return itemStack.getItemHolder().unwrapKey().map(ResourceKey::location).stream();
            }, list);
         }, Util.backgroundExecutor());
         completableFuture.cancel(true);
      });
   }

   public SearchTree<ItemStack> creativeNameSearch() {
      return (SearchTree)this.creativeByNameSearch.join();
   }

   @Environment(EnvType.CLIENT)
   private static class Key {
      Key() {
      }
   }
}
