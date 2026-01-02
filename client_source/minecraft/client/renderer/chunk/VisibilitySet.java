package net.minecraft.client.renderer.chunk;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;

@Environment(EnvType.CLIENT)
public class VisibilitySet {
   private static final int FACINGS = Direction.values().length;
   private final BitSet data;

   public VisibilitySet() {
      this.data = new BitSet(FACINGS * FACINGS);
   }

   public void add(Set<Direction> set) {
      Iterator var2 = set.iterator();

      while(var2.hasNext()) {
         Direction direction = (Direction)var2.next();
         Iterator var4 = set.iterator();

         while(var4.hasNext()) {
            Direction direction2 = (Direction)var4.next();
            this.set(direction, direction2, true);
         }
      }

   }

   public void set(Direction direction, Direction direction2, boolean bl) {
      this.data.set(direction.ordinal() + direction2.ordinal() * FACINGS, bl);
      this.data.set(direction2.ordinal() + direction.ordinal() * FACINGS, bl);
   }

   public void setAll(boolean bl) {
      this.data.set(0, this.data.size(), bl);
   }

   public boolean visibilityBetween(Direction direction, Direction direction2) {
      return this.data.get(direction.ordinal() + direction2.ordinal() * FACINGS);
   }

   public String toString() {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(' ');
      Direction[] var2 = Direction.values();
      int var3 = var2.length;

      int var4;
      Direction direction;
      for(var4 = 0; var4 < var3; ++var4) {
         direction = var2[var4];
         stringBuilder.append(' ').append(direction.toString().toUpperCase().charAt(0));
      }

      stringBuilder.append('\n');
      var2 = Direction.values();
      var3 = var2.length;

      for(var4 = 0; var4 < var3; ++var4) {
         direction = var2[var4];
         stringBuilder.append(direction.toString().toUpperCase().charAt(0));
         Direction[] var6 = Direction.values();
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            Direction direction2 = var6[var8];
            if (direction == direction2) {
               stringBuilder.append("  ");
            } else {
               boolean bl = this.visibilityBetween(direction, direction2);
               stringBuilder.append(' ').append((char)(bl ? 'Y' : 'n'));
            }
         }

         stringBuilder.append('\n');
      }

      return stringBuilder.toString();
   }
}
