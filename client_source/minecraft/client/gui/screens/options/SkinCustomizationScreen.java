package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;

@Environment(EnvType.CLIENT)
public class SkinCustomizationScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.skinCustomisation.title");

   public SkinCustomizationScreen(Screen screen, Options options) {
      super(screen, options, TITLE);
   }

   protected void addOptions() {
      List<AbstractWidget> list = new ArrayList();
      PlayerModelPart[] var2 = PlayerModelPart.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         PlayerModelPart playerModelPart = var2[var4];
         list.add(CycleButton.onOffBuilder(this.options.isModelPartEnabled(playerModelPart)).create(playerModelPart.getName(), (cycleButton, boolean_) -> {
            this.options.toggleModelPart(playerModelPart, boolean_);
         }));
      }

      list.add(this.options.mainHand().createButton(this.options));
      this.list.addSmall((List)list);
   }
}
