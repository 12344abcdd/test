package net.minecraft.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientRecipeBook extends RecipeBook {
   private static final Logger LOGGER = LogUtils.getLogger();
   private Map<RecipeBookCategories, List<RecipeCollection>> collectionsByTab = ImmutableMap.of();
   private List<RecipeCollection> allCollections = ImmutableList.of();

   public void setupCollections(Iterable<RecipeHolder<?>> iterable, RegistryAccess registryAccess) {
      Map<RecipeBookCategories, List<List<RecipeHolder<?>>>> map = categorizeAndGroupRecipes(iterable);
      Map<RecipeBookCategories, List<RecipeCollection>> map2 = Maps.newHashMap();
      Builder<RecipeCollection> builder = ImmutableList.builder();
      map.forEach((recipeBookCategories, list) -> {
         Stream var10002 = list.stream().map((listx) -> {
            return new RecipeCollection(registryAccess, listx);
         });
         Objects.requireNonNull(builder);
         map2.put(recipeBookCategories, (List)var10002.peek(builder::add).collect(ImmutableList.toImmutableList()));
      });
      RecipeBookCategories.AGGREGATE_CATEGORIES.forEach((recipeBookCategories, list) -> {
         map2.put(recipeBookCategories, (List)list.stream().flatMap((recipeBookCategoriesx) -> {
            return ((List)map2.getOrDefault(recipeBookCategoriesx, ImmutableList.of())).stream();
         }).collect(ImmutableList.toImmutableList()));
      });
      this.collectionsByTab = ImmutableMap.copyOf(map2);
      this.allCollections = builder.build();
   }

   private static Map<RecipeBookCategories, List<List<RecipeHolder<?>>>> categorizeAndGroupRecipes(Iterable<RecipeHolder<?>> iterable) {
      Map<RecipeBookCategories, List<List<RecipeHolder<?>>>> map = Maps.newHashMap();
      Table<RecipeBookCategories, String, List<RecipeHolder<?>>> table = HashBasedTable.create();
      Iterator var3 = iterable.iterator();

      while(var3.hasNext()) {
         RecipeHolder<?> recipeHolder = (RecipeHolder)var3.next();
         Recipe<?> recipe = recipeHolder.value();
         if (!recipe.isSpecial() && !recipe.isIncomplete()) {
            RecipeBookCategories recipeBookCategories = getCategory(recipeHolder);
            String string = recipe.getGroup();
            if (string.isEmpty()) {
               ((List)map.computeIfAbsent(recipeBookCategories, (recipeBookCategoriesx) -> {
                  return Lists.newArrayList();
               })).add(ImmutableList.of(recipeHolder));
            } else {
               List<RecipeHolder<?>> list = (List)table.get(recipeBookCategories, string);
               if (list == null) {
                  list = Lists.newArrayList();
                  table.put(recipeBookCategories, string, list);
                  ((List)map.computeIfAbsent(recipeBookCategories, (recipeBookCategoriesx) -> {
                     return Lists.newArrayList();
                  })).add(list);
               }

               ((List)list).add(recipeHolder);
            }
         }
      }

      return map;
   }

   private static RecipeBookCategories getCategory(RecipeHolder<?> recipeHolder) {
      Recipe<?> recipe = recipeHolder.value();
      RecipeBookCategories var5;
      if (recipe instanceof CraftingRecipe) {
         CraftingRecipe craftingRecipe = (CraftingRecipe)recipe;
         switch(craftingRecipe.category()) {
         case BUILDING:
            var5 = RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
            break;
         case EQUIPMENT:
            var5 = RecipeBookCategories.CRAFTING_EQUIPMENT;
            break;
         case REDSTONE:
            var5 = RecipeBookCategories.CRAFTING_REDSTONE;
            break;
         case MISC:
            var5 = RecipeBookCategories.CRAFTING_MISC;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var5;
      } else {
         RecipeType<?> recipeType = recipe.getType();
         if (recipe instanceof AbstractCookingRecipe) {
            AbstractCookingRecipe abstractCookingRecipe = (AbstractCookingRecipe)recipe;
            CookingBookCategory cookingBookCategory = abstractCookingRecipe.category();
            if (recipeType == RecipeType.SMELTING) {
               switch(cookingBookCategory) {
               case BLOCKS:
                  var5 = RecipeBookCategories.FURNACE_BLOCKS;
                  break;
               case FOOD:
                  var5 = RecipeBookCategories.FURNACE_FOOD;
                  break;
               case MISC:
                  var5 = RecipeBookCategories.FURNACE_MISC;
                  break;
               default:
                  throw new MatchException((String)null, (Throwable)null);
               }

               return var5;
            }

            if (recipeType == RecipeType.BLASTING) {
               return cookingBookCategory == CookingBookCategory.BLOCKS ? RecipeBookCategories.BLAST_FURNACE_BLOCKS : RecipeBookCategories.BLAST_FURNACE_MISC;
            }

            if (recipeType == RecipeType.SMOKING) {
               return RecipeBookCategories.SMOKER_FOOD;
            }

            if (recipeType == RecipeType.CAMPFIRE_COOKING) {
               return RecipeBookCategories.CAMPFIRE;
            }
         }

         if (recipeType == RecipeType.STONECUTTING) {
            return RecipeBookCategories.STONECUTTER;
         } else if (recipeType == RecipeType.SMITHING) {
            return RecipeBookCategories.SMITHING;
         } else {
            Logger var10000 = LOGGER;
            Object var10002 = LogUtils.defer(() -> {
               return BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            });
            Objects.requireNonNull(recipeHolder);
            var10000.warn("Unknown recipe category: {}/{}", var10002, LogUtils.defer(recipeHolder::id));
            return RecipeBookCategories.UNKNOWN;
         }
      }
   }

   public List<RecipeCollection> getCollections() {
      return this.allCollections;
   }

   public List<RecipeCollection> getCollection(RecipeBookCategories recipeBookCategories) {
      return (List)this.collectionsByTab.getOrDefault(recipeBookCategories, Collections.emptyList());
   }
}
