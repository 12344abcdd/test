package net.minecraft.client.gui.screens.multiplayer;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.ServerLinks.Entry;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ServerLinksScreen extends Screen {
   private static final int LINK_BUTTON_WIDTH = 310;
   private static final int DEFAULT_ITEM_HEIGHT = 25;
   private static final Component TITLE = Component.translatable("menu.server_links.title");
   private final Screen lastScreen;
   @Nullable
   private ServerLinksScreen.LinkList list;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   final ServerLinks links;

   public ServerLinksScreen(Screen screen, ServerLinks serverLinks) {
      super(TITLE);
      this.lastScreen = screen;
      this.links = serverLinks;
   }

   protected void init() {
      this.layout.addTitleHeader(this.title, this.font);
      this.list = (ServerLinksScreen.LinkList)this.layout.addToContents(new ServerLinksScreen.LinkList(this.minecraft, this.width, this));
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, (button) -> {
         this.onClose();
      }).width(200).build());
      this.layout.visitWidgets((guiEventListener) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
      });
      this.repositionElements();
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.list != null) {
         this.list.updateSize(this.width, this.layout);
      }

   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Environment(EnvType.CLIENT)
   private static class LinkList extends ContainerObjectSelectionList<ServerLinksScreen.LinkListEntry> {
      public LinkList(Minecraft minecraft, int i, ServerLinksScreen serverLinksScreen) {
         super(minecraft, i, serverLinksScreen.layout.getContentHeight(), serverLinksScreen.layout.getHeaderHeight(), 25);
         serverLinksScreen.links.entries().forEach((entry) -> {
            this.addEntry(new ServerLinksScreen.LinkListEntry(serverLinksScreen, entry));
         });
      }

      public int getRowWidth() {
         return 310;
      }

      public void updateSize(int i, HeaderAndFooterLayout headerAndFooterLayout) {
         super.updateSize(i, headerAndFooterLayout);
         int j = i / 2 - 155;
         this.children().forEach((linkListEntry) -> {
            linkListEntry.button.setX(j);
         });
      }
   }

   @Environment(EnvType.CLIENT)
   private static class LinkListEntry extends ContainerObjectSelectionList.Entry<ServerLinksScreen.LinkListEntry> {
      final AbstractWidget button;

      LinkListEntry(Screen screen, Entry entry) {
         this.button = Button.builder(entry.displayName(), ConfirmLinkScreen.confirmLink(screen, entry.link(), false)).width(310).build();
      }

      public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
         this.button.setY(j);
         this.button.render(guiGraphics, n, o, f);
      }

      public List<? extends GuiEventListener> children() {
         return List.of(this.button);
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of(this.button);
      }
   }
}
