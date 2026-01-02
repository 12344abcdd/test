package net.minecraft.client.gui.components;

import java.util.Objects;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.SingleKeyCache;

@Environment(EnvType.CLIENT)
public class MultiLineTextWidget extends AbstractStringWidget {
   private OptionalInt maxWidth;
   private OptionalInt maxRows;
   private final SingleKeyCache<MultiLineTextWidget.CacheKey, MultiLineLabel> cache;
   private boolean centered;

   public MultiLineTextWidget(Component component, Font font) {
      this(0, 0, component, font);
   }

   public MultiLineTextWidget(int i, int j, Component component, Font font) {
      super(i, j, 0, 0, component, font);
      this.maxWidth = OptionalInt.empty();
      this.maxRows = OptionalInt.empty();
      this.centered = false;
      this.cache = Util.singleKeyCache((cacheKey) -> {
         return cacheKey.maxRows.isPresent() ? MultiLineLabel.create(font, cacheKey.maxWidth, cacheKey.maxRows.getAsInt(), cacheKey.message) : MultiLineLabel.create(font, cacheKey.message, cacheKey.maxWidth);
      });
      this.active = false;
   }

   public MultiLineTextWidget setColor(int i) {
      super.setColor(i);
      return this;
   }

   public MultiLineTextWidget setMaxWidth(int i) {
      this.maxWidth = OptionalInt.of(i);
      return this;
   }

   public MultiLineTextWidget setMaxRows(int i) {
      this.maxRows = OptionalInt.of(i);
      return this;
   }

   public MultiLineTextWidget setCentered(boolean bl) {
      this.centered = bl;
      return this;
   }

   public int getWidth() {
      return ((MultiLineLabel)this.cache.getValue(this.getFreshCacheKey())).getWidth();
   }

   public int getHeight() {
      int var10000 = ((MultiLineLabel)this.cache.getValue(this.getFreshCacheKey())).getLineCount();
      Objects.requireNonNull(this.getFont());
      return var10000 * 9;
   }

   public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
      MultiLineLabel multiLineLabel = (MultiLineLabel)this.cache.getValue(this.getFreshCacheKey());
      int k = this.getX();
      int l = this.getY();
      Objects.requireNonNull(this.getFont());
      int m = 9;
      int n = this.getColor();
      if (this.centered) {
         multiLineLabel.renderCentered(guiGraphics, k + this.getWidth() / 2, l, m, n);
      } else {
         multiLineLabel.renderLeftAligned(guiGraphics, k, l, m, n);
      }

   }

   private MultiLineTextWidget.CacheKey getFreshCacheKey() {
      return new MultiLineTextWidget.CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
   }

   // $FF: synthetic method
   public AbstractStringWidget setColor(final int i) {
      return this.setColor(i);
   }

   @Environment(EnvType.CLIENT)
   private static record CacheKey(Component message, int maxWidth, OptionalInt maxRows) {
      final Component message;
      final int maxWidth;
      final OptionalInt maxRows;

      CacheKey(Component component, int i, OptionalInt optionalInt) {
         this.message = component;
         this.maxWidth = i;
         this.maxRows = optionalInt;
      }

      public Component message() {
         return this.message;
      }

      public int maxWidth() {
         return this.maxWidth;
      }

      public OptionalInt maxRows() {
         return this.maxRows;
      }
   }
}
