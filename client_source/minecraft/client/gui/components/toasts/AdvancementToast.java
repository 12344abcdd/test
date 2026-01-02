package net.minecraft.client.gui.components.toasts;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class AdvancementToast implements Toast {
   private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/advancement");
   public static final int DISPLAY_TIME = 5000;
   private final AdvancementHolder advancement;
   private boolean playedSound;

   public AdvancementToast(AdvancementHolder advancementHolder) {
      this.advancement = advancementHolder;
   }

   public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long l) {
      DisplayInfo displayInfo = (DisplayInfo)this.advancement.value().display().orElse((Object)null);
      guiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
      if (displayInfo != null) {
         List<FormattedCharSequence> list = toastComponent.getMinecraft().font.split(displayInfo.getTitle(), 125);
         int i = displayInfo.getType() == AdvancementType.CHALLENGE ? 16746751 : 16776960;
         if (list.size() == 1) {
            guiGraphics.drawString(toastComponent.getMinecraft().font, (Component)displayInfo.getType().getDisplayName(), 30, 7, i | -16777216, false);
            guiGraphics.drawString(toastComponent.getMinecraft().font, (FormattedCharSequence)((FormattedCharSequence)list.get(0)), 30, 18, -1, false);
         } else {
            int j = true;
            float f = 300.0F;
            int k;
            if (l < 1500L) {
               k = Mth.floor(Mth.clamp((float)(1500L - l) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
               guiGraphics.drawString(toastComponent.getMinecraft().font, (Component)displayInfo.getType().getDisplayName(), 30, 11, i | k, false);
            } else {
               k = Mth.floor(Mth.clamp((float)(l - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
               int var10000 = this.height() / 2;
               int var10001 = list.size();
               Objects.requireNonNull(toastComponent.getMinecraft().font);
               int m = var10000 - var10001 * 9 / 2;

               for(Iterator var12 = list.iterator(); var12.hasNext(); m += 9) {
                  FormattedCharSequence formattedCharSequence = (FormattedCharSequence)var12.next();
                  guiGraphics.drawString(toastComponent.getMinecraft().font, (FormattedCharSequence)formattedCharSequence, 30, m, 16777215 | k, false);
                  Objects.requireNonNull(toastComponent.getMinecraft().font);
               }
            }
         }

         if (!this.playedSound && l > 0L) {
            this.playedSound = true;
            if (displayInfo.getType() == AdvancementType.CHALLENGE) {
               toastComponent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
            }
         }

         guiGraphics.renderFakeItem(displayInfo.getIcon(), 8, 8);
         return (double)l >= 5000.0D * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
      } else {
         return Toast.Visibility.HIDE;
      }
   }
}
