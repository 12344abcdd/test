package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class Stitcher<T extends Stitcher.Entry> {
   private static final Comparator<Stitcher.Holder<?>> HOLDER_COMPARATOR = Comparator.comparing((holder) -> {
      return -holder.height;
   }).thenComparing((holder) -> {
      return -holder.width;
   }).thenComparing((holder) -> {
      return holder.entry.name();
   });
   private final int mipLevel;
   private final List<Stitcher.Holder<T>> texturesToBeStitched = new ArrayList();
   private final List<Stitcher.Region<T>> storage = new ArrayList();
   private int storageX;
   private int storageY;
   private final int maxWidth;
   private final int maxHeight;

   public Stitcher(int i, int j, int k) {
      this.mipLevel = k;
      this.maxWidth = i;
      this.maxHeight = j;
   }

   public int getWidth() {
      return this.storageX;
   }

   public int getHeight() {
      return this.storageY;
   }

   public void registerSprite(T entry) {
      Stitcher.Holder<T> holder = new Stitcher.Holder(entry, this.mipLevel);
      this.texturesToBeStitched.add(holder);
   }

   public void stitch() {
      List<Stitcher.Holder<T>> list = new ArrayList(this.texturesToBeStitched);
      list.sort(HOLDER_COMPARATOR);
      Iterator var2 = list.iterator();

      Stitcher.Holder holder;
      do {
         if (!var2.hasNext()) {
            return;
         }

         holder = (Stitcher.Holder)var2.next();
      } while(this.addToStorage(holder));

      throw new StitcherException(holder.entry, (Collection)list.stream().map((holderx) -> {
         return holderx.entry;
      }).collect(ImmutableList.toImmutableList()));
   }

   public void gatherSprites(Stitcher.SpriteLoader<T> spriteLoader) {
      Iterator var2 = this.storage.iterator();

      while(var2.hasNext()) {
         Stitcher.Region<T> region = (Stitcher.Region)var2.next();
         region.walk(spriteLoader);
      }

   }

   static int smallestFittingMinTexel(int i, int j) {
      return (i >> j) + ((i & (1 << j) - 1) == 0 ? 0 : 1) << j;
   }

   private boolean addToStorage(Stitcher.Holder<T> holder) {
      Iterator var2 = this.storage.iterator();

      Stitcher.Region region;
      do {
         if (!var2.hasNext()) {
            return this.expand(holder);
         }

         region = (Stitcher.Region)var2.next();
      } while(!region.add(holder));

      return true;
   }

   private boolean expand(Stitcher.Holder<T> holder) {
      int i = Mth.smallestEncompassingPowerOfTwo(this.storageX);
      int j = Mth.smallestEncompassingPowerOfTwo(this.storageY);
      int k = Mth.smallestEncompassingPowerOfTwo(this.storageX + holder.width);
      int l = Mth.smallestEncompassingPowerOfTwo(this.storageY + holder.height);
      boolean bl = k <= this.maxWidth;
      boolean bl2 = l <= this.maxHeight;
      if (!bl && !bl2) {
         return false;
      } else {
         boolean bl3 = bl && i != k;
         boolean bl4 = bl2 && j != l;
         boolean bl5;
         if (bl3 ^ bl4) {
            bl5 = bl3;
         } else {
            bl5 = bl && i <= j;
         }

         Stitcher.Region region;
         if (bl5) {
            if (this.storageY == 0) {
               this.storageY = l;
            }

            region = new Stitcher.Region(this.storageX, 0, k - this.storageX, this.storageY);
            this.storageX = k;
         } else {
            region = new Stitcher.Region(0, this.storageY, this.storageX, l - this.storageY);
            this.storageY = l;
         }

         region.add(holder);
         this.storage.add(region);
         return true;
      }
   }

   @Environment(EnvType.CLIENT)
   private static record Holder<T extends Stitcher.Entry>(T entry, int width, int height) {
      final T entry;
      final int width;
      final int height;

      public Holder(T entry, int i) {
         this(entry, Stitcher.smallestFittingMinTexel(entry.width(), i), Stitcher.smallestFittingMinTexel(entry.height(), i));
      }

      private Holder(T entry, int i, int j) {
         this.entry = entry;
         this.width = i;
         this.height = j;
      }

      public T entry() {
         return this.entry;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Entry {
      int width();

      int height();

      ResourceLocation name();
   }

   @Environment(EnvType.CLIENT)
   public static class Region<T extends Stitcher.Entry> {
      private final int originX;
      private final int originY;
      private final int width;
      private final int height;
      @Nullable
      private List<Stitcher.Region<T>> subSlots;
      @Nullable
      private Stitcher.Holder<T> holder;

      public Region(int i, int j, int k, int l) {
         this.originX = i;
         this.originY = j;
         this.width = k;
         this.height = l;
      }

      public int getX() {
         return this.originX;
      }

      public int getY() {
         return this.originY;
      }

      public boolean add(Stitcher.Holder<T> holder) {
         if (this.holder != null) {
            return false;
         } else {
            int i = holder.width;
            int j = holder.height;
            if (i <= this.width && j <= this.height) {
               if (i == this.width && j == this.height) {
                  this.holder = holder;
                  return true;
               } else {
                  if (this.subSlots == null) {
                     this.subSlots = new ArrayList(1);
                     this.subSlots.add(new Stitcher.Region(this.originX, this.originY, i, j));
                     int k = this.width - i;
                     int l = this.height - j;
                     if (l > 0 && k > 0) {
                        int m = Math.max(this.height, k);
                        int n = Math.max(this.width, l);
                        if (m >= n) {
                           this.subSlots.add(new Stitcher.Region(this.originX, this.originY + j, i, l));
                           this.subSlots.add(new Stitcher.Region(this.originX + i, this.originY, k, this.height));
                        } else {
                           this.subSlots.add(new Stitcher.Region(this.originX + i, this.originY, k, j));
                           this.subSlots.add(new Stitcher.Region(this.originX, this.originY + j, this.width, l));
                        }
                     } else if (k == 0) {
                        this.subSlots.add(new Stitcher.Region(this.originX, this.originY + j, i, l));
                     } else if (l == 0) {
                        this.subSlots.add(new Stitcher.Region(this.originX + i, this.originY, k, j));
                     }
                  }

                  Iterator var8 = this.subSlots.iterator();

                  Stitcher.Region region;
                  do {
                     if (!var8.hasNext()) {
                        return false;
                     }

                     region = (Stitcher.Region)var8.next();
                  } while(!region.add(holder));

                  return true;
               }
            } else {
               return false;
            }
         }
      }

      public void walk(Stitcher.SpriteLoader<T> spriteLoader) {
         if (this.holder != null) {
            spriteLoader.load(this.holder.entry, this.getX(), this.getY());
         } else if (this.subSlots != null) {
            Iterator var2 = this.subSlots.iterator();

            while(var2.hasNext()) {
               Stitcher.Region<T> region = (Stitcher.Region)var2.next();
               region.walk(spriteLoader);
            }
         }

      }

      public String toString() {
         int var10000 = this.originX;
         return "Slot{originX=" + var10000 + ", originY=" + this.originY + ", width=" + this.width + ", height=" + this.height + ", texture=" + String.valueOf(this.holder) + ", subSlots=" + String.valueOf(this.subSlots) + "}";
      }
   }

   @Environment(EnvType.CLIENT)
   public interface SpriteLoader<T extends Stitcher.Entry> {
      void load(T entry, int i, int j);
   }
}
