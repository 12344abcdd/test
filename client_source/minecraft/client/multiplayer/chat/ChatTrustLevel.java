package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import java.time.Instant;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public enum ChatTrustLevel implements StringRepresentable {
   SECURE("secure"),
   MODIFIED("modified"),
   NOT_SECURE("not_secure");

   public static final Codec<ChatTrustLevel> CODEC = StringRepresentable.fromEnum(ChatTrustLevel::values);
   private final String serializedName;

   private ChatTrustLevel(final String string2) {
      this.serializedName = string2;
   }

   public static ChatTrustLevel evaluate(PlayerChatMessage playerChatMessage, Component component, Instant instant) {
      if (playerChatMessage.hasSignature() && !playerChatMessage.hasExpiredClient(instant)) {
         return isModified(playerChatMessage, component) ? MODIFIED : SECURE;
      } else {
         return NOT_SECURE;
      }
   }

   private static boolean isModified(PlayerChatMessage playerChatMessage, Component component) {
      if (!component.getString().contains(playerChatMessage.signedContent())) {
         return true;
      } else {
         Component component2 = playerChatMessage.unsignedContent();
         return component2 == null ? false : containsModifiedStyle(component2);
      }
   }

   private static boolean containsModifiedStyle(Component component) {
      return (Boolean)component.visit((style, string) -> {
         return isModifiedStyle(style) ? Optional.of(true) : Optional.empty();
      }, Style.EMPTY).orElse(false);
   }

   private static boolean isModifiedStyle(Style style) {
      return !style.getFont().equals(Style.DEFAULT_FONT);
   }

   public boolean isNotSecure() {
      return this == NOT_SECURE;
   }

   @Nullable
   public GuiMessageTag createTag(PlayerChatMessage playerChatMessage) {
      GuiMessageTag var10000;
      switch(this.ordinal()) {
      case 1:
         var10000 = GuiMessageTag.chatModified(playerChatMessage.signedContent());
         break;
      case 2:
         var10000 = GuiMessageTag.chatNotSecure();
         break;
      default:
         var10000 = null;
      }

      return var10000;
   }

   public String getSerializedName() {
      return this.serializedName;
   }

   // $FF: synthetic method
   private static ChatTrustLevel[] $values() {
      return new ChatTrustLevel[]{SECURE, MODIFIED, NOT_SECURE};
   }
}
