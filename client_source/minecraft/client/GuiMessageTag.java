package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record GuiMessageTag(int indicatorColor, @Nullable GuiMessageTag.Icon icon, @Nullable Component text, @Nullable String logTag) {
   private static final Component SYSTEM_TEXT = Component.translatable("chat.tag.system");
   private static final Component SYSTEM_TEXT_SINGLE_PLAYER = Component.translatable("chat.tag.system_single_player");
   private static final Component CHAT_NOT_SECURE_TEXT = Component.translatable("chat.tag.not_secure");
   private static final Component CHAT_MODIFIED_TEXT = Component.translatable("chat.tag.modified");
   private static final Component CHAT_ERROR_TEXT = Component.translatable("chat.tag.error");
   private static final int CHAT_NOT_SECURE_INDICATOR_COLOR = 13684944;
   private static final int CHAT_MODIFIED_INDICATOR_COLOR = 6316128;
   private static final GuiMessageTag SYSTEM;
   private static final GuiMessageTag SYSTEM_SINGLE_PLAYER;
   private static final GuiMessageTag CHAT_NOT_SECURE;
   private static final GuiMessageTag CHAT_ERROR;

   public GuiMessageTag(int i, @Nullable GuiMessageTag.Icon icon, @Nullable Component component, @Nullable String string) {
      this.indicatorColor = i;
      this.icon = icon;
      this.text = component;
      this.logTag = string;
   }

   public static GuiMessageTag system() {
      return SYSTEM;
   }

   public static GuiMessageTag systemSinglePlayer() {
      return SYSTEM_SINGLE_PLAYER;
   }

   public static GuiMessageTag chatNotSecure() {
      return CHAT_NOT_SECURE;
   }

   public static GuiMessageTag chatModified(String string) {
      Component component = Component.literal(string).withStyle(ChatFormatting.GRAY);
      Component component2 = Component.empty().append(CHAT_MODIFIED_TEXT).append(CommonComponents.NEW_LINE).append(component);
      return new GuiMessageTag(6316128, GuiMessageTag.Icon.CHAT_MODIFIED, component2, "Modified");
   }

   public static GuiMessageTag chatError() {
      return CHAT_ERROR;
   }

   public int indicatorColor() {
      return this.indicatorColor;
   }

   @Nullable
   public GuiMessageTag.Icon icon() {
      return this.icon;
   }

   @Nullable
   public Component text() {
      return this.text;
   }

   @Nullable
   public String logTag() {
      return this.logTag;
   }

   static {
      SYSTEM = new GuiMessageTag(13684944, (GuiMessageTag.Icon)null, SYSTEM_TEXT, "System");
      SYSTEM_SINGLE_PLAYER = new GuiMessageTag(13684944, (GuiMessageTag.Icon)null, SYSTEM_TEXT_SINGLE_PLAYER, "System");
      CHAT_NOT_SECURE = new GuiMessageTag(13684944, (GuiMessageTag.Icon)null, CHAT_NOT_SECURE_TEXT, "Not Secure");
      CHAT_ERROR = new GuiMessageTag(16733525, (GuiMessageTag.Icon)null, CHAT_ERROR_TEXT, "Chat Error");
   }

   @Environment(EnvType.CLIENT)
   public static enum Icon {
      CHAT_MODIFIED(ResourceLocation.withDefaultNamespace("icon/chat_modified"), 9, 9);

      public final ResourceLocation sprite;
      public final int width;
      public final int height;

      private Icon(final ResourceLocation resourceLocation, final int j, final int k) {
         this.sprite = resourceLocation;
         this.width = j;
         this.height = k;
      }

      public void draw(GuiGraphics guiGraphics, int i, int j) {
         guiGraphics.blitSprite(this.sprite, i, j, this.width, this.height);
      }

      // $FF: synthetic method
      private static GuiMessageTag.Icon[] $values() {
         return new GuiMessageTag.Icon[]{CHAT_MODIFIED};
      }
   }
}
