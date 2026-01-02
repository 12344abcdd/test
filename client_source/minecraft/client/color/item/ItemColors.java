package net.minecraft.client.color.item;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMapper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public class ItemColors {
   private static final int DEFAULT = -1;
   private final IdMapper<ItemColor> itemColors = new IdMapper(32);

   public static ItemColors createDefault(BlockColors blockColors) {
      ItemColors itemColors = new ItemColors();
      itemColors.register((itemStack, i) -> {
         return i > 0 ? -1 : DyedItemColor.getOrDefault(itemStack, -6265536);
      }, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, Items.LEATHER_HORSE_ARMOR);
      itemColors.register((itemStack, i) -> {
         return i != 1 ? -1 : DyedItemColor.getOrDefault(itemStack, 0);
      }, Items.WOLF_ARMOR);
      itemColors.register((itemStack, i) -> {
         return GrassColor.get(0.5D, 1.0D);
      }, Blocks.TALL_GRASS, Blocks.LARGE_FERN);
      itemColors.register((itemStack, i) -> {
         if (i != 1) {
            return -1;
         } else {
            FireworkExplosion fireworkExplosion = (FireworkExplosion)itemStack.get(DataComponents.FIREWORK_EXPLOSION);
            IntList intList = fireworkExplosion != null ? fireworkExplosion.colors() : IntList.of();
            int j = intList.size();
            if (j == 0) {
               return -7697782;
            } else if (j == 1) {
               return ARGB32.opaque(intList.getInt(0));
            } else {
               int k = 0;
               int l = 0;
               int m = 0;

               for(int n = 0; n < j; ++n) {
                  int o = intList.getInt(n);
                  k += ARGB32.red(o);
                  l += ARGB32.green(o);
                  m += ARGB32.blue(o);
               }

               return ARGB32.color(k / j, l / j, m / j);
            }
         }
      }, Items.FIREWORK_STAR);
      itemColors.register((itemStack, i) -> {
         return i > 0 ? -1 : ARGB32.opaque(((PotionContents)itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)).getColor());
      }, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW);
      Iterator var2 = SpawnEggItem.eggs().iterator();

      while(var2.hasNext()) {
         SpawnEggItem spawnEggItem = (SpawnEggItem)var2.next();
         itemColors.register((itemStack, i) -> {
            return ARGB32.opaque(spawnEggItem.getColor(i));
         }, spawnEggItem);
      }

      itemColors.register((itemStack, i) -> {
         BlockState blockState = ((BlockItem)itemStack.getItem()).getBlock().defaultBlockState();
         return blockColors.getColor(blockState, (BlockAndTintGetter)null, (BlockPos)null, i);
      }, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.VINE, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.LILY_PAD);
      itemColors.register((itemStack, i) -> {
         return FoliageColor.getMangroveColor();
      }, Blocks.MANGROVE_LEAVES);
      itemColors.register((itemStack, i) -> {
         return i == 0 ? -1 : ARGB32.opaque(((MapItemColor)itemStack.getOrDefault(DataComponents.MAP_COLOR, MapItemColor.DEFAULT)).rgb());
      }, Items.FILLED_MAP);
      return itemColors;
   }

   public int getColor(ItemStack itemStack, int i) {
      ItemColor itemColor = (ItemColor)this.itemColors.byId(BuiltInRegistries.ITEM.getId(itemStack.getItem()));
      return itemColor == null ? -1 : itemColor.getColor(itemStack, i);
   }

   public void register(ItemColor itemColor, ItemLike... itemLikes) {
      ItemLike[] var3 = itemLikes;
      int var4 = itemLikes.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ItemLike itemLike = var3[var5];
         this.itemColors.addMapping(itemColor, Item.getId(itemLike.asItem()));
      }

   }
}
