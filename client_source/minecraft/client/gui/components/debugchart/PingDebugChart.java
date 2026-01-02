package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.debugchart.SampleStorage;

@Environment(EnvType.CLIENT)
public class PingDebugChart extends AbstractDebugChart {
   private static final int RED = -65536;
   private static final int YELLOW = -256;
   private static final int GREEN = -16711936;
   private static final int CHART_TOP_VALUE = 500;

   public PingDebugChart(Font font, SampleStorage sampleStorage) {
      super(font, sampleStorage);
   }

   protected void renderAdditionalLinesAndLabels(GuiGraphics guiGraphics, int i, int j, int k) {
      this.drawStringWithShade(guiGraphics, "500 ms", i + 1, k - 60 + 1);
   }

   protected String toDisplayString(double d) {
      return String.format(Locale.ROOT, "%d ms", (int)Math.round(d));
   }

   protected int getSampleHeight(double d) {
      return (int)Math.round(d * 60.0D / 500.0D);
   }

   protected int getSampleColor(long l) {
      return this.getSampleColor((double)l, 0.0D, -16711936, 250.0D, -256, 500.0D, -65536);
   }
}
