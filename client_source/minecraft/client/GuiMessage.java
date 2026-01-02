package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record GuiMessage(int addedTime, Component content, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag) {
   public GuiMessage(int i, Component component, @Nullable MessageSignature messageSignature, @Nullable GuiMessageTag guiMessageTag) {
      this.addedTime = i;
      this.content = component;
      this.signature = messageSignature;
      this.tag = guiMessageTag;
   }

   @Nullable
   public GuiMessageTag.Icon icon() {
      return this.tag != null ? this.tag.icon() : null;
   }

   public int addedTime() {
      return this.addedTime;
   }

   public Component content() {
      return this.content;
   }

   @Nullable
   public MessageSignature signature() {
      return this.signature;
   }

   @Nullable
   public GuiMessageTag tag() {
      return this.tag;
   }

   @Environment(EnvType.CLIENT)
   public static record Line(int addedTime, FormattedCharSequence content, @Nullable GuiMessageTag tag, boolean endOfEntry) {
      public Line(int i, FormattedCharSequence formattedCharSequence, @Nullable GuiMessageTag guiMessageTag, boolean bl) {
         this.addedTime = i;
         this.content = formattedCharSequence;
         this.tag = guiMessageTag;
         this.endOfEntry = bl;
      }

      public int addedTime() {
         return this.addedTime;
      }

      public FormattedCharSequence content() {
         return this.content;
      }

      @Nullable
      public GuiMessageTag tag() {
         return this.tag;
      }

      public boolean endOfEntry() {
         return this.endOfEntry;
      }
   }
}
