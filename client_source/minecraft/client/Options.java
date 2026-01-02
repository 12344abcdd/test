package net.minecraft.client;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.Util.OS;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class Options {
   static final Logger LOGGER = LogUtils.getLogger();
   static final Gson GSON = new Gson();
   private static final TypeToken<List<String>> LIST_OF_STRINGS_TYPE = new TypeToken<List<String>>() {
   };
   public static final int RENDER_DISTANCE_TINY = 2;
   public static final int RENDER_DISTANCE_SHORT = 4;
   public static final int RENDER_DISTANCE_NORMAL = 8;
   public static final int RENDER_DISTANCE_FAR = 12;
   public static final int RENDER_DISTANCE_REALLY_FAR = 16;
   public static final int RENDER_DISTANCE_EXTREME = 32;
   private static final Splitter OPTION_SPLITTER = Splitter.on(':').limit(2);
   public static final String DEFAULT_SOUND_DEVICE = "";
   private static final Component ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND = Component.translatable("options.darkMojangStudiosBackgroundColor.tooltip");
   private final OptionInstance<Boolean> darkMojangStudiosBackground;
   private static final Component ACCESSIBILITY_TOOLTIP_HIDE_LIGHTNING_FLASHES = Component.translatable("options.hideLightningFlashes.tooltip");
   private final OptionInstance<Boolean> hideLightningFlash;
   private static final Component ACCESSIBILITY_TOOLTIP_HIDE_SPLASH_TEXTS = Component.translatable("options.hideSplashTexts.tooltip");
   private final OptionInstance<Boolean> hideSplashTexts;
   private final OptionInstance<Double> sensitivity;
   private final OptionInstance<Integer> renderDistance;
   private final OptionInstance<Integer> simulationDistance;
   private int serverRenderDistance;
   private final OptionInstance<Double> entityDistanceScaling;
   public static final int UNLIMITED_FRAMERATE_CUTOFF = 260;
   private final OptionInstance<Integer> framerateLimit;
   private final OptionInstance<CloudStatus> cloudStatus;
   private static final Component GRAPHICS_TOOLTIP_FAST = Component.translatable("options.graphics.fast.tooltip");
   private static final Component GRAPHICS_TOOLTIP_FABULOUS;
   private static final Component GRAPHICS_TOOLTIP_FANCY;
   private final OptionInstance<GraphicsStatus> graphicsMode;
   private final OptionInstance<Boolean> ambientOcclusion;
   private static final Component PRIORITIZE_CHUNK_TOOLTIP_NONE;
   private static final Component PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED;
   private static final Component PRIORITIZE_CHUNK_TOOLTIP_NEARBY;
   private final OptionInstance<PrioritizeChunkUpdates> prioritizeChunkUpdates;
   public List<String> resourcePacks;
   public List<String> incompatibleResourcePacks;
   private final OptionInstance<ChatVisiblity> chatVisibility;
   private final OptionInstance<Double> chatOpacity;
   private final OptionInstance<Double> chatLineSpacing;
   private static final Component MENU_BACKGROUND_BLURRINESS_TOOLTIP;
   private static final int BLURRINESS_DEFAULT_VALUE = 5;
   private final OptionInstance<Integer> menuBackgroundBlurriness;
   private final OptionInstance<Double> textBackgroundOpacity;
   private final OptionInstance<Double> panoramaSpeed;
   private static final Component ACCESSIBILITY_TOOLTIP_CONTRAST_MODE;
   private final OptionInstance<Boolean> highContrast;
   private final OptionInstance<Boolean> narratorHotkey;
   @Nullable
   public String fullscreenVideoModeString;
   public boolean hideServerAddress;
   public boolean advancedItemTooltips;
   public boolean pauseOnLostFocus;
   private final Set<PlayerModelPart> modelParts;
   private final OptionInstance<HumanoidArm> mainHand;
   public int overrideWidth;
   public int overrideHeight;
   private final OptionInstance<Double> chatScale;
   private final OptionInstance<Double> chatWidth;
   private final OptionInstance<Double> chatHeightUnfocused;
   private final OptionInstance<Double> chatHeightFocused;
   private final OptionInstance<Double> chatDelay;
   private static final Component ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME;
   private final OptionInstance<Double> notificationDisplayTime;
   private final OptionInstance<Integer> mipmapLevels;
   public boolean useNativeTransport;
   private final OptionInstance<AttackIndicatorStatus> attackIndicator;
   public TutorialSteps tutorialStep;
   public boolean joinedFirstServer;
   public boolean hideBundleTutorial;
   private final OptionInstance<Integer> biomeBlendRadius;
   private final OptionInstance<Double> mouseWheelSensitivity;
   private final OptionInstance<Boolean> rawMouseInput;
   public int glDebugVerbosity;
   private final OptionInstance<Boolean> autoJump;
   private final OptionInstance<Boolean> operatorItemsTab;
   private final OptionInstance<Boolean> autoSuggestions;
   private final OptionInstance<Boolean> chatColors;
   private final OptionInstance<Boolean> chatLinks;
   private final OptionInstance<Boolean> chatLinksPrompt;
   private final OptionInstance<Boolean> enableVsync;
   private final OptionInstance<Boolean> entityShadows;
   private final OptionInstance<Boolean> forceUnicodeFont;
   private final OptionInstance<Boolean> japaneseGlyphVariants;
   private final OptionInstance<Boolean> invertYMouse;
   private final OptionInstance<Boolean> discreteMouseScroll;
   private static final Component REALMS_NOTIFICATIONS_TOOLTIP;
   private final OptionInstance<Boolean> realmsNotifications;
   private static final Component ALLOW_SERVER_LISTING_TOOLTIP;
   private final OptionInstance<Boolean> allowServerListing;
   private final OptionInstance<Boolean> reducedDebugInfo;
   private final Map<SoundSource, OptionInstance<Double>> soundSourceVolumes;
   private final OptionInstance<Boolean> showSubtitles;
   private static final Component DIRECTIONAL_AUDIO_TOOLTIP_ON;
   private static final Component DIRECTIONAL_AUDIO_TOOLTIP_OFF;
   private final OptionInstance<Boolean> directionalAudio;
   private final OptionInstance<Boolean> backgroundForChatOnly;
   private final OptionInstance<Boolean> touchscreen;
   private final OptionInstance<Boolean> fullscreen;
   private final OptionInstance<Boolean> bobView;
   private static final Component MOVEMENT_TOGGLE;
   private static final Component MOVEMENT_HOLD;
   private final OptionInstance<Boolean> toggleCrouch;
   private final OptionInstance<Boolean> toggleSprint;
   public boolean skipMultiplayerWarning;
   private static final Component CHAT_TOOLTIP_HIDE_MATCHED_NAMES;
   private final OptionInstance<Boolean> hideMatchedNames;
   private final OptionInstance<Boolean> showAutosaveIndicator;
   private static final Component CHAT_TOOLTIP_ONLY_SHOW_SECURE;
   private final OptionInstance<Boolean> onlyShowSecureChat;
   public final KeyMapping keyUp;
   public final KeyMapping keyLeft;
   public final KeyMapping keyDown;
   public final KeyMapping keyRight;
   public final KeyMapping keyJump;
   public final KeyMapping keyShift;
   public final KeyMapping keySprint;
   public final KeyMapping keyInventory;
   public final KeyMapping keySwapOffhand;
   public final KeyMapping keyDrop;
   public final KeyMapping keyUse;
   public final KeyMapping keyAttack;
   public final KeyMapping keyPickItem;
   public final KeyMapping keyChat;
   public final KeyMapping keyPlayerList;
   public final KeyMapping keyCommand;
   public final KeyMapping keySocialInteractions;
   public final KeyMapping keyScreenshot;
   public final KeyMapping keyTogglePerspective;
   public final KeyMapping keySmoothCamera;
   public final KeyMapping keyFullscreen;
   public final KeyMapping keySpectatorOutlines;
   public final KeyMapping keyAdvancements;
   public final KeyMapping[] keyHotbarSlots;
   public final KeyMapping keySaveHotbarActivator;
   public final KeyMapping keyLoadHotbarActivator;
   public final KeyMapping[] keyMappings;
   protected Minecraft minecraft;
   private final File optionsFile;
   public boolean hideGui;
   private CameraType cameraType;
   public String lastMpIp;
   public boolean smoothCamera;
   private final OptionInstance<Integer> fov;
   private static final Component TELEMETRY_TOOLTIP;
   private final OptionInstance<Boolean> telemetryOptInExtra;
   private static final Component ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT;
   private final OptionInstance<Double> screenEffectScale;
   private static final Component ACCESSIBILITY_TOOLTIP_FOV_EFFECT;
   private final OptionInstance<Double> fovEffectScale;
   private static final Component ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT;
   private final OptionInstance<Double> darknessEffectScale;
   private static final Component ACCESSIBILITY_TOOLTIP_GLINT_SPEED;
   private final OptionInstance<Double> glintSpeed;
   private static final Component ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH;
   private final OptionInstance<Double> glintStrength;
   private static final Component ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH;
   private final OptionInstance<Double> damageTiltStrength;
   private final OptionInstance<Double> gamma;
   public static final int AUTO_GUI_SCALE = 0;
   private static final int MAX_GUI_SCALE_INCLUSIVE = 2147483646;
   private final OptionInstance<Integer> guiScale;
   private final OptionInstance<ParticleStatus> particles;
   private final OptionInstance<NarratorStatus> narrator;
   public String languageCode;
   private final OptionInstance<String> soundDevice;
   public boolean onboardAccessibility;
   public boolean syncWrites;

   public OptionInstance<Boolean> darkMojangStudiosBackground() {
      return this.darkMojangStudiosBackground;
   }

   public OptionInstance<Boolean> hideLightningFlash() {
      return this.hideLightningFlash;
   }

   public OptionInstance<Boolean> hideSplashTexts() {
      return this.hideSplashTexts;
   }

   public OptionInstance<Double> sensitivity() {
      return this.sensitivity;
   }

   public OptionInstance<Integer> renderDistance() {
      return this.renderDistance;
   }

   public OptionInstance<Integer> simulationDistance() {
      return this.simulationDistance;
   }

   public OptionInstance<Double> entityDistanceScaling() {
      return this.entityDistanceScaling;
   }

   public OptionInstance<Integer> framerateLimit() {
      return this.framerateLimit;
   }

   public OptionInstance<CloudStatus> cloudStatus() {
      return this.cloudStatus;
   }

   public OptionInstance<GraphicsStatus> graphicsMode() {
      return this.graphicsMode;
   }

   public OptionInstance<Boolean> ambientOcclusion() {
      return this.ambientOcclusion;
   }

   public OptionInstance<PrioritizeChunkUpdates> prioritizeChunkUpdates() {
      return this.prioritizeChunkUpdates;
   }

   public void updateResourcePacks(PackRepository packRepository) {
      List<String> list = ImmutableList.copyOf(this.resourcePacks);
      this.resourcePacks.clear();
      this.incompatibleResourcePacks.clear();
      Iterator var3 = packRepository.getSelectedPacks().iterator();

      while(var3.hasNext()) {
         Pack pack = (Pack)var3.next();
         if (!pack.isFixedPosition()) {
            this.resourcePacks.add(pack.getId());
            if (!pack.getCompatibility().isCompatible()) {
               this.incompatibleResourcePacks.add(pack.getId());
            }
         }
      }

      this.save();
      List<String> list2 = ImmutableList.copyOf(this.resourcePacks);
      if (!list2.equals(list)) {
         this.minecraft.reloadResourcePacks();
      }

   }

   public OptionInstance<ChatVisiblity> chatVisibility() {
      return this.chatVisibility;
   }

   public OptionInstance<Double> chatOpacity() {
      return this.chatOpacity;
   }

   public OptionInstance<Double> chatLineSpacing() {
      return this.chatLineSpacing;
   }

   public OptionInstance<Integer> menuBackgroundBlurriness() {
      return this.menuBackgroundBlurriness;
   }

   public int getMenuBackgroundBlurriness() {
      return (Integer)this.menuBackgroundBlurriness().get();
   }

   public OptionInstance<Double> textBackgroundOpacity() {
      return this.textBackgroundOpacity;
   }

   public OptionInstance<Double> panoramaSpeed() {
      return this.panoramaSpeed;
   }

   public OptionInstance<Boolean> highContrast() {
      return this.highContrast;
   }

   public OptionInstance<Boolean> narratorHotkey() {
      return this.narratorHotkey;
   }

   public OptionInstance<HumanoidArm> mainHand() {
      return this.mainHand;
   }

   public OptionInstance<Double> chatScale() {
      return this.chatScale;
   }

   public OptionInstance<Double> chatWidth() {
      return this.chatWidth;
   }

   public OptionInstance<Double> chatHeightUnfocused() {
      return this.chatHeightUnfocused;
   }

   public OptionInstance<Double> chatHeightFocused() {
      return this.chatHeightFocused;
   }

   public OptionInstance<Double> chatDelay() {
      return this.chatDelay;
   }

   public OptionInstance<Double> notificationDisplayTime() {
      return this.notificationDisplayTime;
   }

   public OptionInstance<Integer> mipmapLevels() {
      return this.mipmapLevels;
   }

   public OptionInstance<AttackIndicatorStatus> attackIndicator() {
      return this.attackIndicator;
   }

   public OptionInstance<Integer> biomeBlendRadius() {
      return this.biomeBlendRadius;
   }

   private static double logMouse(int i) {
      return Math.pow(10.0D, (double)i / 100.0D);
   }

   private static int unlogMouse(double d) {
      return Mth.floor(Math.log10(d) * 100.0D);
   }

   public OptionInstance<Double> mouseWheelSensitivity() {
      return this.mouseWheelSensitivity;
   }

   public OptionInstance<Boolean> rawMouseInput() {
      return this.rawMouseInput;
   }

   public OptionInstance<Boolean> autoJump() {
      return this.autoJump;
   }

   public OptionInstance<Boolean> operatorItemsTab() {
      return this.operatorItemsTab;
   }

   public OptionInstance<Boolean> autoSuggestions() {
      return this.autoSuggestions;
   }

   public OptionInstance<Boolean> chatColors() {
      return this.chatColors;
   }

   public OptionInstance<Boolean> chatLinks() {
      return this.chatLinks;
   }

   public OptionInstance<Boolean> chatLinksPrompt() {
      return this.chatLinksPrompt;
   }

   public OptionInstance<Boolean> enableVsync() {
      return this.enableVsync;
   }

   public OptionInstance<Boolean> entityShadows() {
      return this.entityShadows;
   }

   private static void updateFontOptions() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.getWindow() != null) {
         minecraft.updateFontOptions();
         minecraft.resizeDisplay();
      }

   }

   public OptionInstance<Boolean> forceUnicodeFont() {
      return this.forceUnicodeFont;
   }

   private static boolean japaneseGlyphVariantsDefault() {
      return Locale.getDefault().getLanguage().equalsIgnoreCase("ja");
   }

   public OptionInstance<Boolean> japaneseGlyphVariants() {
      return this.japaneseGlyphVariants;
   }

   public OptionInstance<Boolean> invertYMouse() {
      return this.invertYMouse;
   }

   public OptionInstance<Boolean> discreteMouseScroll() {
      return this.discreteMouseScroll;
   }

   public OptionInstance<Boolean> realmsNotifications() {
      return this.realmsNotifications;
   }

   public OptionInstance<Boolean> allowServerListing() {
      return this.allowServerListing;
   }

   public OptionInstance<Boolean> reducedDebugInfo() {
      return this.reducedDebugInfo;
   }

   public final float getSoundSourceVolume(SoundSource soundSource) {
      return ((Double)this.getSoundSourceOptionInstance(soundSource).get()).floatValue();
   }

   public final OptionInstance<Double> getSoundSourceOptionInstance(SoundSource soundSource) {
      return (OptionInstance)Objects.requireNonNull((OptionInstance)this.soundSourceVolumes.get(soundSource));
   }

   private OptionInstance<Double> createSoundSliderOptionInstance(String string, SoundSource soundSource) {
      return new OptionInstance(string, OptionInstance.noTooltip(), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 1.0D, (double_) -> {
         Minecraft.getInstance().getSoundManager().updateSourceVolume(soundSource, double_.floatValue());
      });
   }

   public OptionInstance<Boolean> showSubtitles() {
      return this.showSubtitles;
   }

   public OptionInstance<Boolean> directionalAudio() {
      return this.directionalAudio;
   }

   public OptionInstance<Boolean> backgroundForChatOnly() {
      return this.backgroundForChatOnly;
   }

   public OptionInstance<Boolean> touchscreen() {
      return this.touchscreen;
   }

   public OptionInstance<Boolean> fullscreen() {
      return this.fullscreen;
   }

   public OptionInstance<Boolean> bobView() {
      return this.bobView;
   }

   public OptionInstance<Boolean> toggleCrouch() {
      return this.toggleCrouch;
   }

   public OptionInstance<Boolean> toggleSprint() {
      return this.toggleSprint;
   }

   public OptionInstance<Boolean> hideMatchedNames() {
      return this.hideMatchedNames;
   }

   public OptionInstance<Boolean> showAutosaveIndicator() {
      return this.showAutosaveIndicator;
   }

   public OptionInstance<Boolean> onlyShowSecureChat() {
      return this.onlyShowSecureChat;
   }

   public OptionInstance<Integer> fov() {
      return this.fov;
   }

   public OptionInstance<Boolean> telemetryOptInExtra() {
      return this.telemetryOptInExtra;
   }

   public OptionInstance<Double> screenEffectScale() {
      return this.screenEffectScale;
   }

   public OptionInstance<Double> fovEffectScale() {
      return this.fovEffectScale;
   }

   public OptionInstance<Double> darknessEffectScale() {
      return this.darknessEffectScale;
   }

   public OptionInstance<Double> glintSpeed() {
      return this.glintSpeed;
   }

   public OptionInstance<Double> glintStrength() {
      return this.glintStrength;
   }

   public OptionInstance<Double> damageTiltStrength() {
      return this.damageTiltStrength;
   }

   public OptionInstance<Double> gamma() {
      return this.gamma;
   }

   public OptionInstance<Integer> guiScale() {
      return this.guiScale;
   }

   public OptionInstance<ParticleStatus> particles() {
      return this.particles;
   }

   public OptionInstance<NarratorStatus> narrator() {
      return this.narrator;
   }

   public OptionInstance<String> soundDevice() {
      return this.soundDevice;
   }

   public void onboardingAccessibilityFinished() {
      this.onboardAccessibility = false;
      this.save();
   }

   public Options(Minecraft minecraft, File file) {
      this.darkMojangStudiosBackground = OptionInstance.createBoolean("options.darkMojangStudiosBackgroundColor", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND), false);
      this.hideLightningFlash = OptionInstance.createBoolean("options.hideLightningFlashes", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_HIDE_LIGHTNING_FLASHES), false);
      this.hideSplashTexts = OptionInstance.createBoolean("options.hideSplashTexts", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_HIDE_SPLASH_TEXTS), false);
      this.sensitivity = new OptionInstance("options.sensitivity", OptionInstance.noTooltip(), (component, double_) -> {
         if (double_ == 0.0D) {
            return genericValueLabel(component, Component.translatable("options.sensitivity.min"));
         } else {
            return double_ == 1.0D ? genericValueLabel(component, Component.translatable("options.sensitivity.max")) : percentValueLabel(component, 2.0D * double_);
         }
      }, OptionInstance.UnitDouble.INSTANCE, 0.5D, (double_) -> {
      });
      this.serverRenderDistance = 0;
      this.entityDistanceScaling = new OptionInstance("options.entityDistanceScaling", OptionInstance.noTooltip(), Options::percentValueLabel, (new OptionInstance.IntRange(2, 20)).xmap((i) -> {
         return (double)i / 4.0D;
      }, (double_) -> {
         return (int)(double_ * 4.0D);
      }), Codec.doubleRange(0.5D, 5.0D), 1.0D, (double_) -> {
      });
      this.framerateLimit = new OptionInstance("options.framerateLimit", OptionInstance.noTooltip(), (component, integer) -> {
         return integer == 260 ? genericValueLabel(component, Component.translatable("options.framerateLimit.max")) : genericValueLabel(component, Component.translatable("options.framerate", new Object[]{integer}));
      }, (new OptionInstance.IntRange(1, 26)).xmap((i) -> {
         return i * 10;
      }, (integer) -> {
         return integer / 10;
      }), Codec.intRange(10, 260), 120, (integer) -> {
         Minecraft.getInstance().getWindow().setFramerateLimit(integer);
      });
      this.cloudStatus = new OptionInstance("options.renderClouds", OptionInstance.noTooltip(), OptionInstance.forOptionEnum(), new OptionInstance.Enum(Arrays.asList(CloudStatus.values()), Codec.withAlternative(CloudStatus.CODEC, Codec.BOOL, (boolean_) -> {
         return boolean_ ? CloudStatus.FANCY : CloudStatus.OFF;
      })), CloudStatus.FANCY, (cloudStatus) -> {
         if (Minecraft.useShaderTransparency()) {
            RenderTarget renderTarget = Minecraft.getInstance().levelRenderer.getCloudsTarget();
            if (renderTarget != null) {
               renderTarget.clear(Minecraft.ON_OSX);
            }
         }

      });
      this.graphicsMode = new OptionInstance("options.graphics", (graphicsStatus) -> {
         Tooltip var10000;
         switch(graphicsStatus) {
         case FANCY:
            var10000 = Tooltip.create(GRAPHICS_TOOLTIP_FANCY);
            break;
         case FAST:
            var10000 = Tooltip.create(GRAPHICS_TOOLTIP_FAST);
            break;
         case FABULOUS:
            var10000 = Tooltip.create(GRAPHICS_TOOLTIP_FABULOUS);
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }, (component, graphicsStatus) -> {
         MutableComponent mutableComponent = Component.translatable(graphicsStatus.getKey());
         return graphicsStatus == GraphicsStatus.FABULOUS ? mutableComponent.withStyle(ChatFormatting.ITALIC) : mutableComponent;
      }, new OptionInstance.AltEnum(Arrays.asList(GraphicsStatus.values()), (List)Stream.of(GraphicsStatus.values()).filter((graphicsStatus) -> {
         return graphicsStatus != GraphicsStatus.FABULOUS;
      }).collect(Collectors.toList()), () -> {
         return Minecraft.getInstance().isRunning() && Minecraft.getInstance().getGpuWarnlistManager().isSkippingFabulous();
      }, (optionInstance, graphicsStatus) -> {
         Minecraft minecraft = Minecraft.getInstance();
         GpuWarnlistManager gpuWarnlistManager = minecraft.getGpuWarnlistManager();
         if (graphicsStatus == GraphicsStatus.FABULOUS && gpuWarnlistManager.willShowWarning()) {
            gpuWarnlistManager.showWarning();
         } else {
            optionInstance.set(graphicsStatus);
            minecraft.levelRenderer.allChanged();
         }
      }, Codec.INT.xmap(GraphicsStatus::byId, GraphicsStatus::getId)), GraphicsStatus.FANCY, (graphicsStatus) -> {
      });
      this.ambientOcclusion = OptionInstance.createBoolean("options.ao", true, (boolean_) -> {
         Minecraft.getInstance().levelRenderer.allChanged();
      });
      this.prioritizeChunkUpdates = new OptionInstance("options.prioritizeChunkUpdates", (prioritizeChunkUpdates) -> {
         Tooltip var10000;
         switch(prioritizeChunkUpdates) {
         case NONE:
            var10000 = Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_NONE);
            break;
         case PLAYER_AFFECTED:
            var10000 = Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED);
            break;
         case NEARBY:
            var10000 = Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_NEARBY);
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }, OptionInstance.forOptionEnum(), new OptionInstance.Enum(Arrays.asList(PrioritizeChunkUpdates.values()), Codec.INT.xmap(PrioritizeChunkUpdates::byId, PrioritizeChunkUpdates::getId)), PrioritizeChunkUpdates.NONE, (prioritizeChunkUpdates) -> {
      });
      this.resourcePacks = Lists.newArrayList();
      this.incompatibleResourcePacks = Lists.newArrayList();
      this.chatVisibility = new OptionInstance("options.chat.visibility", OptionInstance.noTooltip(), OptionInstance.forOptionEnum(), new OptionInstance.Enum(Arrays.asList(ChatVisiblity.values()), Codec.INT.xmap(ChatVisiblity::byId, ChatVisiblity::getId)), ChatVisiblity.FULL, (chatVisiblity) -> {
      });
      this.chatOpacity = new OptionInstance("options.chat.opacity", OptionInstance.noTooltip(), (component, double_) -> {
         return percentValueLabel(component, double_ * 0.9D + 0.1D);
      }, OptionInstance.UnitDouble.INSTANCE, 1.0D, (double_) -> {
         Minecraft.getInstance().gui.getChat().rescaleChat();
      });
      this.chatLineSpacing = new OptionInstance("options.chat.line_spacing", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.0D, (double_) -> {
      });
      this.menuBackgroundBlurriness = new OptionInstance("options.accessibility.menu_background_blurriness", OptionInstance.cachedConstantTooltip(MENU_BACKGROUND_BLURRINESS_TOOLTIP), Options::genericValueOrOffLabel, new OptionInstance.IntRange(0, 10), 5, (integer) -> {
      });
      this.textBackgroundOpacity = new OptionInstance("options.accessibility.text_background_opacity", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.5D, (double_) -> {
         Minecraft.getInstance().gui.getChat().rescaleChat();
      });
      this.panoramaSpeed = new OptionInstance("options.accessibility.panorama_speed", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 1.0D, (double_) -> {
      });
      this.highContrast = OptionInstance.createBoolean("options.accessibility.high_contrast", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_CONTRAST_MODE), false, (boolean_) -> {
         PackRepository packRepository = Minecraft.getInstance().getResourcePackRepository();
         boolean bl = packRepository.getSelectedIds().contains("high_contrast");
         if (!bl && boolean_) {
            if (packRepository.addPack("high_contrast")) {
               this.updateResourcePacks(packRepository);
            }
         } else if (bl && !boolean_ && packRepository.removePack("high_contrast")) {
            this.updateResourcePacks(packRepository);
         }

      });
      this.narratorHotkey = OptionInstance.createBoolean("options.accessibility.narrator_hotkey", OptionInstance.cachedConstantTooltip(Minecraft.ON_OSX ? Component.translatable("options.accessibility.narrator_hotkey.mac.tooltip") : Component.translatable("options.accessibility.narrator_hotkey.tooltip")), true);
      this.pauseOnLostFocus = true;
      this.modelParts = EnumSet.allOf(PlayerModelPart.class);
      this.mainHand = new OptionInstance("options.mainHand", OptionInstance.noTooltip(), OptionInstance.forOptionEnum(), new OptionInstance.Enum(Arrays.asList(HumanoidArm.values()), HumanoidArm.CODEC), HumanoidArm.RIGHT, (humanoidArm) -> {
         this.broadcastOptions();
      });
      this.chatScale = new OptionInstance("options.chat.scale", OptionInstance.noTooltip(), (component, double_) -> {
         return (Component)(double_ == 0.0D ? CommonComponents.optionStatus(component, false) : percentValueLabel(component, double_));
      }, OptionInstance.UnitDouble.INSTANCE, 1.0D, (double_) -> {
         Minecraft.getInstance().gui.getChat().rescaleChat();
      });
      this.chatWidth = new OptionInstance("options.chat.width", OptionInstance.noTooltip(), (component, double_) -> {
         return pixelValueLabel(component, ChatComponent.getWidth(double_));
      }, OptionInstance.UnitDouble.INSTANCE, 1.0D, (double_) -> {
         Minecraft.getInstance().gui.getChat().rescaleChat();
      });
      this.chatHeightUnfocused = new OptionInstance("options.chat.height.unfocused", OptionInstance.noTooltip(), (component, double_) -> {
         return pixelValueLabel(component, ChatComponent.getHeight(double_));
      }, OptionInstance.UnitDouble.INSTANCE, ChatComponent.defaultUnfocusedPct(), (double_) -> {
         Minecraft.getInstance().gui.getChat().rescaleChat();
      });
      this.chatHeightFocused = new OptionInstance("options.chat.height.focused", OptionInstance.noTooltip(), (component, double_) -> {
         return pixelValueLabel(component, ChatComponent.getHeight(double_));
      }, OptionInstance.UnitDouble.INSTANCE, 1.0D, (double_) -> {
         Minecraft.getInstance().gui.getChat().rescaleChat();
      });
      this.chatDelay = new OptionInstance("options.chat.delay_instant", OptionInstance.noTooltip(), (component, double_) -> {
         return double_ <= 0.0D ? Component.translatable("options.chat.delay_none") : Component.translatable("options.chat.delay", new Object[]{String.format(Locale.ROOT, "%.1f", double_)});
      }, (new OptionInstance.IntRange(0, 60)).xmap((i) -> {
         return (double)i / 10.0D;
      }, (double_) -> {
         return (int)(double_ * 10.0D);
      }), Codec.doubleRange(0.0D, 6.0D), 0.0D, (double_) -> {
         Minecraft.getInstance().getChatListener().setMessageDelay(double_);
      });
      this.notificationDisplayTime = new OptionInstance("options.notifications.display_time", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME), (component, double_) -> {
         return genericValueLabel(component, Component.translatable("options.multiplier", new Object[]{double_}));
      }, (new OptionInstance.IntRange(5, 100)).xmap((i) -> {
         return (double)i / 10.0D;
      }, (double_) -> {
         return (int)(double_ * 10.0D);
      }), Codec.doubleRange(0.5D, 10.0D), 1.0D, (double_) -> {
      });
      this.mipmapLevels = new OptionInstance("options.mipmapLevels", OptionInstance.noTooltip(), (component, integer) -> {
         return (Component)(integer == 0 ? CommonComponents.optionStatus(component, false) : genericValueLabel(component, integer));
      }, new OptionInstance.IntRange(0, 4), 4, (integer) -> {
      });
      this.useNativeTransport = true;
      this.attackIndicator = new OptionInstance("options.attackIndicator", OptionInstance.noTooltip(), OptionInstance.forOptionEnum(), new OptionInstance.Enum(Arrays.asList(AttackIndicatorStatus.values()), Codec.INT.xmap(AttackIndicatorStatus::byId, AttackIndicatorStatus::getId)), AttackIndicatorStatus.CROSSHAIR, (attackIndicatorStatus) -> {
      });
      this.tutorialStep = TutorialSteps.MOVEMENT;
      this.joinedFirstServer = false;
      this.hideBundleTutorial = false;
      this.biomeBlendRadius = new OptionInstance("options.biomeBlendRadius", OptionInstance.noTooltip(), (component, integer) -> {
         int i = integer * 2 + 1;
         return genericValueLabel(component, Component.translatable("options.biomeBlendRadius." + i));
      }, new OptionInstance.IntRange(0, 7, false), 2, (integer) -> {
         Minecraft.getInstance().levelRenderer.allChanged();
      });
      this.mouseWheelSensitivity = new OptionInstance("options.mouseWheelSensitivity", OptionInstance.noTooltip(), (component, double_) -> {
         return genericValueLabel(component, Component.literal(String.format(Locale.ROOT, "%.2f", double_)));
      }, (new OptionInstance.IntRange(-200, 100)).xmap(Options::logMouse, Options::unlogMouse), Codec.doubleRange(logMouse(-200), logMouse(100)), logMouse(0), (double_) -> {
      });
      this.rawMouseInput = OptionInstance.createBoolean("options.rawMouseInput", true, (boolean_) -> {
         Window window = Minecraft.getInstance().getWindow();
         if (window != null) {
            window.updateRawMouseInput(boolean_);
         }

      });
      this.glDebugVerbosity = 1;
      this.autoJump = OptionInstance.createBoolean("options.autoJump", false);
      this.operatorItemsTab = OptionInstance.createBoolean("options.operatorItemsTab", false);
      this.autoSuggestions = OptionInstance.createBoolean("options.autoSuggestCommands", true);
      this.chatColors = OptionInstance.createBoolean("options.chat.color", true);
      this.chatLinks = OptionInstance.createBoolean("options.chat.links", true);
      this.chatLinksPrompt = OptionInstance.createBoolean("options.chat.links.prompt", true);
      this.enableVsync = OptionInstance.createBoolean("options.vsync", true, (boolean_) -> {
         if (Minecraft.getInstance().getWindow() != null) {
            Minecraft.getInstance().getWindow().updateVsync(boolean_);
         }

      });
      this.entityShadows = OptionInstance.createBoolean("options.entityShadows", true);
      this.forceUnicodeFont = OptionInstance.createBoolean("options.forceUnicodeFont", false, (boolean_) -> {
         updateFontOptions();
      });
      this.japaneseGlyphVariants = OptionInstance.createBoolean("options.japaneseGlyphVariants", OptionInstance.cachedConstantTooltip(Component.translatable("options.japaneseGlyphVariants.tooltip")), japaneseGlyphVariantsDefault(), (boolean_) -> {
         updateFontOptions();
      });
      this.invertYMouse = OptionInstance.createBoolean("options.invertMouse", false);
      this.discreteMouseScroll = OptionInstance.createBoolean("options.discrete_mouse_scroll", false);
      this.realmsNotifications = OptionInstance.createBoolean("options.realmsNotifications", OptionInstance.cachedConstantTooltip(REALMS_NOTIFICATIONS_TOOLTIP), true);
      this.allowServerListing = OptionInstance.createBoolean("options.allowServerListing", OptionInstance.cachedConstantTooltip(ALLOW_SERVER_LISTING_TOOLTIP), true, (boolean_) -> {
         this.broadcastOptions();
      });
      this.reducedDebugInfo = OptionInstance.createBoolean("options.reducedDebugInfo", false);
      this.soundSourceVolumes = (Map)Util.make(new EnumMap(SoundSource.class), (enumMap) -> {
         SoundSource[] var2 = SoundSource.values();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            SoundSource soundSource = var2[var4];
            enumMap.put(soundSource, this.createSoundSliderOptionInstance("soundCategory." + soundSource.getName(), soundSource));
         }

      });
      this.showSubtitles = OptionInstance.createBoolean("options.showSubtitles", false);
      this.directionalAudio = OptionInstance.createBoolean("options.directionalAudio", (boolean_) -> {
         return boolean_ ? Tooltip.create(DIRECTIONAL_AUDIO_TOOLTIP_ON) : Tooltip.create(DIRECTIONAL_AUDIO_TOOLTIP_OFF);
      }, false, (boolean_) -> {
         SoundManager soundManager = Minecraft.getInstance().getSoundManager();
         soundManager.reload();
         soundManager.play(SimpleSoundInstance.forUI((Holder)SoundEvents.UI_BUTTON_CLICK, 1.0F));
      });
      this.backgroundForChatOnly = new OptionInstance("options.accessibility.text_background", OptionInstance.noTooltip(), (component, boolean_) -> {
         return boolean_ ? Component.translatable("options.accessibility.text_background.chat") : Component.translatable("options.accessibility.text_background.everywhere");
      }, OptionInstance.BOOLEAN_VALUES, true, (boolean_) -> {
      });
      this.touchscreen = OptionInstance.createBoolean("options.touchscreen", false);
      this.fullscreen = OptionInstance.createBoolean("options.fullscreen", false, (boolean_) -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.getWindow() != null && minecraft.getWindow().isFullscreen() != boolean_) {
            minecraft.getWindow().toggleFullScreen();
            this.fullscreen().set(minecraft.getWindow().isFullscreen());
         }

      });
      this.bobView = OptionInstance.createBoolean("options.viewBobbing", true);
      this.toggleCrouch = new OptionInstance("key.sneak", OptionInstance.noTooltip(), (component, boolean_) -> {
         return boolean_ ? MOVEMENT_TOGGLE : MOVEMENT_HOLD;
      }, OptionInstance.BOOLEAN_VALUES, false, (boolean_) -> {
      });
      this.toggleSprint = new OptionInstance("key.sprint", OptionInstance.noTooltip(), (component, boolean_) -> {
         return boolean_ ? MOVEMENT_TOGGLE : MOVEMENT_HOLD;
      }, OptionInstance.BOOLEAN_VALUES, false, (boolean_) -> {
      });
      this.hideMatchedNames = OptionInstance.createBoolean("options.hideMatchedNames", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_HIDE_MATCHED_NAMES), true);
      this.showAutosaveIndicator = OptionInstance.createBoolean("options.autosaveIndicator", true);
      this.onlyShowSecureChat = OptionInstance.createBoolean("options.onlyShowSecureChat", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_ONLY_SHOW_SECURE), false);
      this.keyUp = new KeyMapping("key.forward", 87, "key.categories.movement");
      this.keyLeft = new KeyMapping("key.left", 65, "key.categories.movement");
      this.keyDown = new KeyMapping("key.back", 83, "key.categories.movement");
      this.keyRight = new KeyMapping("key.right", 68, "key.categories.movement");
      this.keyJump = new KeyMapping("key.jump", 32, "key.categories.movement");
      OptionInstance var10006 = this.toggleCrouch;
      Objects.requireNonNull(var10006);
      this.keyShift = new ToggleKeyMapping("key.sneak", 340, "key.categories.movement", var10006::get);
      var10006 = this.toggleSprint;
      Objects.requireNonNull(var10006);
      this.keySprint = new ToggleKeyMapping("key.sprint", 341, "key.categories.movement", var10006::get);
      this.keyInventory = new KeyMapping("key.inventory", 69, "key.categories.inventory");
      this.keySwapOffhand = new KeyMapping("key.swapOffhand", 70, "key.categories.inventory");
      this.keyDrop = new KeyMapping("key.drop", 81, "key.categories.inventory");
      this.keyUse = new KeyMapping("key.use", InputConstants.Type.MOUSE, 1, "key.categories.gameplay");
      this.keyAttack = new KeyMapping("key.attack", InputConstants.Type.MOUSE, 0, "key.categories.gameplay");
      this.keyPickItem = new KeyMapping("key.pickItem", InputConstants.Type.MOUSE, 2, "key.categories.gameplay");
      this.keyChat = new KeyMapping("key.chat", 84, "key.categories.multiplayer");
      this.keyPlayerList = new KeyMapping("key.playerlist", 258, "key.categories.multiplayer");
      this.keyCommand = new KeyMapping("key.command", 47, "key.categories.multiplayer");
      this.keySocialInteractions = new KeyMapping("key.socialInteractions", 80, "key.categories.multiplayer");
      this.keyScreenshot = new KeyMapping("key.screenshot", 291, "key.categories.misc");
      this.keyTogglePerspective = new KeyMapping("key.togglePerspective", 294, "key.categories.misc");
      this.keySmoothCamera = new KeyMapping("key.smoothCamera", InputConstants.UNKNOWN.getValue(), "key.categories.misc");
      this.keyFullscreen = new KeyMapping("key.fullscreen", 300, "key.categories.misc");
      this.keySpectatorOutlines = new KeyMapping("key.spectatorOutlines", InputConstants.UNKNOWN.getValue(), "key.categories.misc");
      this.keyAdvancements = new KeyMapping("key.advancements", 76, "key.categories.misc");
      this.keyHotbarSlots = new KeyMapping[]{new KeyMapping("key.hotbar.1", 49, "key.categories.inventory"), new KeyMapping("key.hotbar.2", 50, "key.categories.inventory"), new KeyMapping("key.hotbar.3", 51, "key.categories.inventory"), new KeyMapping("key.hotbar.4", 52, "key.categories.inventory"), new KeyMapping("key.hotbar.5", 53, "key.categories.inventory"), new KeyMapping("key.hotbar.6", 54, "key.categories.inventory"), new KeyMapping("key.hotbar.7", 55, "key.categories.inventory"), new KeyMapping("key.hotbar.8", 56, "key.categories.inventory"), new KeyMapping("key.hotbar.9", 57, "key.categories.inventory")};
      this.keySaveHotbarActivator = new KeyMapping("key.saveToolbarActivator", 67, "key.categories.creative");
      this.keyLoadHotbarActivator = new KeyMapping("key.loadToolbarActivator", 88, "key.categories.creative");
      this.keyMappings = (KeyMapping[])ArrayUtils.addAll(new KeyMapping[]{this.keyAttack, this.keyUse, this.keyUp, this.keyLeft, this.keyDown, this.keyRight, this.keyJump, this.keyShift, this.keySprint, this.keyDrop, this.keyInventory, this.keyChat, this.keyPlayerList, this.keyPickItem, this.keyCommand, this.keySocialInteractions, this.keyScreenshot, this.keyTogglePerspective, this.keySmoothCamera, this.keyFullscreen, this.keySpectatorOutlines, this.keySwapOffhand, this.keySaveHotbarActivator, this.keyLoadHotbarActivator, this.keyAdvancements}, this.keyHotbarSlots);
      this.cameraType = CameraType.FIRST_PERSON;
      this.lastMpIp = "";
      this.fov = new OptionInstance("options.fov", OptionInstance.noTooltip(), (component, integer) -> {
         Component var10000;
         switch(integer) {
         case 70:
            var10000 = genericValueLabel(component, Component.translatable("options.fov.min"));
            break;
         case 110:
            var10000 = genericValueLabel(component, Component.translatable("options.fov.max"));
            break;
         default:
            var10000 = genericValueLabel(component, integer);
         }

         return var10000;
      }, new OptionInstance.IntRange(30, 110), Codec.DOUBLE.xmap((double_) -> {
         return (int)(double_ * 40.0D + 70.0D);
      }, (integer) -> {
         return ((double)integer - 70.0D) / 40.0D;
      }), 70, (integer) -> {
         Minecraft.getInstance().levelRenderer.needsUpdate();
      });
      this.telemetryOptInExtra = OptionInstance.createBoolean("options.telemetry.button", OptionInstance.cachedConstantTooltip(TELEMETRY_TOOLTIP), (component, boolean_) -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (!minecraft.allowsTelemetry()) {
            return Component.translatable("options.telemetry.state.none");
         } else {
            return boolean_ && minecraft.extraTelemetryAvailable() ? Component.translatable("options.telemetry.state.all") : Component.translatable("options.telemetry.state.minimal");
         }
      }, false, (boolean_) -> {
      });
      this.screenEffectScale = new OptionInstance("options.screenEffectScale", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 1.0D, (double_) -> {
      });
      this.fovEffectScale = new OptionInstance("options.fovEffectScale", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_FOV_EFFECT), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt), Codec.doubleRange(0.0D, 1.0D), 1.0D, (double_) -> {
      });
      this.darknessEffectScale = new OptionInstance("options.darknessEffectScale", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt), 1.0D, (double_) -> {
      });
      this.glintSpeed = new OptionInstance("options.glintSpeed", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_GLINT_SPEED), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 0.5D, (double_) -> {
      });
      this.glintStrength = new OptionInstance("options.glintStrength", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 0.75D, RenderSystem::setShaderGlintAlpha);
      this.damageTiltStrength = new OptionInstance("options.damageTiltStrength", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 1.0D, (double_) -> {
      });
      this.gamma = new OptionInstance("options.gamma", OptionInstance.noTooltip(), (component, double_) -> {
         int i = (int)(double_ * 100.0D);
         if (i == 0) {
            return genericValueLabel(component, Component.translatable("options.gamma.min"));
         } else if (i == 50) {
            return genericValueLabel(component, Component.translatable("options.gamma.default"));
         } else {
            return i == 100 ? genericValueLabel(component, Component.translatable("options.gamma.max")) : genericValueLabel(component, i);
         }
      }, OptionInstance.UnitDouble.INSTANCE, 0.5D, (double_) -> {
      });
      this.guiScale = new OptionInstance("options.guiScale", OptionInstance.noTooltip(), (component, integer) -> {
         return integer == 0 ? Component.translatable("options.guiScale.auto") : Component.literal(Integer.toString(integer));
      }, new OptionInstance.ClampingLazyMaxIntRange(0, () -> {
         Minecraft minecraft = Minecraft.getInstance();
         return !minecraft.isRunning() ? 2147483646 : minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
      }, 2147483646), 0, (integer) -> {
         this.minecraft.resizeDisplay();
      });
      this.particles = new OptionInstance("options.particles", OptionInstance.noTooltip(), OptionInstance.forOptionEnum(), new OptionInstance.Enum(Arrays.asList(ParticleStatus.values()), Codec.INT.xmap(ParticleStatus::byId, ParticleStatus::getId)), ParticleStatus.ALL, (particleStatus) -> {
      });
      this.narrator = new OptionInstance("options.narrator", OptionInstance.noTooltip(), (component, narratorStatus) -> {
         return (Component)(this.minecraft.getNarrator().isActive() ? narratorStatus.getName() : Component.translatable("options.narrator.notavailable"));
      }, new OptionInstance.Enum(Arrays.asList(NarratorStatus.values()), Codec.INT.xmap(NarratorStatus::byId, NarratorStatus::getId)), NarratorStatus.OFF, (narratorStatus) -> {
         this.minecraft.getNarrator().updateNarratorStatus(narratorStatus);
      });
      this.languageCode = "en_us";
      this.soundDevice = new OptionInstance("options.audioDevice", OptionInstance.noTooltip(), (component, string) -> {
         if ("".equals(string)) {
            return Component.translatable("options.audioDevice.default");
         } else {
            return string.startsWith("OpenAL Soft on ") ? Component.literal(string.substring(SoundEngine.OPEN_AL_SOFT_PREFIX_LENGTH)) : Component.literal(string);
         }
      }, new OptionInstance.LazyEnum(() -> {
         return Stream.concat(Stream.of(""), Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().stream()).toList();
      }, (string) -> {
         return Minecraft.getInstance().isRunning() && string != "" && !Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().contains(string) ? Optional.empty() : Optional.of(string);
      }, Codec.STRING), "", (string) -> {
         SoundManager soundManager = Minecraft.getInstance().getSoundManager();
         soundManager.reload();
         soundManager.play(SimpleSoundInstance.forUI((Holder)SoundEvents.UI_BUTTON_CLICK, 1.0F));
      });
      this.onboardAccessibility = true;
      this.minecraft = minecraft;
      this.optionsFile = new File(file, "options.txt");
      boolean bl = Runtime.getRuntime().maxMemory() >= 1000000000L;
      this.renderDistance = new OptionInstance("options.renderDistance", OptionInstance.noTooltip(), (component, integer) -> {
         return genericValueLabel(component, Component.translatable("options.chunks", new Object[]{integer}));
      }, new OptionInstance.IntRange(2, bl ? 32 : 16, false), 12, (integer) -> {
         Minecraft.getInstance().levelRenderer.needsUpdate();
      });
      this.simulationDistance = new OptionInstance("options.simulationDistance", OptionInstance.noTooltip(), (component, integer) -> {
         return genericValueLabel(component, Component.translatable("options.chunks", new Object[]{integer}));
      }, new OptionInstance.IntRange(5, bl ? 32 : 16, false), 12, (integer) -> {
      });
      this.syncWrites = Util.getPlatform() == OS.WINDOWS;
      this.load();
   }

   public float getBackgroundOpacity(float f) {
      return (Boolean)this.backgroundForChatOnly.get() ? f : ((Double)this.textBackgroundOpacity().get()).floatValue();
   }

   public int getBackgroundColor(float f) {
      return (int)(this.getBackgroundOpacity(f) * 255.0F) << 24 & -16777216;
   }

   public int getBackgroundColor(int i) {
      return (Boolean)this.backgroundForChatOnly.get() ? i : (int)((Double)this.textBackgroundOpacity.get() * 255.0D) << 24 & -16777216;
   }

   public void setKey(KeyMapping keyMapping, InputConstants.Key key) {
      keyMapping.setKey(key);
      this.save();
   }

   private void processDumpedOptions(Options.OptionAccess optionAccess) {
      optionAccess.process("ao", this.ambientOcclusion);
      optionAccess.process("biomeBlendRadius", this.biomeBlendRadius);
      optionAccess.process("enableVsync", this.enableVsync);
      optionAccess.process("entityDistanceScaling", this.entityDistanceScaling);
      optionAccess.process("entityShadows", this.entityShadows);
      optionAccess.process("forceUnicodeFont", this.forceUnicodeFont);
      optionAccess.process("japaneseGlyphVariants", this.japaneseGlyphVariants);
      optionAccess.process("fov", this.fov);
      optionAccess.process("fovEffectScale", this.fovEffectScale);
      optionAccess.process("darknessEffectScale", this.darknessEffectScale);
      optionAccess.process("glintSpeed", this.glintSpeed);
      optionAccess.process("glintStrength", this.glintStrength);
      optionAccess.process("prioritizeChunkUpdates", this.prioritizeChunkUpdates);
      optionAccess.process("fullscreen", this.fullscreen);
      optionAccess.process("gamma", this.gamma);
      optionAccess.process("graphicsMode", this.graphicsMode);
      optionAccess.process("guiScale", this.guiScale);
      optionAccess.process("maxFps", this.framerateLimit);
      optionAccess.process("mipmapLevels", this.mipmapLevels);
      optionAccess.process("narrator", this.narrator);
      optionAccess.process("particles", this.particles);
      optionAccess.process("reducedDebugInfo", this.reducedDebugInfo);
      optionAccess.process("renderClouds", this.cloudStatus);
      optionAccess.process("renderDistance", this.renderDistance);
      optionAccess.process("simulationDistance", this.simulationDistance);
      optionAccess.process("screenEffectScale", this.screenEffectScale);
      optionAccess.process("soundDevice", this.soundDevice);
   }

   private void processOptions(Options.FieldAccess fieldAccess) {
      this.processDumpedOptions(fieldAccess);
      fieldAccess.process("autoJump", this.autoJump);
      fieldAccess.process("operatorItemsTab", this.operatorItemsTab);
      fieldAccess.process("autoSuggestions", this.autoSuggestions);
      fieldAccess.process("chatColors", this.chatColors);
      fieldAccess.process("chatLinks", this.chatLinks);
      fieldAccess.process("chatLinksPrompt", this.chatLinksPrompt);
      fieldAccess.process("discrete_mouse_scroll", this.discreteMouseScroll);
      fieldAccess.process("invertYMouse", this.invertYMouse);
      fieldAccess.process("realmsNotifications", this.realmsNotifications);
      fieldAccess.process("showSubtitles", this.showSubtitles);
      fieldAccess.process("directionalAudio", this.directionalAudio);
      fieldAccess.process("touchscreen", this.touchscreen);
      fieldAccess.process("bobView", this.bobView);
      fieldAccess.process("toggleCrouch", this.toggleCrouch);
      fieldAccess.process("toggleSprint", this.toggleSprint);
      fieldAccess.process("darkMojangStudiosBackground", this.darkMojangStudiosBackground);
      fieldAccess.process("hideLightningFlashes", this.hideLightningFlash);
      fieldAccess.process("hideSplashTexts", this.hideSplashTexts);
      fieldAccess.process("mouseSensitivity", this.sensitivity);
      fieldAccess.process("damageTiltStrength", this.damageTiltStrength);
      fieldAccess.process("highContrast", this.highContrast);
      fieldAccess.process("narratorHotkey", this.narratorHotkey);
      List var10003 = this.resourcePacks;
      Function var10004 = Options::readListOfStrings;
      Gson var10005 = GSON;
      Objects.requireNonNull(var10005);
      this.resourcePacks = (List)fieldAccess.process("resourcePacks", var10003, var10004, var10005::toJson);
      var10003 = this.incompatibleResourcePacks;
      var10004 = Options::readListOfStrings;
      var10005 = GSON;
      Objects.requireNonNull(var10005);
      this.incompatibleResourcePacks = (List)fieldAccess.process("incompatibleResourcePacks", var10003, var10004, var10005::toJson);
      this.lastMpIp = fieldAccess.process("lastServer", this.lastMpIp);
      this.languageCode = fieldAccess.process("lang", this.languageCode);
      fieldAccess.process("chatVisibility", this.chatVisibility);
      fieldAccess.process("chatOpacity", this.chatOpacity);
      fieldAccess.process("chatLineSpacing", this.chatLineSpacing);
      fieldAccess.process("textBackgroundOpacity", this.textBackgroundOpacity);
      fieldAccess.process("backgroundForChatOnly", this.backgroundForChatOnly);
      this.hideServerAddress = fieldAccess.process("hideServerAddress", this.hideServerAddress);
      this.advancedItemTooltips = fieldAccess.process("advancedItemTooltips", this.advancedItemTooltips);
      this.pauseOnLostFocus = fieldAccess.process("pauseOnLostFocus", this.pauseOnLostFocus);
      this.overrideWidth = fieldAccess.process("overrideWidth", this.overrideWidth);
      this.overrideHeight = fieldAccess.process("overrideHeight", this.overrideHeight);
      fieldAccess.process("chatHeightFocused", this.chatHeightFocused);
      fieldAccess.process("chatDelay", this.chatDelay);
      fieldAccess.process("chatHeightUnfocused", this.chatHeightUnfocused);
      fieldAccess.process("chatScale", this.chatScale);
      fieldAccess.process("chatWidth", this.chatWidth);
      fieldAccess.process("notificationDisplayTime", this.notificationDisplayTime);
      this.useNativeTransport = fieldAccess.process("useNativeTransport", this.useNativeTransport);
      fieldAccess.process("mainHand", this.mainHand);
      fieldAccess.process("attackIndicator", this.attackIndicator);
      this.tutorialStep = (TutorialSteps)fieldAccess.process("tutorialStep", this.tutorialStep, TutorialSteps::getByName, TutorialSteps::getName);
      fieldAccess.process("mouseWheelSensitivity", this.mouseWheelSensitivity);
      fieldAccess.process("rawMouseInput", this.rawMouseInput);
      this.glDebugVerbosity = fieldAccess.process("glDebugVerbosity", this.glDebugVerbosity);
      this.skipMultiplayerWarning = fieldAccess.process("skipMultiplayerWarning", this.skipMultiplayerWarning);
      fieldAccess.process("hideMatchedNames", this.hideMatchedNames);
      this.joinedFirstServer = fieldAccess.process("joinedFirstServer", this.joinedFirstServer);
      this.hideBundleTutorial = fieldAccess.process("hideBundleTutorial", this.hideBundleTutorial);
      this.syncWrites = fieldAccess.process("syncChunkWrites", this.syncWrites);
      fieldAccess.process("showAutosaveIndicator", this.showAutosaveIndicator);
      fieldAccess.process("allowServerListing", this.allowServerListing);
      fieldAccess.process("onlyShowSecureChat", this.onlyShowSecureChat);
      fieldAccess.process("panoramaScrollSpeed", this.panoramaSpeed);
      fieldAccess.process("telemetryOptInExtra", this.telemetryOptInExtra);
      this.onboardAccessibility = fieldAccess.process("onboardAccessibility", this.onboardAccessibility);
      fieldAccess.process("menuBackgroundBlurriness", this.menuBackgroundBlurriness);
      KeyMapping[] var2 = this.keyMappings;
      int var3 = var2.length;

      int var4;
      for(var4 = 0; var4 < var3; ++var4) {
         KeyMapping keyMapping = var2[var4];
         String string = keyMapping.saveString();
         String string2 = fieldAccess.process("key_" + keyMapping.getName(), string);
         if (!string.equals(string2)) {
            keyMapping.setKey(InputConstants.getKey(string2));
         }
      }

      SoundSource[] var8 = SoundSource.values();
      var3 = var8.length;

      for(var4 = 0; var4 < var3; ++var4) {
         SoundSource soundSource = var8[var4];
         fieldAccess.process("soundCategory_" + soundSource.getName(), (OptionInstance)this.soundSourceVolumes.get(soundSource));
      }

      PlayerModelPart[] var9 = PlayerModelPart.values();
      var3 = var9.length;

      for(var4 = 0; var4 < var3; ++var4) {
         PlayerModelPart playerModelPart = var9[var4];
         boolean bl = this.modelParts.contains(playerModelPart);
         boolean bl2 = fieldAccess.process("modelPart_" + playerModelPart.getId(), bl);
         if (bl2 != bl) {
            this.setModelPart(playerModelPart, bl2);
         }
      }

   }

   public void load() {
      try {
         if (!this.optionsFile.exists()) {
            return;
         }

         CompoundTag compoundTag = new CompoundTag();
         BufferedReader bufferedReader = Files.newReader(this.optionsFile, Charsets.UTF_8);

         try {
            bufferedReader.lines().forEach((string) -> {
               try {
                  Iterator<String> iterator = OPTION_SPLITTER.split(string).iterator();
                  compoundTag.putString((String)iterator.next(), (String)iterator.next());
               } catch (Exception var3) {
                  LOGGER.warn("Skipping bad option: {}", string);
               }

            });
         } catch (Throwable var6) {
            if (bufferedReader != null) {
               try {
                  bufferedReader.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (bufferedReader != null) {
            bufferedReader.close();
         }

         final CompoundTag compoundTag2 = this.dataFix(compoundTag);
         if (!compoundTag2.contains("graphicsMode") && compoundTag2.contains("fancyGraphics")) {
            if (isTrue(compoundTag2.getString("fancyGraphics"))) {
               this.graphicsMode.set(GraphicsStatus.FANCY);
            } else {
               this.graphicsMode.set(GraphicsStatus.FAST);
            }
         }

         this.processOptions(new Options.FieldAccess(this) {
            @Nullable
            private String getValueOrNull(String string) {
               return compoundTag2.contains(string) ? compoundTag2.get(string).getAsString() : null;
            }

            public <T> void process(String string, OptionInstance<T> optionInstance) {
               String string2 = this.getValueOrNull(string);
               if (string2 != null) {
                  JsonReader jsonReader = new JsonReader(new StringReader(string2.isEmpty() ? "\"\"" : string2));
                  JsonElement jsonElement = JsonParser.parseReader(jsonReader);
                  DataResult<T> dataResult = optionInstance.codec().parse(JsonOps.INSTANCE, jsonElement);
                  dataResult.error().ifPresent((error) -> {
                     Options.LOGGER.error("Error parsing option value " + string2 + " for option " + String.valueOf(optionInstance) + ": " + error.message());
                  });
                  Objects.requireNonNull(optionInstance);
                  dataResult.ifSuccess(optionInstance::set);
               }

            }

            public int process(String string, int i) {
               String string2 = this.getValueOrNull(string);
               if (string2 != null) {
                  try {
                     return Integer.parseInt(string2);
                  } catch (NumberFormatException var5) {
                     Options.LOGGER.warn("Invalid integer value for option {} = {}", new Object[]{string, string2, var5});
                  }
               }

               return i;
            }

            public boolean process(String string, boolean bl) {
               String string2 = this.getValueOrNull(string);
               return string2 != null ? Options.isTrue(string2) : bl;
            }

            public String process(String string, String string2) {
               return (String)MoreObjects.firstNonNull(this.getValueOrNull(string), string2);
            }

            public float process(String string, float f) {
               String string2 = this.getValueOrNull(string);
               if (string2 != null) {
                  if (Options.isTrue(string2)) {
                     return 1.0F;
                  }

                  if (Options.isFalse(string2)) {
                     return 0.0F;
                  }

                  try {
                     return Float.parseFloat(string2);
                  } catch (NumberFormatException var5) {
                     Options.LOGGER.warn("Invalid floating point value for option {} = {}", new Object[]{string, string2, var5});
                  }
               }

               return f;
            }

            public <T> T process(String string, T object, Function<String, T> function, Function<T, String> function2) {
               String string2 = this.getValueOrNull(string);
               return string2 == null ? object : function.apply(string2);
            }
         });
         if (compoundTag2.contains("fullscreenResolution")) {
            this.fullscreenVideoModeString = compoundTag2.getString("fullscreenResolution");
         }

         if (this.minecraft.getWindow() != null) {
            this.minecraft.getWindow().setFramerateLimit((Integer)this.framerateLimit.get());
         }

         KeyMapping.resetMapping();
      } catch (Exception var7) {
         LOGGER.error("Failed to load options", var7);
      }

   }

   static boolean isTrue(String string) {
      return "true".equals(string);
   }

   static boolean isFalse(String string) {
      return "false".equals(string);
   }

   private CompoundTag dataFix(CompoundTag compoundTag) {
      int i = 0;

      try {
         i = Integer.parseInt(compoundTag.getString("version"));
      } catch (RuntimeException var4) {
      }

      return DataFixTypes.OPTIONS.updateToCurrentVersion(this.minecraft.getFixerUpper(), compoundTag, i);
   }

   public void save() {
      try {
         final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.optionsFile), StandardCharsets.UTF_8));

         try {
            printWriter.println("version:" + SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            this.processOptions(new Options.FieldAccess(this) {
               public void writePrefix(String string) {
                  printWriter.print(string);
                  printWriter.print(':');
               }

               public <T> void process(String string, OptionInstance<T> optionInstance) {
                  optionInstance.codec().encodeStart(JsonOps.INSTANCE, optionInstance.get()).ifError((error) -> {
                     Logger var10000 = Options.LOGGER;
                     String var10001 = String.valueOf(optionInstance);
                     var10000.error("Error saving option " + var10001 + ": " + String.valueOf(error));
                  }).ifSuccess((jsonElement) -> {
                     this.writePrefix(string);
                     printWriter.println(Options.GSON.toJson(jsonElement));
                  });
               }

               public int process(String string, int i) {
                  this.writePrefix(string);
                  printWriter.println(i);
                  return i;
               }

               public boolean process(String string, boolean bl) {
                  this.writePrefix(string);
                  printWriter.println(bl);
                  return bl;
               }

               public String process(String string, String string2) {
                  this.writePrefix(string);
                  printWriter.println(string2);
                  return string2;
               }

               public float process(String string, float f) {
                  this.writePrefix(string);
                  printWriter.println(f);
                  return f;
               }

               public <T> T process(String string, T object, Function<String, T> function, Function<T, String> function2) {
                  this.writePrefix(string);
                  printWriter.println((String)function2.apply(object));
                  return object;
               }
            });
            if (this.minecraft.getWindow().getPreferredFullscreenVideoMode().isPresent()) {
               printWriter.println("fullscreenResolution:" + ((VideoMode)this.minecraft.getWindow().getPreferredFullscreenVideoMode().get()).write());
            }
         } catch (Throwable var5) {
            try {
               printWriter.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         printWriter.close();
      } catch (Exception var6) {
         LOGGER.error("Failed to save options", var6);
      }

      this.broadcastOptions();
   }

   public ClientInformation buildPlayerInformation() {
      int i = 0;

      PlayerModelPart playerModelPart;
      for(Iterator var2 = this.modelParts.iterator(); var2.hasNext(); i |= playerModelPart.getMask()) {
         playerModelPart = (PlayerModelPart)var2.next();
      }

      return new ClientInformation(this.languageCode, (Integer)this.renderDistance.get(), (ChatVisiblity)this.chatVisibility.get(), (Boolean)this.chatColors.get(), i, (HumanoidArm)this.mainHand.get(), this.minecraft.isTextFilteringEnabled(), (Boolean)this.allowServerListing.get());
   }

   public void broadcastOptions() {
      if (this.minecraft.player != null) {
         this.minecraft.player.connection.send(new ServerboundClientInformationPacket(this.buildPlayerInformation()));
      }

   }

   private void setModelPart(PlayerModelPart playerModelPart, boolean bl) {
      if (bl) {
         this.modelParts.add(playerModelPart);
      } else {
         this.modelParts.remove(playerModelPart);
      }

   }

   public boolean isModelPartEnabled(PlayerModelPart playerModelPart) {
      return this.modelParts.contains(playerModelPart);
   }

   public void toggleModelPart(PlayerModelPart playerModelPart, boolean bl) {
      this.setModelPart(playerModelPart, bl);
      this.broadcastOptions();
   }

   public CloudStatus getCloudsType() {
      return this.getEffectiveRenderDistance() >= 4 ? (CloudStatus)this.cloudStatus.get() : CloudStatus.OFF;
   }

   public boolean useNativeTransport() {
      return this.useNativeTransport;
   }

   public void loadSelectedResourcePacks(PackRepository packRepository) {
      Set<String> set = Sets.newLinkedHashSet();
      Iterator iterator = this.resourcePacks.iterator();

      while(true) {
         while(iterator.hasNext()) {
            String string = (String)iterator.next();
            Pack pack = packRepository.getPack(string);
            if (pack == null && !string.startsWith("file/")) {
               pack = packRepository.getPack("file/" + string);
            }

            if (pack == null) {
               LOGGER.warn("Removed resource pack {} from options because it doesn't seem to exist anymore", string);
               iterator.remove();
            } else if (!pack.getCompatibility().isCompatible() && !this.incompatibleResourcePacks.contains(string)) {
               LOGGER.warn("Removed resource pack {} from options because it is no longer compatible", string);
               iterator.remove();
            } else if (pack.getCompatibility().isCompatible() && this.incompatibleResourcePacks.contains(string)) {
               LOGGER.info("Removed resource pack {} from incompatibility list because it's now compatible", string);
               this.incompatibleResourcePacks.remove(string);
            } else {
               set.add(pack.getId());
            }
         }

         packRepository.setSelected(set);
         return;
      }
   }

   public CameraType getCameraType() {
      return this.cameraType;
   }

   public void setCameraType(CameraType cameraType) {
      this.cameraType = cameraType;
   }

   private static List<String> readListOfStrings(String string) {
      List<String> list = (List)GsonHelper.fromNullableJson(GSON, string, LIST_OF_STRINGS_TYPE);
      return (List)(list != null ? list : Lists.newArrayList());
   }

   public File getFile() {
      return this.optionsFile;
   }

   public String dumpOptionsForReport() {
      final List<Pair<String, Object>> list = new ArrayList();
      this.processDumpedOptions(new Options.OptionAccess(this) {
         public <T> void process(String string, OptionInstance<T> optionInstance) {
            list.add(Pair.of(string, optionInstance.get()));
         }
      });
      list.add(Pair.of("fullscreenResolution", String.valueOf(this.fullscreenVideoModeString)));
      list.add(Pair.of("glDebugVerbosity", this.glDebugVerbosity));
      list.add(Pair.of("overrideHeight", this.overrideHeight));
      list.add(Pair.of("overrideWidth", this.overrideWidth));
      list.add(Pair.of("syncChunkWrites", this.syncWrites));
      list.add(Pair.of("useNativeTransport", this.useNativeTransport));
      list.add(Pair.of("resourcePacks", this.resourcePacks));
      return (String)list.stream().sorted(Comparator.comparing(Pair::getFirst)).map((pair) -> {
         String var10000 = (String)pair.getFirst();
         return var10000 + ": " + String.valueOf(pair.getSecond());
      }).collect(Collectors.joining(System.lineSeparator()));
   }

   public void setServerRenderDistance(int i) {
      this.serverRenderDistance = i;
   }

   public int getEffectiveRenderDistance() {
      return this.serverRenderDistance > 0 ? Math.min((Integer)this.renderDistance.get(), this.serverRenderDistance) : (Integer)this.renderDistance.get();
   }

   private static Component pixelValueLabel(Component component, int i) {
      return Component.translatable("options.pixel_value", new Object[]{component, i});
   }

   private static Component percentValueLabel(Component component, double d) {
      return Component.translatable("options.percent_value", new Object[]{component, (int)(d * 100.0D)});
   }

   public static Component genericValueLabel(Component component, Component component2) {
      return Component.translatable("options.generic_value", new Object[]{component, component2});
   }

   public static Component genericValueLabel(Component component, int i) {
      return genericValueLabel(component, Component.literal(Integer.toString(i)));
   }

   public static Component genericValueOrOffLabel(Component component, int i) {
      return i == 0 ? genericValueLabel(component, CommonComponents.OPTION_OFF) : genericValueLabel(component, i);
   }

   private static Component percentValueOrOffLabel(Component component, double d) {
      return d == 0.0D ? genericValueLabel(component, CommonComponents.OPTION_OFF) : percentValueLabel(component, d);
   }

   static {
      GRAPHICS_TOOLTIP_FABULOUS = Component.translatable("options.graphics.fabulous.tooltip", new Object[]{Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC)});
      GRAPHICS_TOOLTIP_FANCY = Component.translatable("options.graphics.fancy.tooltip");
      PRIORITIZE_CHUNK_TOOLTIP_NONE = Component.translatable("options.prioritizeChunkUpdates.none.tooltip");
      PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED = Component.translatable("options.prioritizeChunkUpdates.byPlayer.tooltip");
      PRIORITIZE_CHUNK_TOOLTIP_NEARBY = Component.translatable("options.prioritizeChunkUpdates.nearby.tooltip");
      MENU_BACKGROUND_BLURRINESS_TOOLTIP = Component.translatable("options.accessibility.menu_background_blurriness.tooltip");
      ACCESSIBILITY_TOOLTIP_CONTRAST_MODE = Component.translatable("options.accessibility.high_contrast.tooltip");
      ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME = Component.translatable("options.notifications.display_time.tooltip");
      REALMS_NOTIFICATIONS_TOOLTIP = Component.translatable("options.realmsNotifications.tooltip");
      ALLOW_SERVER_LISTING_TOOLTIP = Component.translatable("options.allowServerListing.tooltip");
      DIRECTIONAL_AUDIO_TOOLTIP_ON = Component.translatable("options.directionalAudio.on.tooltip");
      DIRECTIONAL_AUDIO_TOOLTIP_OFF = Component.translatable("options.directionalAudio.off.tooltip");
      MOVEMENT_TOGGLE = Component.translatable("options.key.toggle");
      MOVEMENT_HOLD = Component.translatable("options.key.hold");
      CHAT_TOOLTIP_HIDE_MATCHED_NAMES = Component.translatable("options.hideMatchedNames.tooltip");
      CHAT_TOOLTIP_ONLY_SHOW_SECURE = Component.translatable("options.onlyShowSecureChat.tooltip");
      TELEMETRY_TOOLTIP = Component.translatable("options.telemetry.button.tooltip", new Object[]{Component.translatable("options.telemetry.state.minimal"), Component.translatable("options.telemetry.state.all")});
      ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT = Component.translatable("options.screenEffectScale.tooltip");
      ACCESSIBILITY_TOOLTIP_FOV_EFFECT = Component.translatable("options.fovEffectScale.tooltip");
      ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT = Component.translatable("options.darknessEffectScale.tooltip");
      ACCESSIBILITY_TOOLTIP_GLINT_SPEED = Component.translatable("options.glintSpeed.tooltip");
      ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH = Component.translatable("options.glintStrength.tooltip");
      ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH = Component.translatable("options.damageTiltStrength.tooltip");
   }

   @Environment(EnvType.CLIENT)
   private interface OptionAccess {
      <T> void process(String string, OptionInstance<T> optionInstance);
   }

   @Environment(EnvType.CLIENT)
   private interface FieldAccess extends Options.OptionAccess {
      int process(String string, int i);

      boolean process(String string, boolean bl);

      String process(String string, String string2);

      float process(String string, float f);

      <T> T process(String string, T object, Function<String, T> function, Function<T, String> function2);
   }
}
