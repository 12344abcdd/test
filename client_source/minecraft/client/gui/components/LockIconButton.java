package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class LockIconButton extends Button {
   private boolean locked;

   public LockIconButton(int i, int j, Button.OnPress onPress) {
      super(i, j, 20, 20, Component.translatable("narrator.button.difficulty_lock"), onPress, DEFAULT_NARRATION);
   }

   protected MutableComponent createNarrationMessage() {
      return CommonComponents.joinForNarration(new Component[]{super.createNarrationMessage(), this.isLocked() ? Component.translatable("narrator.button.difficulty_lock.locked") : Component.translatable("narrator.button.difficulty_lock.unlocked")});
   }

   public boolean isLocked() {
      return this.locked;
   }

   public void setLocked(boolean bl) {
      this.locked = bl;
   }

   public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
      LockIconButton.Icon icon;
      if (!this.active) {
         icon = this.locked ? LockIconButton.Icon.LOCKED_DISABLED : LockIconButton.Icon.UNLOCKED_DISABLED;
      } else if (this.isHoveredOrFocused()) {
         icon = this.locked ? LockIconButton.Icon.LOCKED_HOVER : LockIconButton.Icon.UNLOCKED_HOVER;
      } else {
         icon = this.locked ? LockIconButton.Icon.LOCKED : LockIconButton.Icon.UNLOCKED;
      }

      guiGraphics.blitSprite(icon.sprite, this.getX(), this.getY(), this.width, this.height);
   }

   @Environment(EnvType.CLIENT)
   private static enum Icon {
      LOCKED(ResourceLocation.withDefaultNamespace("widget/locked_button")),
      LOCKED_HOVER(ResourceLocation.withDefaultNamespace("widget/locked_button_highlighted")),
      LOCKED_DISABLED(ResourceLocation.withDefaultNamespace("widget/locked_button_disabled")),
      UNLOCKED(ResourceLocation.withDefaultNamespace("widget/unlocked_button")),
      UNLOCKED_HOVER(ResourceLocation.withDefaultNamespace("widget/unlocked_button_highlighted")),
      UNLOCKED_DISABLED(ResourceLocation.withDefaultNamespace("widget/unlocked_button_disabled"));

      final ResourceLocation sprite;

      private Icon(final ResourceLocation resourceLocation) {
         this.sprite = resourceLocation;
      }

      // $FF: synthetic method
      private static LockIconButton.Icon[] $values() {
         return new LockIconButton.Icon[]{LOCKED, LOCKED_HOVER, LOCKED_DISABLED, UNLOCKED, UNLOCKED_HOVER, UNLOCKED_DISABLED};
      }
   }
}
