package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock.MinecartCommandBase;
import net.minecraft.world.level.BaseCommandBlock;

@Environment(EnvType.CLIENT)
public class MinecartCommandBlockEditScreen extends AbstractCommandBlockEditScreen {
   private final BaseCommandBlock commandBlock;

   public MinecartCommandBlockEditScreen(BaseCommandBlock baseCommandBlock) {
      this.commandBlock = baseCommandBlock;
   }

   public BaseCommandBlock getCommandBlock() {
      return this.commandBlock;
   }

   int getPreviousY() {
      return 150;
   }

   protected void init() {
      super.init();
      this.commandEdit.setValue(this.getCommandBlock().getCommand());
   }

   protected void populateAndSendPacket(BaseCommandBlock baseCommandBlock) {
      if (baseCommandBlock instanceof MinecartCommandBase) {
         MinecartCommandBase minecartCommandBase = (MinecartCommandBase)baseCommandBlock;
         this.minecraft.getConnection().send(new ServerboundSetCommandMinecartPacket(minecartCommandBase.getMinecart().getId(), this.commandEdit.getValue(), baseCommandBlock.isTrackOutput()));
      }

   }
}
