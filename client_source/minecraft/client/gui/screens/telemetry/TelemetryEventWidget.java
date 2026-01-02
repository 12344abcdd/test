package net.minecraft.client.gui.screens.telemetry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TelemetryEventWidget extends AbstractScrollWidget {
   private static final int HEADER_HORIZONTAL_PADDING = 32;
   private static final String TELEMETRY_REQUIRED_TRANSLATION_KEY = "telemetry.event.required";
   private static final String TELEMETRY_OPTIONAL_TRANSLATION_KEY = "telemetry.event.optional";
   private static final String TELEMETRY_OPTIONAL_DISABLED_TRANSLATION_KEY = "telemetry.event.optional.disabled";
   private static final Component PROPERTY_TITLE;
   private final Font font;
   private TelemetryEventWidget.Content content;
   @Nullable
   private DoubleConsumer onScrolledListener;

   public TelemetryEventWidget(int i, int j, int k, int l, Font font) {
      super(i, j, k, l, Component.empty());
      this.font = font;
      this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
   }

   public void onOptInChanged(boolean bl) {
      this.content = this.buildContent(bl);
      this.setScrollAmount(this.scrollAmount());
   }

   public void updateLayout() {
      this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
      this.setScrollAmount(this.scrollAmount());
   }

   private TelemetryEventWidget.Content buildContent(boolean bl) {
      TelemetryEventWidget.ContentBuilder contentBuilder = new TelemetryEventWidget.ContentBuilder(this.containerWidth());
      List<TelemetryEventType> list = new ArrayList(TelemetryEventType.values());
      list.sort(Comparator.comparing(TelemetryEventType::isOptIn));

      for(int i = 0; i < list.size(); ++i) {
         TelemetryEventType telemetryEventType = (TelemetryEventType)list.get(i);
         boolean bl2 = telemetryEventType.isOptIn() && !bl;
         this.addEventType(contentBuilder, telemetryEventType, bl2);
         if (i < list.size() - 1) {
            Objects.requireNonNull(this.font);
            contentBuilder.addSpacer(9);
         }
      }

      return contentBuilder.build();
   }

   public void setOnScrolledListener(@Nullable DoubleConsumer doubleConsumer) {
      this.onScrolledListener = doubleConsumer;
   }

   protected void setScrollAmount(double d) {
      super.setScrollAmount(d);
      if (this.onScrolledListener != null) {
         this.onScrolledListener.accept(this.scrollAmount());
      }

   }

   protected int getInnerHeight() {
      return this.content.container().getHeight();
   }

   protected double scrollRate() {
      Objects.requireNonNull(this.font);
      return 9.0D;
   }

   protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
      int k = this.getY() + this.innerPadding();
      int l = this.getX() + this.innerPadding();
      guiGraphics.pose().pushPose();
      guiGraphics.pose().translate((double)l, (double)k, 0.0D);
      this.content.container().visitWidgets((abstractWidget) -> {
         abstractWidget.render(guiGraphics, i, j, f);
      });
      guiGraphics.pose().popPose();
   }

   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      narrationElementOutput.add(NarratedElementType.TITLE, this.content.narration());
   }

   private Component grayOutIfDisabled(Component component, boolean bl) {
      return (Component)(bl ? component.copy().withStyle(ChatFormatting.GRAY) : component);
   }

   private void addEventType(TelemetryEventWidget.ContentBuilder contentBuilder, TelemetryEventType telemetryEventType, boolean bl) {
      String string = telemetryEventType.isOptIn() ? (bl ? "telemetry.event.optional.disabled" : "telemetry.event.optional") : "telemetry.event.required";
      contentBuilder.addHeader(this.font, this.grayOutIfDisabled(Component.translatable(string, new Object[]{telemetryEventType.title()}), bl));
      contentBuilder.addHeader(this.font, telemetryEventType.description().withStyle(ChatFormatting.GRAY));
      Objects.requireNonNull(this.font);
      contentBuilder.addSpacer(9 / 2);
      contentBuilder.addLine(this.font, this.grayOutIfDisabled(PROPERTY_TITLE, bl), 2);
      this.addEventTypeProperties(telemetryEventType, contentBuilder, bl);
   }

   private void addEventTypeProperties(TelemetryEventType telemetryEventType, TelemetryEventWidget.ContentBuilder contentBuilder, boolean bl) {
      Iterator var4 = telemetryEventType.properties().iterator();

      while(var4.hasNext()) {
         TelemetryProperty<?> telemetryProperty = (TelemetryProperty)var4.next();
         contentBuilder.addLine(this.font, this.grayOutIfDisabled(telemetryProperty.title(), bl));
      }

   }

   private int containerWidth() {
      return this.width - this.totalInnerPadding();
   }

   static {
      PROPERTY_TITLE = Component.translatable("telemetry_info.property_title").withStyle(ChatFormatting.UNDERLINE);
   }

   @Environment(EnvType.CLIENT)
   private static record Content(Layout container, Component narration) {
      Content(Layout layout, Component component) {
         this.container = layout;
         this.narration = component;
      }

      public Layout container() {
         return this.container;
      }

      public Component narration() {
         return this.narration;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class ContentBuilder {
      private final int width;
      private final LinearLayout layout;
      private final MutableComponent narration = Component.empty();

      public ContentBuilder(int i) {
         this.width = i;
         this.layout = LinearLayout.vertical();
         this.layout.defaultCellSetting().alignHorizontallyLeft();
         this.layout.addChild(SpacerElement.width(i));
      }

      public void addLine(Font font, Component component) {
         this.addLine(font, component, 0);
      }

      public void addLine(Font font, Component component, int i) {
         this.layout.addChild((new MultiLineTextWidget(component, font)).setMaxWidth(this.width), (Consumer)((layoutSettings) -> {
            layoutSettings.paddingBottom(i);
         }));
         this.narration.append(component).append("\n");
      }

      public void addHeader(Font font, Component component) {
         this.layout.addChild((new MultiLineTextWidget(component, font)).setMaxWidth(this.width - 64).setCentered(true), (Consumer)((layoutSettings) -> {
            layoutSettings.alignHorizontallyCenter().paddingHorizontal(32);
         }));
         this.narration.append(component).append("\n");
      }

      public void addSpacer(int i) {
         this.layout.addChild(SpacerElement.height(i));
      }

      public TelemetryEventWidget.Content build() {
         this.layout.arrangeElements();
         return new TelemetryEventWidget.Content(this.layout, this.narration);
      }
   }
}
