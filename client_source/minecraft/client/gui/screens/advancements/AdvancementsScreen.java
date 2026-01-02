package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class AdvancementsScreen extends Screen implements ClientAdvancements.Listener {
   private static final ResourceLocation WINDOW_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/advancements/window.png");
   public static final int WINDOW_WIDTH = 252;
   public static final int WINDOW_HEIGHT = 140;
   private static final int WINDOW_INSIDE_X = 9;
   private static final int WINDOW_INSIDE_Y = 18;
   public static final int WINDOW_INSIDE_WIDTH = 234;
   public static final int WINDOW_INSIDE_HEIGHT = 113;
   private static final int WINDOW_TITLE_X = 8;
   private static final int WINDOW_TITLE_Y = 6;
   public static final int BACKGROUND_TILE_WIDTH = 16;
   public static final int BACKGROUND_TILE_HEIGHT = 16;
   public static final int BACKGROUND_TILE_COUNT_X = 14;
   public static final int BACKGROUND_TILE_COUNT_Y = 7;
   private static final double SCROLL_SPEED = 16.0D;
   private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
   private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
   private static final Component TITLE = Component.translatable("gui.advancements");
   private final HeaderAndFooterLayout layout;
   @Nullable
   private final Screen lastScreen;
   private final ClientAdvancements advancements;
   private final Map<AdvancementHolder, AdvancementTab> tabs;
   @Nullable
   private AdvancementTab selectedTab;
   private boolean isScrolling;

   public AdvancementsScreen(ClientAdvancements clientAdvancements) {
      this(clientAdvancements, (Screen)null);
   }

   public AdvancementsScreen(ClientAdvancements clientAdvancements, @Nullable Screen screen) {
      super(TITLE);
      this.layout = new HeaderAndFooterLayout(this);
      this.tabs = Maps.newLinkedHashMap();
      this.advancements = clientAdvancements;
      this.lastScreen = screen;
   }

   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.tabs.clear();
      this.selectedTab = null;
      this.advancements.setListener(this);
      if (this.selectedTab == null && !this.tabs.isEmpty()) {
         AdvancementTab advancementTab = (AdvancementTab)this.tabs.values().iterator().next();
         this.advancements.setSelectedTab(advancementTab.getRootNode().holder(), true);
      } else {
         this.advancements.setSelectedTab(this.selectedTab == null ? null : this.selectedTab.getRootNode().holder(), true);
      }

      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, (button) -> {
         this.onClose();
      }).width(200).build());
      this.layout.visitWidgets((guiEventListener) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
      });
      this.repositionElements();
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public void removed() {
      this.advancements.setListener((ClientAdvancements.Listener)null);
      ClientPacketListener clientPacketListener = this.minecraft.getConnection();
      if (clientPacketListener != null) {
         clientPacketListener.send(ServerboundSeenAdvancementsPacket.closedScreen());
      }

   }

   public boolean mouseClicked(double d, double e, int i) {
      if (i == 0) {
         int j = (this.width - 252) / 2;
         int k = (this.height - 140) / 2;
         Iterator var8 = this.tabs.values().iterator();

         while(var8.hasNext()) {
            AdvancementTab advancementTab = (AdvancementTab)var8.next();
            if (advancementTab.isMouseOver(j, k, d, e)) {
               this.advancements.setSelectedTab(advancementTab.getRootNode().holder(), true);
               break;
            }
         }
      }

      return super.mouseClicked(d, e, i);
   }

   public boolean keyPressed(int i, int j, int k) {
      if (this.minecraft.options.keyAdvancements.matches(i, j)) {
         this.minecraft.setScreen((Screen)null);
         this.minecraft.mouseHandler.grabMouse();
         return true;
      } else {
         return super.keyPressed(i, j, k);
      }
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      int k = (this.width - 252) / 2;
      int l = (this.height - 140) / 2;
      this.renderInside(guiGraphics, i, j, k, l);
      this.renderWindow(guiGraphics, k, l);
      this.renderTooltips(guiGraphics, i, j, k, l);
   }

   public boolean mouseDragged(double d, double e, int i, double f, double g) {
      if (i != 0) {
         this.isScrolling = false;
         return false;
      } else {
         if (!this.isScrolling) {
            this.isScrolling = true;
         } else if (this.selectedTab != null) {
            this.selectedTab.scroll(f, g);
         }

         return true;
      }
   }

   public boolean mouseScrolled(double d, double e, double f, double g) {
      if (this.selectedTab != null) {
         this.selectedTab.scroll(f * 16.0D, g * 16.0D);
         return true;
      } else {
         return false;
      }
   }

   private void renderInside(GuiGraphics guiGraphics, int i, int j, int k, int l) {
      AdvancementTab advancementTab = this.selectedTab;
      if (advancementTab == null) {
         guiGraphics.fill(k + 9, l + 18, k + 9 + 234, l + 18 + 113, -16777216);
         int m = k + 9 + 117;
         Font var10001 = this.font;
         Component var10002 = NO_ADVANCEMENTS_LABEL;
         int var10004 = l + 18 + 56;
         Objects.requireNonNull(this.font);
         guiGraphics.drawCenteredString(var10001, (Component)var10002, m, var10004 - 9 / 2, -1);
         var10001 = this.font;
         var10002 = VERY_SAD_LABEL;
         var10004 = l + 18 + 113;
         Objects.requireNonNull(this.font);
         guiGraphics.drawCenteredString(var10001, (Component)var10002, m, var10004 - 9, -1);
      } else {
         advancementTab.drawContents(guiGraphics, k + 9, l + 18);
      }
   }

   public void renderWindow(GuiGraphics guiGraphics, int i, int j) {
      RenderSystem.enableBlend();
      guiGraphics.blit(WINDOW_LOCATION, i, j, 0, 0, 252, 140);
      if (this.tabs.size() > 1) {
         Iterator var4 = this.tabs.values().iterator();

         AdvancementTab advancementTab;
         while(var4.hasNext()) {
            advancementTab = (AdvancementTab)var4.next();
            advancementTab.drawTab(guiGraphics, i, j, advancementTab == this.selectedTab);
         }

         var4 = this.tabs.values().iterator();

         while(var4.hasNext()) {
            advancementTab = (AdvancementTab)var4.next();
            advancementTab.drawIcon(guiGraphics, i, j);
         }
      }

      guiGraphics.drawString(this.font, this.selectedTab != null ? this.selectedTab.getTitle() : TITLE, i + 8, j + 6, 4210752, false);
   }

   private void renderTooltips(GuiGraphics guiGraphics, int i, int j, int k, int l) {
      if (this.selectedTab != null) {
         guiGraphics.pose().pushPose();
         guiGraphics.pose().translate((float)(k + 9), (float)(l + 18), 400.0F);
         RenderSystem.enableDepthTest();
         this.selectedTab.drawTooltips(guiGraphics, i - k - 9, j - l - 18, k, l);
         RenderSystem.disableDepthTest();
         guiGraphics.pose().popPose();
      }

      if (this.tabs.size() > 1) {
         Iterator var6 = this.tabs.values().iterator();

         while(var6.hasNext()) {
            AdvancementTab advancementTab = (AdvancementTab)var6.next();
            if (advancementTab.isMouseOver(k, l, (double)i, (double)j)) {
               guiGraphics.renderTooltip(this.font, advancementTab.getTitle(), i, j);
            }
         }
      }

   }

   public void onAddAdvancementRoot(AdvancementNode advancementNode) {
      AdvancementTab advancementTab = AdvancementTab.create(this.minecraft, this, this.tabs.size(), advancementNode);
      if (advancementTab != null) {
         this.tabs.put(advancementNode.holder(), advancementTab);
      }
   }

   public void onRemoveAdvancementRoot(AdvancementNode advancementNode) {
   }

   public void onAddAdvancementTask(AdvancementNode advancementNode) {
      AdvancementTab advancementTab = this.getTab(advancementNode);
      if (advancementTab != null) {
         advancementTab.addAdvancement(advancementNode);
      }

   }

   public void onRemoveAdvancementTask(AdvancementNode advancementNode) {
   }

   public void onUpdateAdvancementProgress(AdvancementNode advancementNode, AdvancementProgress advancementProgress) {
      AdvancementWidget advancementWidget = this.getAdvancementWidget(advancementNode);
      if (advancementWidget != null) {
         advancementWidget.setProgress(advancementProgress);
      }

   }

   public void onSelectedTabChanged(@Nullable AdvancementHolder advancementHolder) {
      this.selectedTab = (AdvancementTab)this.tabs.get(advancementHolder);
   }

   public void onAdvancementsCleared() {
      this.tabs.clear();
      this.selectedTab = null;
   }

   @Nullable
   public AdvancementWidget getAdvancementWidget(AdvancementNode advancementNode) {
      AdvancementTab advancementTab = this.getTab(advancementNode);
      return advancementTab == null ? null : advancementTab.getWidget(advancementNode.holder());
   }

   @Nullable
   private AdvancementTab getTab(AdvancementNode advancementNode) {
      AdvancementNode advancementNode2 = advancementNode.root();
      return (AdvancementTab)this.tabs.get(advancementNode2.holder());
   }
}
