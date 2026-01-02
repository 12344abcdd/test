package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class VideoSettingsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.videoTitle");
   private static final Component FABULOUS;
   private static final Component WARNING_MESSAGE;
   private static final Component WARNING_TITLE;
   private static final Component BUTTON_ACCEPT;
   private static final Component BUTTON_CANCEL;
   private final GpuWarnlistManager gpuWarnlistManager;
   private final int oldMipmaps;

   private static OptionInstance<?>[] options(Options options) {
      return new OptionInstance[]{options.graphicsMode(), options.renderDistance(), options.prioritizeChunkUpdates(), options.simulationDistance(), options.ambientOcclusion(), options.framerateLimit(), options.enableVsync(), options.bobView(), options.guiScale(), options.attackIndicator(), options.gamma(), options.cloudStatus(), options.fullscreen(), options.particles(), options.mipmapLevels(), options.entityShadows(), options.screenEffectScale(), options.entityDistanceScaling(), options.fovEffectScale(), options.showAutosaveIndicator(), options.glintSpeed(), options.glintStrength(), options.menuBackgroundBlurriness()};
   }

   public VideoSettingsScreen(Screen screen, Minecraft minecraft, Options options) {
      super(screen, options, TITLE);
      this.gpuWarnlistManager = minecraft.getGpuWarnlistManager();
      this.gpuWarnlistManager.resetWarnings();
      if (options.graphicsMode().get() == GraphicsStatus.FABULOUS) {
         this.gpuWarnlistManager.dismissWarning();
      }

      this.oldMipmaps = (Integer)options.mipmapLevels().get();
   }

   protected void addOptions() {
      int i = true;
      Window window = this.minecraft.getWindow();
      Monitor monitor = window.findBestMonitor();
      int j;
      if (monitor == null) {
         j = -1;
      } else {
         Optional<VideoMode> optional = window.getPreferredFullscreenVideoMode();
         Objects.requireNonNull(monitor);
         j = (Integer)optional.map(monitor::getVideoModeIndex).orElse(-1);
      }

      OptionInstance<Integer> optionInstance = new OptionInstance("options.fullscreen.resolution", OptionInstance.noTooltip(), (component, integer) -> {
         if (monitor == null) {
            return Component.translatable("options.fullscreen.unavailable");
         } else if (integer == -1) {
            return Options.genericValueLabel(component, Component.translatable("options.fullscreen.current"));
         } else {
            VideoMode videoMode = monitor.getMode(integer);
            return Options.genericValueLabel(component, Component.translatable("options.fullscreen.entry", new Object[]{videoMode.getWidth(), videoMode.getHeight(), videoMode.getRefreshRate(), videoMode.getRedBits() + videoMode.getGreenBits() + videoMode.getBlueBits()}));
         }
      }, new OptionInstance.IntRange(-1, monitor != null ? monitor.getModeCount() - 1 : -1), j, (integer) -> {
         if (monitor != null) {
            window.setPreferredFullscreenVideoMode(integer == -1 ? Optional.empty() : Optional.of(monitor.getMode(integer)));
         }
      });
      this.list.addBig(optionInstance);
      this.list.addBig(this.options.biomeBlendRadius());
      this.list.addSmall(options(this.options));
   }

   public void onClose() {
      this.minecraft.getWindow().changeFullscreenVideoMode();
      super.onClose();
   }

   public void removed() {
      if ((Integer)this.options.mipmapLevels().get() != this.oldMipmaps) {
         this.minecraft.updateMaxMipLevel((Integer)this.options.mipmapLevels().get());
         this.minecraft.delayTextureReload();
      }

      super.removed();
   }

   public boolean mouseClicked(double d, double e, int i) {
      if (super.mouseClicked(d, e, i)) {
         if (this.gpuWarnlistManager.isShowingWarning()) {
            List<Component> list = Lists.newArrayList(new Component[]{WARNING_MESSAGE, CommonComponents.NEW_LINE});
            String string = this.gpuWarnlistManager.getRendererWarnings();
            if (string != null) {
               list.add(CommonComponents.NEW_LINE);
               list.add(Component.translatable("options.graphics.warning.renderer", new Object[]{string}).withStyle(ChatFormatting.GRAY));
            }

            String string2 = this.gpuWarnlistManager.getVendorWarnings();
            if (string2 != null) {
               list.add(CommonComponents.NEW_LINE);
               list.add(Component.translatable("options.graphics.warning.vendor", new Object[]{string2}).withStyle(ChatFormatting.GRAY));
            }

            String string3 = this.gpuWarnlistManager.getVersionWarnings();
            if (string3 != null) {
               list.add(CommonComponents.NEW_LINE);
               list.add(Component.translatable("options.graphics.warning.version", new Object[]{string3}).withStyle(ChatFormatting.GRAY));
            }

            this.minecraft.setScreen(new UnsupportedGraphicsWarningScreen(WARNING_TITLE, list, ImmutableList.of(new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_ACCEPT, (button) -> {
               this.options.graphicsMode().set(GraphicsStatus.FABULOUS);
               Minecraft.getInstance().levelRenderer.allChanged();
               this.gpuWarnlistManager.dismissWarning();
               this.minecraft.setScreen(this);
            }), new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_CANCEL, (button) -> {
               this.gpuWarnlistManager.dismissWarningAndSkipFabulous();
               this.minecraft.setScreen(this);
            }))));
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double d, double e, double f, double g) {
      if (Screen.hasControlDown()) {
         OptionInstance<Integer> optionInstance = this.options.guiScale();
         OptionInstance.ValueSet var11 = optionInstance.values();
         if (var11 instanceof OptionInstance.ClampingLazyMaxIntRange) {
            OptionInstance.ClampingLazyMaxIntRange clampingLazyMaxIntRange = (OptionInstance.ClampingLazyMaxIntRange)var11;
            int i = (Integer)optionInstance.get();
            int j = i == 0 ? clampingLazyMaxIntRange.maxInclusive() + 1 : i;
            int k = j + (int)Math.signum(g);
            if (k != 0 && k <= clampingLazyMaxIntRange.maxInclusive() && k >= clampingLazyMaxIntRange.minInclusive()) {
               CycleButton<Integer> cycleButton = (CycleButton)this.list.findOption(optionInstance);
               if (cycleButton != null) {
                  optionInstance.set(k);
                  cycleButton.setValue(k);
                  this.list.setScrollAmount(0.0D);
                  return true;
               }
            }
         }

         return false;
      } else {
         return super.mouseScrolled(d, e, f, g);
      }
   }

   static {
      FABULOUS = Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC);
      WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", new Object[]{FABULOUS, FABULOUS});
      WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
      BUTTON_ACCEPT = Component.translatable("options.graphics.warning.accept");
      BUTTON_CANCEL = Component.translatable("options.graphics.warning.cancel");
   }
}
