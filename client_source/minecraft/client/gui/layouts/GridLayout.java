package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class GridLayout extends AbstractLayout {
   private final List<LayoutElement> children;
   private final List<GridLayout.CellInhabitant> cellInhabitants;
   private final LayoutSettings defaultCellSettings;
   private int rowSpacing;
   private int columnSpacing;

   public GridLayout() {
      this(0, 0);
   }

   public GridLayout(int i, int j) {
      super(i, j, 0, 0);
      this.children = new ArrayList();
      this.cellInhabitants = new ArrayList();
      this.defaultCellSettings = LayoutSettings.defaults();
      this.rowSpacing = 0;
      this.columnSpacing = 0;
   }

   public void arrangeElements() {
      super.arrangeElements();
      int i = 0;
      int j = 0;

      GridLayout.CellInhabitant cellInhabitant;
      for(Iterator var3 = this.cellInhabitants.iterator(); var3.hasNext(); j = Math.max(cellInhabitant.getLastOccupiedColumn(), j)) {
         cellInhabitant = (GridLayout.CellInhabitant)var3.next();
         i = Math.max(cellInhabitant.getLastOccupiedRow(), i);
      }

      int[] is = new int[j + 1];
      int[] js = new int[i + 1];
      Iterator var5 = this.cellInhabitants.iterator();

      int k;
      int l;
      int m;
      while(var5.hasNext()) {
         GridLayout.CellInhabitant cellInhabitant2 = (GridLayout.CellInhabitant)var5.next();
         k = cellInhabitant2.getHeight() - (cellInhabitant2.occupiedRows - 1) * this.rowSpacing;
         Divisor divisor = new Divisor(k, cellInhabitant2.occupiedRows);

         for(l = cellInhabitant2.row; l <= cellInhabitant2.getLastOccupiedRow(); ++l) {
            js[l] = Math.max(js[l], divisor.nextInt());
         }

         l = cellInhabitant2.getWidth() - (cellInhabitant2.occupiedColumns - 1) * this.columnSpacing;
         Divisor divisor2 = new Divisor(l, cellInhabitant2.occupiedColumns);

         for(m = cellInhabitant2.column; m <= cellInhabitant2.getLastOccupiedColumn(); ++m) {
            is[m] = Math.max(is[m], divisor2.nextInt());
         }
      }

      int[] ks = new int[j + 1];
      int[] ls = new int[i + 1];
      ks[0] = 0;

      for(k = 1; k <= j; ++k) {
         ks[k] = ks[k - 1] + is[k - 1] + this.columnSpacing;
      }

      ls[0] = 0;

      for(k = 1; k <= i; ++k) {
         ls[k] = ls[k - 1] + js[k - 1] + this.rowSpacing;
      }

      Iterator var17 = this.cellInhabitants.iterator();

      while(var17.hasNext()) {
         GridLayout.CellInhabitant cellInhabitant3 = (GridLayout.CellInhabitant)var17.next();
         l = 0;

         int n;
         for(n = cellInhabitant3.column; n <= cellInhabitant3.getLastOccupiedColumn(); ++n) {
            l += is[n];
         }

         l += this.columnSpacing * (cellInhabitant3.occupiedColumns - 1);
         cellInhabitant3.setX(this.getX() + ks[cellInhabitant3.column], l);
         n = 0;

         for(m = cellInhabitant3.row; m <= cellInhabitant3.getLastOccupiedRow(); ++m) {
            n += js[m];
         }

         n += this.rowSpacing * (cellInhabitant3.occupiedRows - 1);
         cellInhabitant3.setY(this.getY() + ls[cellInhabitant3.row], n);
      }

      this.width = ks[j] + is[j];
      this.height = ls[i] + js[i];
   }

   public <T extends LayoutElement> T addChild(T layoutElement, int i, int j) {
      return this.addChild(layoutElement, i, j, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T layoutElement, int i, int j, LayoutSettings layoutSettings) {
      return this.addChild(layoutElement, i, j, 1, 1, (LayoutSettings)layoutSettings);
   }

   public <T extends LayoutElement> T addChild(T layoutElement, int i, int j, Consumer<LayoutSettings> consumer) {
      return this.addChild(layoutElement, i, j, 1, 1, (LayoutSettings)((LayoutSettings)Util.make(this.newCellSettings(), consumer)));
   }

   public <T extends LayoutElement> T addChild(T layoutElement, int i, int j, int k, int l) {
      return this.addChild(layoutElement, i, j, k, l, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T layoutElement, int i, int j, int k, int l, LayoutSettings layoutSettings) {
      if (k < 1) {
         throw new IllegalArgumentException("Occupied rows must be at least 1");
      } else if (l < 1) {
         throw new IllegalArgumentException("Occupied columns must be at least 1");
      } else {
         this.cellInhabitants.add(new GridLayout.CellInhabitant(layoutElement, i, j, k, l, layoutSettings));
         this.children.add(layoutElement);
         return layoutElement;
      }
   }

   public <T extends LayoutElement> T addChild(T layoutElement, int i, int j, int k, int l, Consumer<LayoutSettings> consumer) {
      return this.addChild(layoutElement, i, j, k, l, (LayoutSettings)Util.make(this.newCellSettings(), consumer));
   }

   public GridLayout columnSpacing(int i) {
      this.columnSpacing = i;
      return this;
   }

   public GridLayout rowSpacing(int i) {
      this.rowSpacing = i;
      return this;
   }

   public GridLayout spacing(int i) {
      return this.columnSpacing(i).rowSpacing(i);
   }

   public void visitChildren(Consumer<LayoutElement> consumer) {
      this.children.forEach(consumer);
   }

   public LayoutSettings newCellSettings() {
      return this.defaultCellSettings.copy();
   }

   public LayoutSettings defaultCellSetting() {
      return this.defaultCellSettings;
   }

   public GridLayout.RowHelper createRowHelper(int i) {
      return new GridLayout.RowHelper(i);
   }

   @Environment(EnvType.CLIENT)
   private static class CellInhabitant extends AbstractLayout.AbstractChildWrapper {
      final int row;
      final int column;
      final int occupiedRows;
      final int occupiedColumns;

      CellInhabitant(LayoutElement layoutElement, int i, int j, int k, int l, LayoutSettings layoutSettings) {
         super(layoutElement, layoutSettings.getExposed());
         this.row = i;
         this.column = j;
         this.occupiedRows = k;
         this.occupiedColumns = l;
      }

      public int getLastOccupiedRow() {
         return this.row + this.occupiedRows - 1;
      }

      public int getLastOccupiedColumn() {
         return this.column + this.occupiedColumns - 1;
      }
   }

   @Environment(EnvType.CLIENT)
   public final class RowHelper {
      private final int columns;
      private int index;

      RowHelper(final int i) {
         this.columns = i;
      }

      public <T extends LayoutElement> T addChild(T layoutElement) {
         return this.addChild(layoutElement, 1);
      }

      public <T extends LayoutElement> T addChild(T layoutElement, int i) {
         return this.addChild(layoutElement, i, this.defaultCellSetting());
      }

      public <T extends LayoutElement> T addChild(T layoutElement, LayoutSettings layoutSettings) {
         return this.addChild(layoutElement, 1, layoutSettings);
      }

      public <T extends LayoutElement> T addChild(T layoutElement, int i, LayoutSettings layoutSettings) {
         int j = this.index / this.columns;
         int k = this.index % this.columns;
         if (k + i > this.columns) {
            ++j;
            k = 0;
            this.index = Mth.roundToward(this.index, this.columns);
         }

         this.index += i;
         return GridLayout.this.addChild(layoutElement, j, k, 1, i, (LayoutSettings)layoutSettings);
      }

      public GridLayout getGrid() {
         return GridLayout.this;
      }

      public LayoutSettings newCellSettings() {
         return GridLayout.this.newCellSettings();
      }

      public LayoutSettings defaultCellSetting() {
         return GridLayout.this.defaultCellSetting();
      }
   }
}
