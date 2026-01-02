package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class OptimizeWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ToIntFunction<ResourceKey<Level>> DIMENSION_COLORS = (ToIntFunction)Util.make(new Reference2IntOpenHashMap(), (reference2IntOpenHashMap) -> {
      reference2IntOpenHashMap.put(Level.OVERWORLD, -13408734);
      reference2IntOpenHashMap.put(Level.NETHER, -10075085);
      reference2IntOpenHashMap.put(Level.END, -8943531);
      reference2IntOpenHashMap.defaultReturnValue(-2236963);
   });
   private final BooleanConsumer callback;
   private final WorldUpgrader upgrader;

   @Nullable
   public static OptimizeWorldScreen create(Minecraft minecraft, BooleanConsumer booleanConsumer, DataFixer dataFixer, LevelStorageAccess levelStorageAccess, boolean bl) {
      try {
         WorldOpenFlows worldOpenFlows = minecraft.createWorldOpenFlows();
         PackRepository packRepository = ServerPacksSource.createPackRepository(levelStorageAccess);
         WorldStem worldStem = worldOpenFlows.loadWorldStem(levelStorageAccess.getDataTag(), false, packRepository);

         OptimizeWorldScreen var10;
         try {
            WorldData worldData = worldStem.worldData();
            Frozen frozen = worldStem.registries().compositeAccess();
            levelStorageAccess.saveDataTag(frozen, worldData);
            var10 = new OptimizeWorldScreen(booleanConsumer, dataFixer, levelStorageAccess, worldData.getLevelSettings(), bl, frozen);
         } catch (Throwable var12) {
            if (worldStem != null) {
               try {
                  worldStem.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (worldStem != null) {
            worldStem.close();
         }

         return var10;
      } catch (Exception var13) {
         LOGGER.warn("Failed to load datapacks, can't optimize world", var13);
         return null;
      }
   }

   private OptimizeWorldScreen(BooleanConsumer booleanConsumer, DataFixer dataFixer, LevelStorageAccess levelStorageAccess, LevelSettings levelSettings, boolean bl, RegistryAccess registryAccess) {
      super(Component.translatable("optimizeWorld.title", new Object[]{levelSettings.levelName()}));
      this.callback = booleanConsumer;
      this.upgrader = new WorldUpgrader(levelStorageAccess, dataFixer, registryAccess, bl, false);
   }

   protected void init() {
      super.init();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
         this.upgrader.cancel();
         this.callback.accept(false);
      }).bounds(this.width / 2 - 100, this.height / 4 + 150, 200, 20).build());
   }

   public void tick() {
      if (this.upgrader.isFinished()) {
         this.callback.accept(true);
      }

   }

   public void onClose() {
      this.callback.accept(false);
   }

   public void removed() {
      this.upgrader.cancel();
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      guiGraphics.drawCenteredString(this.font, (Component)this.title, this.width / 2, 20, 16777215);
      int k = this.width / 2 - 150;
      int l = this.width / 2 + 150;
      int m = this.height / 4 + 100;
      int n = m + 10;
      Font var10001 = this.font;
      Component var10002 = this.upgrader.getStatus();
      int var10003 = this.width / 2;
      Objects.requireNonNull(this.font);
      guiGraphics.drawCenteredString(var10001, var10002, var10003, m - 9 - 2, 10526880);
      if (this.upgrader.getTotalChunks() > 0) {
         guiGraphics.fill(k - 1, m - 1, l + 1, n + 1, -16777216);
         guiGraphics.drawString(this.font, (Component)Component.translatable("optimizeWorld.info.converted", new Object[]{this.upgrader.getConverted()}), k, 40, 10526880);
         var10001 = this.font;
         MutableComponent var16 = Component.translatable("optimizeWorld.info.skipped", new Object[]{this.upgrader.getSkipped()});
         Objects.requireNonNull(this.font);
         guiGraphics.drawString(var10001, (Component)var16, k, 40 + 9 + 3, 10526880);
         var10001 = this.font;
         var16 = Component.translatable("optimizeWorld.info.total", new Object[]{this.upgrader.getTotalChunks()});
         Objects.requireNonNull(this.font);
         guiGraphics.drawString(var10001, (Component)var16, k, 40 + (9 + 3) * 2, 10526880);
         int o = 0;

         int p;
         for(Iterator var10 = this.upgrader.levels().iterator(); var10.hasNext(); o += p) {
            ResourceKey<Level> resourceKey = (ResourceKey)var10.next();
            p = Mth.floor(this.upgrader.dimensionProgress(resourceKey) * (float)(l - k));
            guiGraphics.fill(k + o, m, k + o + p, n, DIMENSION_COLORS.applyAsInt(resourceKey));
         }

         int q = this.upgrader.getConverted() + this.upgrader.getSkipped();
         Component component = Component.translatable("optimizeWorld.progress.counter", new Object[]{q, this.upgrader.getTotalChunks()});
         Component component2 = Component.translatable("optimizeWorld.progress.percentage", new Object[]{Mth.floor(this.upgrader.getProgress() * 100.0F)});
         var10001 = this.font;
         var10003 = this.width / 2;
         Objects.requireNonNull(this.font);
         guiGraphics.drawCenteredString(var10001, (Component)component, var10003, m + 2 * 9 + 2, 10526880);
         var10001 = this.font;
         var10003 = this.width / 2;
         int var10004 = m + (n - m) / 2;
         Objects.requireNonNull(this.font);
         guiGraphics.drawCenteredString(var10001, (Component)component2, var10003, var10004 - 9 / 2, 10526880);
      }

   }
}
