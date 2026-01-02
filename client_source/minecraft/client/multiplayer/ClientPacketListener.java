package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.time.Instant;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DemoIntroScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.client.renderer.debug.NeighborsUpdateRenderer;
import net.minecraft.client.renderer.debug.VillageSectionsDebugRenderer;
import net.minecraft.client.renderer.debug.WorldGenAttemptRenderer;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.chat.LastSeenMessagesTracker.Update;
import net.minecraft.network.chat.RemoteChatSession.Data;
import net.minecraft.network.chat.SignedMessageChain.Encoder;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.custom.BeeDebugPayload;
import net.minecraft.network.protocol.common.custom.BrainDebugPayload;
import net.minecraft.network.protocol.common.custom.BreezeDebugPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.GameEventDebugPayload;
import net.minecraft.network.protocol.common.custom.GameEventListenerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload;
import net.minecraft.network.protocol.common.custom.GoalDebugPayload;
import net.minecraft.network.protocol.common.custom.HiveDebugPayload;
import net.minecraft.network.protocol.common.custom.NeighborUpdatesDebugPayload;
import net.minecraft.network.protocol.common.custom.PathfindingDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiAddedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiRemovedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiTicketCountDebugPayload;
import net.minecraft.network.protocol.common.custom.RaidsDebugPayload;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload;
import net.minecraft.network.protocol.common.custom.VillageSectionsDebugPayload;
import net.minecraft.network.protocol.common.custom.WorldGenAttemptDebugPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket.ChunkBiomeData;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket.Type;
import net.minecraft.network.protocol.game.ClientboundRecipePacket.State;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Parameters;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.AttributeSnapshot;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Crypt.SaltSupplier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey.ValidationException;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientPacketListener extends ClientCommonPacketListenerImpl implements ClientGamePacketListener, TickablePacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable("multiplayer.unsecureserver.toast.title");
   private static final Component UNSERURE_SERVER_TOAST = Component.translatable("multiplayer.unsecureserver.toast");
   private static final Component INVALID_PACKET = Component.translatable("multiplayer.disconnect.invalid_packet");
   private static final Component CHAT_VALIDATION_FAILED_ERROR = Component.translatable("multiplayer.disconnect.chat_validation_failed");
   private static final Component RECONFIGURE_SCREEN_MESSAGE = Component.translatable("connect.reconfiguring");
   private static final int PENDING_OFFSET_THRESHOLD = 64;
   private final GameProfile localGameProfile;
   private ClientLevel level;
   private ClientLevel.ClientLevelData levelData;
   private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
   private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet();
   private final ClientAdvancements advancements;
   private final ClientSuggestionProvider suggestionsProvider;
   private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
   private int serverChunkRadius = 3;
   private int serverSimulationDistance = 3;
   private final RandomSource random = RandomSource.createThreadSafe();
   private CommandDispatcher<SharedSuggestionProvider> commands = new CommandDispatcher();
   private final RecipeManager recipeManager;
   private final UUID id = UUID.randomUUID();
   private Set<ResourceKey<Level>> levels;
   private final Frozen registryAccess;
   private final FeatureFlagSet enabledFeatures;
   private final PotionBrewing potionBrewing;
   @Nullable
   private LocalChatSession chatSession;
   private Encoder signedMessageEncoder;
   private LastSeenMessagesTracker lastSeenMessages;
   private MessageSignatureCache messageSignatureCache;
   private final ChunkBatchSizeCalculator chunkBatchSizeCalculator;
   private final PingDebugMonitor pingDebugMonitor;
   private final DebugSampleSubscriber debugSampleSubscriber;
   @Nullable
   private LevelLoadStatusManager levelLoadStatusManager;
   private boolean serverEnforcesSecureChat;
   private boolean seenInsecureChatWarning;
   private volatile boolean closed;
   private final Scoreboard scoreboard;
   private final SessionSearchTrees searchTrees;

   public ClientPacketListener(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
      super(minecraft, connection, commonListenerCookie);
      this.signedMessageEncoder = Encoder.UNSIGNED;
      this.lastSeenMessages = new LastSeenMessagesTracker(20);
      this.messageSignatureCache = MessageSignatureCache.createDefault();
      this.chunkBatchSizeCalculator = new ChunkBatchSizeCalculator();
      this.seenInsecureChatWarning = false;
      this.scoreboard = new Scoreboard();
      this.searchTrees = new SessionSearchTrees();
      this.localGameProfile = commonListenerCookie.localGameProfile();
      this.registryAccess = commonListenerCookie.receivedRegistries();
      this.enabledFeatures = commonListenerCookie.enabledFeatures();
      this.advancements = new ClientAdvancements(minecraft, this.telemetryManager);
      this.suggestionsProvider = new ClientSuggestionProvider(this, minecraft);
      this.pingDebugMonitor = new PingDebugMonitor(this, minecraft.getDebugOverlay().getPingLogger());
      this.recipeManager = new RecipeManager(this.registryAccess);
      this.debugSampleSubscriber = new DebugSampleSubscriber(this, minecraft.getDebugOverlay());
      if (commonListenerCookie.chatState() != null) {
         minecraft.gui.getChat().restoreState(commonListenerCookie.chatState());
      }

      this.potionBrewing = PotionBrewing.bootstrap(this.enabledFeatures);
   }

   public ClientSuggestionProvider getSuggestionsProvider() {
      return this.suggestionsProvider;
   }

   public void close() {
      this.closed = true;
      this.clearLevel();
      this.telemetryManager.onDisconnect();
   }

   public void clearLevel() {
      this.level = null;
      this.levelLoadStatusManager = null;
   }

   public RecipeManager getRecipeManager() {
      return this.recipeManager;
   }

   public void handleLogin(ClientboundLoginPacket clientboundLoginPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundLoginPacket, this, this.minecraft);
      this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
      CommonPlayerSpawnInfo commonPlayerSpawnInfo = clientboundLoginPacket.commonPlayerSpawnInfo();
      List<ResourceKey<Level>> list = Lists.newArrayList(clientboundLoginPacket.levels());
      Collections.shuffle(list);
      this.levels = Sets.newLinkedHashSet(list);
      ResourceKey<Level> resourceKey = commonPlayerSpawnInfo.dimension();
      Holder<DimensionType> holder = commonPlayerSpawnInfo.dimensionType();
      this.serverChunkRadius = clientboundLoginPacket.chunkRadius();
      this.serverSimulationDistance = clientboundLoginPacket.simulationDistance();
      boolean bl = commonPlayerSpawnInfo.isDebug();
      boolean bl2 = commonPlayerSpawnInfo.isFlat();
      ClientLevel.ClientLevelData clientLevelData = new ClientLevel.ClientLevelData(Difficulty.NORMAL, clientboundLoginPacket.hardcore(), bl2);
      this.levelData = clientLevelData;
      int var10007 = this.serverChunkRadius;
      int var10008 = this.serverSimulationDistance;
      Minecraft var10009 = this.minecraft;
      Objects.requireNonNull(var10009);
      this.level = new ClientLevel(this, clientLevelData, resourceKey, holder, var10007, var10008, var10009::getProfiler, this.minecraft.levelRenderer, bl, commonPlayerSpawnInfo.seed());
      this.minecraft.setLevel(this.level, ReceivingLevelScreen.Reason.OTHER);
      if (this.minecraft.player == null) {
         this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
         this.minecraft.player.setYRot(-180.0F);
         if (this.minecraft.getSingleplayerServer() != null) {
            this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
         }
      }

      this.minecraft.debugRenderer.clear();
      this.minecraft.player.resetPos();
      this.minecraft.player.setId(clientboundLoginPacket.playerId());
      this.level.addEntity(this.minecraft.player);
      this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
      this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
      this.minecraft.cameraEntity = this.minecraft.player;
      this.startWaitingForNewLevel(this.minecraft.player, this.level, ReceivingLevelScreen.Reason.OTHER);
      this.minecraft.player.setReducedDebugInfo(clientboundLoginPacket.reducedDebugInfo());
      this.minecraft.player.setShowDeathScreen(clientboundLoginPacket.showDeathScreen());
      this.minecraft.player.setDoLimitedCrafting(clientboundLoginPacket.doLimitedCrafting());
      this.minecraft.player.setLastDeathLocation(commonPlayerSpawnInfo.lastDeathLocation());
      this.minecraft.player.setPortalCooldown(commonPlayerSpawnInfo.portalCooldown());
      this.minecraft.gameMode.setLocalMode(commonPlayerSpawnInfo.gameType(), commonPlayerSpawnInfo.previousGameType());
      this.minecraft.options.setServerRenderDistance(clientboundLoginPacket.chunkRadius());
      this.chatSession = null;
      this.lastSeenMessages = new LastSeenMessagesTracker(20);
      this.messageSignatureCache = MessageSignatureCache.createDefault();
      if (this.connection.isEncrypted()) {
         this.minecraft.getProfileKeyPairManager().prepareKeyPair().thenAcceptAsync((optional) -> {
            optional.ifPresent(this::setKeyPair);
         }, this.minecraft);
      }

      this.telemetryManager.onPlayerInfoReceived(commonPlayerSpawnInfo.gameType(), clientboundLoginPacket.hardcore());
      this.minecraft.quickPlayLog().log(this.minecraft);
      this.serverEnforcesSecureChat = clientboundLoginPacket.enforcesSecureChat();
      if (this.serverData != null && !this.seenInsecureChatWarning && !this.enforcesSecureChat()) {
         SystemToast systemToast = SystemToast.multiline(this.minecraft, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST);
         this.minecraft.getToasts().addToast(systemToast);
         this.seenInsecureChatWarning = true;
      }

   }

   public void handleAddEntity(ClientboundAddEntityPacket clientboundAddEntityPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundAddEntityPacket, this, this.minecraft);
      Entity entity = this.createEntityFromPacket(clientboundAddEntityPacket);
      if (entity != null) {
         entity.recreateFromPacket(clientboundAddEntityPacket);
         this.level.addEntity(entity);
         this.postAddEntitySoundInstance(entity);
      } else {
         LOGGER.warn("Skipping Entity with id {}", clientboundAddEntityPacket.getType());
      }

   }

   @Nullable
   private Entity createEntityFromPacket(ClientboundAddEntityPacket clientboundAddEntityPacket) {
      EntityType<?> entityType = clientboundAddEntityPacket.getType();
      if (entityType == EntityType.PLAYER) {
         PlayerInfo playerInfo = this.getPlayerInfo(clientboundAddEntityPacket.getUUID());
         if (playerInfo == null) {
            LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", clientboundAddEntityPacket.getUUID());
            return null;
         } else {
            return new RemotePlayer(this.level, playerInfo.getProfile());
         }
      } else {
         return entityType.create(this.level);
      }
   }

   private void postAddEntitySoundInstance(Entity entity) {
      if (entity instanceof AbstractMinecart) {
         AbstractMinecart abstractMinecart = (AbstractMinecart)entity;
         this.minecraft.getSoundManager().play(new MinecartSoundInstance(abstractMinecart));
      } else if (entity instanceof Bee) {
         Bee bee = (Bee)entity;
         boolean bl = bee.isAngry();
         Object beeSoundInstance;
         if (bl) {
            beeSoundInstance = new BeeAggressiveSoundInstance(bee);
         } else {
            beeSoundInstance = new BeeFlyingSoundInstance(bee);
         }

         this.minecraft.getSoundManager().queueTickingSound((TickableSoundInstance)beeSoundInstance);
      }

   }

   public void handleAddExperienceOrb(ClientboundAddExperienceOrbPacket clientboundAddExperienceOrbPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundAddExperienceOrbPacket, this, this.minecraft);
      double d = clientboundAddExperienceOrbPacket.getX();
      double e = clientboundAddExperienceOrbPacket.getY();
      double f = clientboundAddExperienceOrbPacket.getZ();
      Entity entity = new ExperienceOrb(this.level, d, e, f, clientboundAddExperienceOrbPacket.getValue());
      entity.syncPacketPositionCodec(d, e, f);
      entity.setYRot(0.0F);
      entity.setXRot(0.0F);
      entity.setId(clientboundAddExperienceOrbPacket.getId());
      this.level.addEntity(entity);
   }

   public void handleSetEntityMotion(ClientboundSetEntityMotionPacket clientboundSetEntityMotionPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetEntityMotionPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundSetEntityMotionPacket.getId());
      if (entity != null) {
         entity.lerpMotion(clientboundSetEntityMotionPacket.getXa(), clientboundSetEntityMotionPacket.getYa(), clientboundSetEntityMotionPacket.getZa());
      }
   }

   public void handleSetEntityData(ClientboundSetEntityDataPacket clientboundSetEntityDataPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetEntityDataPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundSetEntityDataPacket.id());
      if (entity != null) {
         entity.getEntityData().assignValues(clientboundSetEntityDataPacket.packedItems());
      }

   }

   public void handleTeleportEntity(ClientboundTeleportEntityPacket clientboundTeleportEntityPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundTeleportEntityPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundTeleportEntityPacket.getId());
      if (entity != null) {
         double d = clientboundTeleportEntityPacket.getX();
         double e = clientboundTeleportEntityPacket.getY();
         double f = clientboundTeleportEntityPacket.getZ();
         entity.syncPacketPositionCodec(d, e, f);
         if (!entity.isControlledByLocalInstance()) {
            float g = (float)(clientboundTeleportEntityPacket.getyRot() * 360) / 256.0F;
            float h = (float)(clientboundTeleportEntityPacket.getxRot() * 360) / 256.0F;
            entity.lerpTo(d, e, f, g, h, 3);
            entity.setOnGround(clientboundTeleportEntityPacket.isOnGround());
         }

      }
   }

   public void handleTickingState(ClientboundTickingStatePacket clientboundTickingStatePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundTickingStatePacket, this, this.minecraft);
      if (this.minecraft.level != null) {
         TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
         tickRateManager.setTickRate(clientboundTickingStatePacket.tickRate());
         tickRateManager.setFrozen(clientboundTickingStatePacket.isFrozen());
      }
   }

   public void handleTickingStep(ClientboundTickingStepPacket clientboundTickingStepPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundTickingStepPacket, this, this.minecraft);
      if (this.minecraft.level != null) {
         TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
         tickRateManager.setFrozenTicksToRun(clientboundTickingStepPacket.tickSteps());
      }
   }

   public void handleSetCarriedItem(ClientboundSetCarriedItemPacket clientboundSetCarriedItemPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetCarriedItemPacket, this, this.minecraft);
      if (Inventory.isHotbarSlot(clientboundSetCarriedItemPacket.getSlot())) {
         this.minecraft.player.getInventory().selected = clientboundSetCarriedItemPacket.getSlot();
      }

   }

   public void handleMoveEntity(ClientboundMoveEntityPacket clientboundMoveEntityPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundMoveEntityPacket, this, this.minecraft);
      Entity entity = clientboundMoveEntityPacket.getEntity(this.level);
      if (entity != null) {
         if (!entity.isControlledByLocalInstance()) {
            if (clientboundMoveEntityPacket.hasPosition()) {
               VecDeltaCodec vecDeltaCodec = entity.getPositionCodec();
               Vec3 vec3 = vecDeltaCodec.decode((long)clientboundMoveEntityPacket.getXa(), (long)clientboundMoveEntityPacket.getYa(), (long)clientboundMoveEntityPacket.getZa());
               vecDeltaCodec.setBase(vec3);
               float f = clientboundMoveEntityPacket.hasRotation() ? (float)(clientboundMoveEntityPacket.getyRot() * 360) / 256.0F : entity.lerpTargetYRot();
               float g = clientboundMoveEntityPacket.hasRotation() ? (float)(clientboundMoveEntityPacket.getxRot() * 360) / 256.0F : entity.lerpTargetXRot();
               entity.lerpTo(vec3.x(), vec3.y(), vec3.z(), f, g, 3);
            } else if (clientboundMoveEntityPacket.hasRotation()) {
               float h = (float)(clientboundMoveEntityPacket.getyRot() * 360) / 256.0F;
               float i = (float)(clientboundMoveEntityPacket.getxRot() * 360) / 256.0F;
               entity.lerpTo(entity.lerpTargetX(), entity.lerpTargetY(), entity.lerpTargetZ(), h, i, 3);
            }

            entity.setOnGround(clientboundMoveEntityPacket.isOnGround());
         }

      }
   }

   public void handleRotateMob(ClientboundRotateHeadPacket clientboundRotateHeadPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundRotateHeadPacket, this, this.minecraft);
      Entity entity = clientboundRotateHeadPacket.getEntity(this.level);
      if (entity != null) {
         float f = (float)(clientboundRotateHeadPacket.getYHeadRot() * 360) / 256.0F;
         entity.lerpHeadTo(f, 3);
      }
   }

   public void handleRemoveEntities(ClientboundRemoveEntitiesPacket clientboundRemoveEntitiesPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundRemoveEntitiesPacket, this, this.minecraft);
      clientboundRemoveEntitiesPacket.getEntityIds().forEach((i) -> {
         this.level.removeEntity(i, RemovalReason.DISCARDED);
      });
   }

   public void handleMovePlayer(ClientboundPlayerPositionPacket clientboundPlayerPositionPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPlayerPositionPacket, this, this.minecraft);
      Player player = this.minecraft.player;
      Vec3 vec3 = player.getDeltaMovement();
      boolean bl = clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.X);
      boolean bl2 = clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.Y);
      boolean bl3 = clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.Z);
      double d;
      double e;
      if (bl) {
         d = vec3.x();
         e = player.getX() + clientboundPlayerPositionPacket.getX();
         player.xOld += clientboundPlayerPositionPacket.getX();
         player.xo += clientboundPlayerPositionPacket.getX();
      } else {
         d = 0.0D;
         e = clientboundPlayerPositionPacket.getX();
         player.xOld = e;
         player.xo = e;
      }

      double f;
      double g;
      if (bl2) {
         f = vec3.y();
         g = player.getY() + clientboundPlayerPositionPacket.getY();
         player.yOld += clientboundPlayerPositionPacket.getY();
         player.yo += clientboundPlayerPositionPacket.getY();
      } else {
         f = 0.0D;
         g = clientboundPlayerPositionPacket.getY();
         player.yOld = g;
         player.yo = g;
      }

      double h;
      double i;
      if (bl3) {
         h = vec3.z();
         i = player.getZ() + clientboundPlayerPositionPacket.getZ();
         player.zOld += clientboundPlayerPositionPacket.getZ();
         player.zo += clientboundPlayerPositionPacket.getZ();
      } else {
         h = 0.0D;
         i = clientboundPlayerPositionPacket.getZ();
         player.zOld = i;
         player.zo = i;
      }

      player.setPos(e, g, i);
      player.setDeltaMovement(d, f, h);
      float j = clientboundPlayerPositionPacket.getYRot();
      float k = clientboundPlayerPositionPacket.getXRot();
      if (clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.X_ROT)) {
         player.setXRot(player.getXRot() + k);
         player.xRotO += k;
      } else {
         player.setXRot(k);
         player.xRotO = k;
      }

      if (clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.Y_ROT)) {
         player.setYRot(player.getYRot() + j);
         player.yRotO += j;
      } else {
         player.setYRot(j);
         player.yRotO = j;
      }

      this.connection.send(new ServerboundAcceptTeleportationPacket(clientboundPlayerPositionPacket.getId()));
      this.connection.send(new PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false));
   }

   public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket clientboundSectionBlocksUpdatePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSectionBlocksUpdatePacket, this, this.minecraft);
      clientboundSectionBlocksUpdatePacket.runUpdates((blockPos, blockState) -> {
         this.level.setServerVerifiedBlockState(blockPos, blockState, 19);
      });
   }

   public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundLevelChunkWithLightPacket, this, this.minecraft);
      int i = clientboundLevelChunkWithLightPacket.getX();
      int j = clientboundLevelChunkWithLightPacket.getZ();
      this.updateLevelChunk(i, j, clientboundLevelChunkWithLightPacket.getChunkData());
      ClientboundLightUpdatePacketData clientboundLightUpdatePacketData = clientboundLevelChunkWithLightPacket.getLightData();
      this.level.queueLightUpdate(() -> {
         this.applyLightData(i, j, clientboundLightUpdatePacketData);
         LevelChunk levelChunk = this.level.getChunkSource().getChunk(i, j, false);
         if (levelChunk != null) {
            this.enableChunkLight(levelChunk, i, j);
         }

      });
   }

   public void handleChunksBiomes(ClientboundChunksBiomesPacket clientboundChunksBiomesPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundChunksBiomesPacket, this, this.minecraft);
      Iterator var2 = clientboundChunksBiomesPacket.chunkBiomeData().iterator();

      ChunkBiomeData chunkBiomeData;
      while(var2.hasNext()) {
         chunkBiomeData = (ChunkBiomeData)var2.next();
         this.level.getChunkSource().replaceBiomes(chunkBiomeData.pos().x, chunkBiomeData.pos().z, chunkBiomeData.getReadBuffer());
      }

      var2 = clientboundChunksBiomesPacket.chunkBiomeData().iterator();

      while(var2.hasNext()) {
         chunkBiomeData = (ChunkBiomeData)var2.next();
         this.level.onChunkLoaded(new ChunkPos(chunkBiomeData.pos().x, chunkBiomeData.pos().z));
      }

      var2 = clientboundChunksBiomesPacket.chunkBiomeData().iterator();

      while(var2.hasNext()) {
         chunkBiomeData = (ChunkBiomeData)var2.next();

         for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
               for(int k = this.level.getMinSection(); k < this.level.getMaxSection(); ++k) {
                  this.minecraft.levelRenderer.setSectionDirty(chunkBiomeData.pos().x + i, k, chunkBiomeData.pos().z + j);
               }
            }
         }
      }

   }

   private void updateLevelChunk(int i, int j, ClientboundLevelChunkPacketData clientboundLevelChunkPacketData) {
      this.level.getChunkSource().replaceWithPacketData(i, j, clientboundLevelChunkPacketData.getReadBuffer(), clientboundLevelChunkPacketData.getHeightmaps(), clientboundLevelChunkPacketData.getBlockEntitiesTagsConsumer(i, j));
   }

   private void enableChunkLight(LevelChunk levelChunk, int i, int j) {
      LevelLightEngine levelLightEngine = this.level.getChunkSource().getLightEngine();
      LevelChunkSection[] levelChunkSections = levelChunk.getSections();
      ChunkPos chunkPos = levelChunk.getPos();

      for(int k = 0; k < levelChunkSections.length; ++k) {
         LevelChunkSection levelChunkSection = levelChunkSections[k];
         int l = this.level.getSectionYFromSectionIndex(k);
         levelLightEngine.updateSectionStatus(SectionPos.of(chunkPos, l), levelChunkSection.hasOnlyAir());
         this.level.setSectionDirtyWithNeighbors(i, l, j);
      }

   }

   public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket clientboundForgetLevelChunkPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundForgetLevelChunkPacket, this, this.minecraft);
      this.level.getChunkSource().drop(clientboundForgetLevelChunkPacket.pos());
      this.queueLightRemoval(clientboundForgetLevelChunkPacket);
   }

   private void queueLightRemoval(ClientboundForgetLevelChunkPacket clientboundForgetLevelChunkPacket) {
      ChunkPos chunkPos = clientboundForgetLevelChunkPacket.pos();
      this.level.queueLightUpdate(() -> {
         LevelLightEngine levelLightEngine = this.level.getLightEngine();
         levelLightEngine.setLightEnabled(chunkPos, false);

         int i;
         for(i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); ++i) {
            SectionPos sectionPos = SectionPos.of(chunkPos, i);
            levelLightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, (DataLayer)null);
            levelLightEngine.queueSectionData(LightLayer.SKY, sectionPos, (DataLayer)null);
         }

         for(i = this.level.getMinSection(); i < this.level.getMaxSection(); ++i) {
            levelLightEngine.updateSectionStatus(SectionPos.of(chunkPos, i), true);
         }

      });
   }

   public void handleBlockUpdate(ClientboundBlockUpdatePacket clientboundBlockUpdatePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundBlockUpdatePacket, this, this.minecraft);
      this.level.setServerVerifiedBlockState(clientboundBlockUpdatePacket.getPos(), clientboundBlockUpdatePacket.getBlockState(), 19);
   }

   public void handleConfigurationStart(ClientboundStartConfigurationPacket clientboundStartConfigurationPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundStartConfigurationPacket, this, this.minecraft);
      this.minecraft.getChatListener().clearQueue();
      this.sendChatAcknowledgement();
      ChatComponent.State state = this.minecraft.gui.getChat().storeState();
      this.minecraft.clearClientLevel(new ServerReconfigScreen(RECONFIGURE_SCREEN_MESSAGE, this.connection));
      this.connection.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND, new ClientConfigurationPacketListenerImpl(this.minecraft, this.connection, new CommonListenerCookie(this.localGameProfile, this.telemetryManager, this.registryAccess, this.enabledFeatures, this.serverBrand, this.serverData, this.postDisconnectScreen, this.serverCookies, state, this.strictErrorHandling, this.customReportDetails, this.serverLinks)));
      this.send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
      this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
   }

   public void handleTakeItemEntity(ClientboundTakeItemEntityPacket clientboundTakeItemEntityPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundTakeItemEntityPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundTakeItemEntityPacket.getItemId());
      LivingEntity livingEntity = (LivingEntity)this.level.getEntity(clientboundTakeItemEntityPacket.getPlayerId());
      if (livingEntity == null) {
         livingEntity = this.minecraft.player;
      }

      if (entity != null) {
         if (entity instanceof ExperienceOrb) {
            this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.35F + 0.9F, false);
         } else {
            this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F, false);
         }

         this.minecraft.particleEngine.add(new ItemPickupParticle(this.minecraft.getEntityRenderDispatcher(), this.minecraft.renderBuffers(), this.level, entity, (Entity)livingEntity));
         if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity)entity;
            ItemStack itemStack = itemEntity.getItem();
            if (!itemStack.isEmpty()) {
               itemStack.shrink(clientboundTakeItemEntityPacket.getAmount());
            }

            if (itemStack.isEmpty()) {
               this.level.removeEntity(clientboundTakeItemEntityPacket.getItemId(), RemovalReason.DISCARDED);
            }
         } else if (!(entity instanceof ExperienceOrb)) {
            this.level.removeEntity(clientboundTakeItemEntityPacket.getItemId(), RemovalReason.DISCARDED);
         }
      }

   }

   public void handleSystemChat(ClientboundSystemChatPacket clientboundSystemChatPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSystemChatPacket, this, this.minecraft);
      this.minecraft.getChatListener().handleSystemMessage(clientboundSystemChatPacket.content(), clientboundSystemChatPacket.overlay());
   }

   public void handlePlayerChat(ClientboundPlayerChatPacket clientboundPlayerChatPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPlayerChatPacket, this, this.minecraft);
      Optional<SignedMessageBody> optional = clientboundPlayerChatPacket.body().unpack(this.messageSignatureCache);
      if (optional.isEmpty()) {
         this.connection.disconnect(INVALID_PACKET);
      } else {
         this.messageSignatureCache.push((SignedMessageBody)optional.get(), clientboundPlayerChatPacket.signature());
         UUID uUID = clientboundPlayerChatPacket.sender();
         PlayerInfo playerInfo = this.getPlayerInfo(uUID);
         if (playerInfo == null) {
            LOGGER.error("Received player chat packet for unknown player with ID: {}", uUID);
            this.minecraft.getChatListener().handleChatMessageError(uUID, clientboundPlayerChatPacket.chatType());
         } else {
            RemoteChatSession remoteChatSession = playerInfo.getChatSession();
            SignedMessageLink signedMessageLink;
            if (remoteChatSession != null) {
               signedMessageLink = new SignedMessageLink(clientboundPlayerChatPacket.index(), uUID, remoteChatSession.sessionId());
            } else {
               signedMessageLink = SignedMessageLink.unsigned(uUID);
            }

            PlayerChatMessage playerChatMessage = new PlayerChatMessage(signedMessageLink, clientboundPlayerChatPacket.signature(), (SignedMessageBody)optional.get(), clientboundPlayerChatPacket.unsignedContent(), clientboundPlayerChatPacket.filterMask());
            playerChatMessage = playerInfo.getMessageValidator().updateAndValidate(playerChatMessage);
            if (playerChatMessage != null) {
               this.minecraft.getChatListener().handlePlayerChatMessage(playerChatMessage, playerInfo.getProfile(), clientboundPlayerChatPacket.chatType());
            } else {
               this.minecraft.getChatListener().handleChatMessageError(uUID, clientboundPlayerChatPacket.chatType());
            }

         }
      }
   }

   public void handleDisguisedChat(ClientboundDisguisedChatPacket clientboundDisguisedChatPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundDisguisedChatPacket, this, this.minecraft);
      this.minecraft.getChatListener().handleDisguisedChatMessage(clientboundDisguisedChatPacket.message(), clientboundDisguisedChatPacket.chatType());
   }

   public void handleDeleteChat(ClientboundDeleteChatPacket clientboundDeleteChatPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundDeleteChatPacket, this, this.minecraft);
      Optional<MessageSignature> optional = clientboundDeleteChatPacket.messageSignature().unpack(this.messageSignatureCache);
      if (optional.isEmpty()) {
         this.connection.disconnect(INVALID_PACKET);
      } else {
         this.lastSeenMessages.ignorePending((MessageSignature)optional.get());
         if (!this.minecraft.getChatListener().removeFromDelayedMessageQueue((MessageSignature)optional.get())) {
            this.minecraft.gui.getChat().deleteMessage((MessageSignature)optional.get());
         }

      }
   }

   public void handleAnimate(ClientboundAnimatePacket clientboundAnimatePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundAnimatePacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundAnimatePacket.getId());
      if (entity != null) {
         LivingEntity livingEntity;
         if (clientboundAnimatePacket.getAction() == 0) {
            livingEntity = (LivingEntity)entity;
            livingEntity.swing(InteractionHand.MAIN_HAND);
         } else if (clientboundAnimatePacket.getAction() == 3) {
            livingEntity = (LivingEntity)entity;
            livingEntity.swing(InteractionHand.OFF_HAND);
         } else if (clientboundAnimatePacket.getAction() == 2) {
            Player player = (Player)entity;
            player.stopSleepInBed(false, false);
         } else if (clientboundAnimatePacket.getAction() == 4) {
            this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
         } else if (clientboundAnimatePacket.getAction() == 5) {
            this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
         }

      }
   }

   public void handleHurtAnimation(ClientboundHurtAnimationPacket clientboundHurtAnimationPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundHurtAnimationPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundHurtAnimationPacket.id());
      if (entity != null) {
         entity.animateHurt(clientboundHurtAnimationPacket.yaw());
      }
   }

   public void handleSetTime(ClientboundSetTimePacket clientboundSetTimePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetTimePacket, this, this.minecraft);
      this.minecraft.level.setGameTime(clientboundSetTimePacket.getGameTime());
      this.minecraft.level.setDayTime(clientboundSetTimePacket.getDayTime());
      this.telemetryManager.setTime(clientboundSetTimePacket.getGameTime());
   }

   public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket clientboundSetDefaultSpawnPositionPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetDefaultSpawnPositionPacket, this, this.minecraft);
      this.minecraft.level.setDefaultSpawnPos(clientboundSetDefaultSpawnPositionPacket.getPos(), clientboundSetDefaultSpawnPositionPacket.getAngle());
   }

   public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket clientboundSetPassengersPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetPassengersPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundSetPassengersPacket.getVehicle());
      if (entity == null) {
         LOGGER.warn("Received passengers for unknown entity");
      } else {
         boolean bl = entity.hasIndirectPassenger(this.minecraft.player);
         entity.ejectPassengers();
         int[] var4 = clientboundSetPassengersPacket.getPassengers();
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            int i = var4[var6];
            Entity entity2 = this.level.getEntity(i);
            if (entity2 != null) {
               entity2.startRiding(entity, true);
               if (entity2 == this.minecraft.player && !bl) {
                  if (entity instanceof Boat) {
                     this.minecraft.player.yRotO = entity.getYRot();
                     this.minecraft.player.setYRot(entity.getYRot());
                     this.minecraft.player.setYHeadRot(entity.getYRot());
                  }

                  Component component = Component.translatable("mount.onboard", new Object[]{this.minecraft.options.keyShift.getTranslatedKeyMessage()});
                  this.minecraft.gui.setOverlayMessage(component, false);
                  this.minecraft.getNarrator().sayNow((Component)component);
               }
            }
         }

      }
   }

   public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket clientboundSetEntityLinkPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetEntityLinkPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundSetEntityLinkPacket.getSourceId());
      if (entity instanceof Leashable) {
         Leashable leashable = (Leashable)entity;
         leashable.setDelayedLeashHolderId(clientboundSetEntityLinkPacket.getDestId());
      }

   }

   private static ItemStack findTotem(Player player) {
      InteractionHand[] var1 = InteractionHand.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         InteractionHand interactionHand = var1[var3];
         ItemStack itemStack = player.getItemInHand(interactionHand);
         if (itemStack.is(Items.TOTEM_OF_UNDYING)) {
            return itemStack;
         }
      }

      return new ItemStack(Items.TOTEM_OF_UNDYING);
   }

   public void handleEntityEvent(ClientboundEntityEventPacket clientboundEntityEventPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundEntityEventPacket, this, this.minecraft);
      Entity entity = clientboundEntityEventPacket.getEntity(this.level);
      if (entity != null) {
         switch(clientboundEntityEventPacket.getEventId()) {
         case 21:
            this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)entity));
            break;
         case 35:
            int i = true;
            this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
            this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, entity.getSoundSource(), 1.0F, 1.0F, false);
            if (entity == this.minecraft.player) {
               this.minecraft.gameRenderer.displayItemActivation(findTotem(this.minecraft.player));
            }
            break;
         case 63:
            this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)entity));
            break;
         default:
            entity.handleEntityEvent(clientboundEntityEventPacket.getEventId());
         }
      }

   }

   public void handleDamageEvent(ClientboundDamageEventPacket clientboundDamageEventPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundDamageEventPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundDamageEventPacket.entityId());
      if (entity != null) {
         entity.handleDamageEvent(clientboundDamageEventPacket.getSource(this.level));
      }
   }

   public void handleSetHealth(ClientboundSetHealthPacket clientboundSetHealthPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetHealthPacket, this, this.minecraft);
      this.minecraft.player.hurtTo(clientboundSetHealthPacket.getHealth());
      this.minecraft.player.getFoodData().setFoodLevel(clientboundSetHealthPacket.getFood());
      this.minecraft.player.getFoodData().setSaturation(clientboundSetHealthPacket.getSaturation());
   }

   public void handleSetExperience(ClientboundSetExperiencePacket clientboundSetExperiencePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetExperiencePacket, this, this.minecraft);
      this.minecraft.player.setExperienceValues(clientboundSetExperiencePacket.getExperienceProgress(), clientboundSetExperiencePacket.getTotalExperience(), clientboundSetExperiencePacket.getExperienceLevel());
   }

   public void handleRespawn(ClientboundRespawnPacket clientboundRespawnPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundRespawnPacket, this, this.minecraft);
      CommonPlayerSpawnInfo commonPlayerSpawnInfo = clientboundRespawnPacket.commonPlayerSpawnInfo();
      ResourceKey<Level> resourceKey = commonPlayerSpawnInfo.dimension();
      Holder<DimensionType> holder = commonPlayerSpawnInfo.dimensionType();
      LocalPlayer localPlayer = this.minecraft.player;
      ResourceKey<Level> resourceKey2 = localPlayer.level().dimension();
      boolean bl = resourceKey != resourceKey2;
      ReceivingLevelScreen.Reason reason = this.determineLevelLoadingReason(localPlayer.isDeadOrDying(), resourceKey, resourceKey2);
      if (bl) {
         Map<MapId, MapItemSavedData> map = this.level.getAllMapData();
         boolean bl2 = commonPlayerSpawnInfo.isDebug();
         boolean bl3 = commonPlayerSpawnInfo.isFlat();
         ClientLevel.ClientLevelData clientLevelData = new ClientLevel.ClientLevelData(this.levelData.getDifficulty(), this.levelData.isHardcore(), bl3);
         this.levelData = clientLevelData;
         int var10007 = this.serverChunkRadius;
         int var10008 = this.serverSimulationDistance;
         Minecraft var10009 = this.minecraft;
         Objects.requireNonNull(var10009);
         this.level = new ClientLevel(this, clientLevelData, resourceKey, holder, var10007, var10008, var10009::getProfiler, this.minecraft.levelRenderer, bl2, commonPlayerSpawnInfo.seed());
         this.level.addMapData(map);
         this.minecraft.setLevel(this.level, reason);
      }

      this.minecraft.cameraEntity = null;
      if (localPlayer.hasContainerOpen()) {
         localPlayer.closeContainer();
      }

      LocalPlayer localPlayer2;
      if (clientboundRespawnPacket.shouldKeep((byte)2)) {
         localPlayer2 = this.minecraft.gameMode.createPlayer(this.level, localPlayer.getStats(), localPlayer.getRecipeBook(), localPlayer.isShiftKeyDown(), localPlayer.isSprinting());
      } else {
         localPlayer2 = this.minecraft.gameMode.createPlayer(this.level, localPlayer.getStats(), localPlayer.getRecipeBook());
      }

      this.startWaitingForNewLevel(localPlayer2, this.level, reason);
      localPlayer2.setId(localPlayer.getId());
      this.minecraft.player = localPlayer2;
      if (bl) {
         this.minecraft.getMusicManager().stopPlaying();
      }

      this.minecraft.cameraEntity = localPlayer2;
      if (clientboundRespawnPacket.shouldKeep((byte)2)) {
         List<DataValue<?>> list = localPlayer.getEntityData().getNonDefaultValues();
         if (list != null) {
            localPlayer2.getEntityData().assignValues(list);
         }
      }

      if (clientboundRespawnPacket.shouldKeep((byte)1)) {
         localPlayer2.getAttributes().assignAllValues(localPlayer.getAttributes());
      } else {
         localPlayer2.getAttributes().assignBaseValues(localPlayer.getAttributes());
      }

      localPlayer2.resetPos();
      this.level.addEntity(localPlayer2);
      localPlayer2.setYRot(-180.0F);
      localPlayer2.input = new KeyboardInput(this.minecraft.options);
      this.minecraft.gameMode.adjustPlayer(localPlayer2);
      localPlayer2.setReducedDebugInfo(localPlayer.isReducedDebugInfo());
      localPlayer2.setShowDeathScreen(localPlayer.shouldShowDeathScreen());
      localPlayer2.setLastDeathLocation(commonPlayerSpawnInfo.lastDeathLocation());
      localPlayer2.setPortalCooldown(commonPlayerSpawnInfo.portalCooldown());
      localPlayer2.spinningEffectIntensity = localPlayer.spinningEffectIntensity;
      localPlayer2.oSpinningEffectIntensity = localPlayer.oSpinningEffectIntensity;
      if (this.minecraft.screen instanceof DeathScreen || this.minecraft.screen instanceof DeathScreen.TitleConfirmScreen) {
         this.minecraft.setScreen((Screen)null);
      }

      this.minecraft.gameMode.setLocalMode(commonPlayerSpawnInfo.gameType(), commonPlayerSpawnInfo.previousGameType());
   }

   private ReceivingLevelScreen.Reason determineLevelLoadingReason(boolean bl, ResourceKey<Level> resourceKey, ResourceKey<Level> resourceKey2) {
      ReceivingLevelScreen.Reason reason = ReceivingLevelScreen.Reason.OTHER;
      if (!bl) {
         if (resourceKey != Level.NETHER && resourceKey2 != Level.NETHER) {
            if (resourceKey == Level.END || resourceKey2 == Level.END) {
               reason = ReceivingLevelScreen.Reason.END_PORTAL;
            }
         } else {
            reason = ReceivingLevelScreen.Reason.NETHER_PORTAL;
         }
      }

      return reason;
   }

   public void handleExplosion(ClientboundExplodePacket clientboundExplodePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundExplodePacket, this, this.minecraft);
      Explosion explosion = new Explosion(this.minecraft.level, (Entity)null, clientboundExplodePacket.getX(), clientboundExplodePacket.getY(), clientboundExplodePacket.getZ(), clientboundExplodePacket.getPower(), clientboundExplodePacket.getToBlow(), clientboundExplodePacket.getBlockInteraction(), clientboundExplodePacket.getSmallExplosionParticles(), clientboundExplodePacket.getLargeExplosionParticles(), clientboundExplodePacket.getExplosionSound());
      explosion.finalizeExplosion(true);
      this.minecraft.player.setDeltaMovement(this.minecraft.player.getDeltaMovement().add((double)clientboundExplodePacket.getKnockbackX(), (double)clientboundExplodePacket.getKnockbackY(), (double)clientboundExplodePacket.getKnockbackZ()));
   }

   public void handleHorseScreenOpen(ClientboundHorseScreenOpenPacket clientboundHorseScreenOpenPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundHorseScreenOpenPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundHorseScreenOpenPacket.getEntityId());
      if (entity instanceof AbstractHorse) {
         AbstractHorse abstractHorse = (AbstractHorse)entity;
         LocalPlayer localPlayer = this.minecraft.player;
         int i = clientboundHorseScreenOpenPacket.getInventoryColumns();
         SimpleContainer simpleContainer = new SimpleContainer(AbstractHorse.getInventorySize(i));
         HorseInventoryMenu horseInventoryMenu = new HorseInventoryMenu(clientboundHorseScreenOpenPacket.getContainerId(), localPlayer.getInventory(), simpleContainer, abstractHorse, i);
         localPlayer.containerMenu = horseInventoryMenu;
         this.minecraft.setScreen(new HorseInventoryScreen(horseInventoryMenu, localPlayer.getInventory(), abstractHorse, i));
      }

   }

   public void handleOpenScreen(ClientboundOpenScreenPacket clientboundOpenScreenPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundOpenScreenPacket, this, this.minecraft);
      MenuScreens.create(clientboundOpenScreenPacket.getType(), this.minecraft, clientboundOpenScreenPacket.getContainerId(), clientboundOpenScreenPacket.getTitle());
   }

   public void handleContainerSetSlot(ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundContainerSetSlotPacket, this, this.minecraft);
      Player player = this.minecraft.player;
      ItemStack itemStack = clientboundContainerSetSlotPacket.getItem();
      int i = clientboundContainerSetSlotPacket.getSlot();
      this.minecraft.getTutorial().onGetItem(itemStack);
      if (clientboundContainerSetSlotPacket.getContainerId() == -1) {
         if (!(this.minecraft.screen instanceof CreativeModeInventoryScreen)) {
            player.containerMenu.setCarried(itemStack);
         }
      } else if (clientboundContainerSetSlotPacket.getContainerId() == -2) {
         player.getInventory().setItem(i, itemStack);
      } else {
         boolean bl = false;
         Screen var7 = this.minecraft.screen;
         if (var7 instanceof CreativeModeInventoryScreen) {
            CreativeModeInventoryScreen creativeModeInventoryScreen = (CreativeModeInventoryScreen)var7;
            bl = !creativeModeInventoryScreen.isInventoryOpen();
         }

         if (clientboundContainerSetSlotPacket.getContainerId() == 0 && InventoryMenu.isHotbarSlot(i)) {
            if (!itemStack.isEmpty()) {
               ItemStack itemStack2 = player.inventoryMenu.getSlot(i).getItem();
               if (itemStack2.isEmpty() || itemStack2.getCount() < itemStack.getCount()) {
                  itemStack.setPopTime(5);
               }
            }

            player.inventoryMenu.setItem(i, clientboundContainerSetSlotPacket.getStateId(), itemStack);
         } else if (clientboundContainerSetSlotPacket.getContainerId() == player.containerMenu.containerId && (clientboundContainerSetSlotPacket.getContainerId() != 0 || !bl)) {
            player.containerMenu.setItem(i, clientboundContainerSetSlotPacket.getStateId(), itemStack);
         }
      }

   }

   public void handleContainerContent(ClientboundContainerSetContentPacket clientboundContainerSetContentPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundContainerSetContentPacket, this, this.minecraft);
      Player player = this.minecraft.player;
      if (clientboundContainerSetContentPacket.getContainerId() == 0) {
         player.inventoryMenu.initializeContents(clientboundContainerSetContentPacket.getStateId(), clientboundContainerSetContentPacket.getItems(), clientboundContainerSetContentPacket.getCarriedItem());
      } else if (clientboundContainerSetContentPacket.getContainerId() == player.containerMenu.containerId) {
         player.containerMenu.initializeContents(clientboundContainerSetContentPacket.getStateId(), clientboundContainerSetContentPacket.getItems(), clientboundContainerSetContentPacket.getCarriedItem());
      }

   }

   public void handleOpenSignEditor(ClientboundOpenSignEditorPacket clientboundOpenSignEditorPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundOpenSignEditorPacket, this, this.minecraft);
      BlockPos blockPos = clientboundOpenSignEditorPacket.getPos();
      BlockEntity var4 = this.level.getBlockEntity(blockPos);
      if (var4 instanceof SignBlockEntity) {
         SignBlockEntity signBlockEntity = (SignBlockEntity)var4;
         this.minecraft.player.openTextEdit(signBlockEntity, clientboundOpenSignEditorPacket.isFrontText());
      } else {
         BlockState blockState = this.level.getBlockState(blockPos);
         SignBlockEntity signBlockEntity2 = new SignBlockEntity(blockPos, blockState);
         signBlockEntity2.setLevel(this.level);
         this.minecraft.player.openTextEdit(signBlockEntity2, clientboundOpenSignEditorPacket.isFrontText());
      }

   }

   public void handleBlockEntityData(ClientboundBlockEntityDataPacket clientboundBlockEntityDataPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundBlockEntityDataPacket, this, this.minecraft);
      BlockPos blockPos = clientboundBlockEntityDataPacket.getPos();
      this.minecraft.level.getBlockEntity(blockPos, clientboundBlockEntityDataPacket.getType()).ifPresent((blockEntity) -> {
         CompoundTag compoundTag = clientboundBlockEntityDataPacket.getTag();
         if (!compoundTag.isEmpty()) {
            blockEntity.loadWithComponents(compoundTag, this.registryAccess);
         }

         if (blockEntity instanceof CommandBlockEntity && this.minecraft.screen instanceof CommandBlockEditScreen) {
            ((CommandBlockEditScreen)this.minecraft.screen).updateGui();
         }

      });
   }

   public void handleContainerSetData(ClientboundContainerSetDataPacket clientboundContainerSetDataPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundContainerSetDataPacket, this, this.minecraft);
      Player player = this.minecraft.player;
      if (player.containerMenu != null && player.containerMenu.containerId == clientboundContainerSetDataPacket.getContainerId()) {
         player.containerMenu.setData(clientboundContainerSetDataPacket.getId(), clientboundContainerSetDataPacket.getValue());
      }

   }

   public void handleSetEquipment(ClientboundSetEquipmentPacket clientboundSetEquipmentPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetEquipmentPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundSetEquipmentPacket.getEntity());
      if (entity instanceof LivingEntity) {
         LivingEntity livingEntity = (LivingEntity)entity;
         clientboundSetEquipmentPacket.getSlots().forEach((pair) -> {
            livingEntity.setItemSlot((EquipmentSlot)pair.getFirst(), (ItemStack)pair.getSecond());
         });
      }

   }

   public void handleContainerClose(ClientboundContainerClosePacket clientboundContainerClosePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundContainerClosePacket, this, this.minecraft);
      this.minecraft.player.clientSideCloseContainer();
   }

   public void handleBlockEvent(ClientboundBlockEventPacket clientboundBlockEventPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundBlockEventPacket, this, this.minecraft);
      this.minecraft.level.blockEvent(clientboundBlockEventPacket.getPos(), clientboundBlockEventPacket.getBlock(), clientboundBlockEventPacket.getB0(), clientboundBlockEventPacket.getB1());
   }

   public void handleBlockDestruction(ClientboundBlockDestructionPacket clientboundBlockDestructionPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundBlockDestructionPacket, this, this.minecraft);
      this.minecraft.level.destroyBlockProgress(clientboundBlockDestructionPacket.getId(), clientboundBlockDestructionPacket.getPos(), clientboundBlockDestructionPacket.getProgress());
   }

   public void handleGameEvent(ClientboundGameEventPacket clientboundGameEventPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundGameEventPacket, this, this.minecraft);
      Player player = this.minecraft.player;
      Type type = clientboundGameEventPacket.getEvent();
      float f = clientboundGameEventPacket.getParam();
      int i = Mth.floor(f + 0.5F);
      if (type == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
         player.displayClientMessage(Component.translatable("block.minecraft.spawn.not_valid"), false);
      } else if (type == ClientboundGameEventPacket.START_RAINING) {
         this.level.getLevelData().setRaining(true);
         this.level.setRainLevel(0.0F);
      } else if (type == ClientboundGameEventPacket.STOP_RAINING) {
         this.level.getLevelData().setRaining(false);
         this.level.setRainLevel(1.0F);
      } else if (type == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
         this.minecraft.gameMode.setLocalMode(GameType.byId(i));
      } else if (type == ClientboundGameEventPacket.WIN_GAME) {
         this.minecraft.setScreen(new WinScreen(true, () -> {
            this.minecraft.player.connection.send(new ServerboundClientCommandPacket(Action.PERFORM_RESPAWN));
            this.minecraft.setScreen((Screen)null);
         }));
      } else if (type == ClientboundGameEventPacket.DEMO_EVENT) {
         Options options = this.minecraft.options;
         if (f == 0.0F) {
            this.minecraft.setScreen(new DemoIntroScreen());
         } else if (f == 101.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.movement", new Object[]{options.keyUp.getTranslatedKeyMessage(), options.keyLeft.getTranslatedKeyMessage(), options.keyDown.getTranslatedKeyMessage(), options.keyRight.getTranslatedKeyMessage()}));
         } else if (f == 102.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.jump", new Object[]{options.keyJump.getTranslatedKeyMessage()}));
         } else if (f == 103.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.inventory", new Object[]{options.keyInventory.getTranslatedKeyMessage()}));
         } else if (f == 104.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.day.6", new Object[]{options.keyScreenshot.getTranslatedKeyMessage()}));
         }
      } else if (type == ClientboundGameEventPacket.ARROW_HIT_PLAYER) {
         this.level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18F, 0.45F);
      } else if (type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
         this.level.setRainLevel(f);
      } else if (type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
         this.level.setThunderLevel(f);
      } else if (type == ClientboundGameEventPacket.PUFFER_FISH_STING) {
         this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0F, 1.0F);
      } else if (type == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
         this.level.addParticle(ParticleTypes.ELDER_GUARDIAN, player.getX(), player.getY(), player.getZ(), 0.0D, 0.0D, 0.0D);
         if (i == 1) {
            this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
         }
      } else if (type == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
         this.minecraft.player.setShowDeathScreen(f == 0.0F);
      } else if (type == ClientboundGameEventPacket.LIMITED_CRAFTING) {
         this.minecraft.player.setDoLimitedCrafting(f == 1.0F);
      } else if (type == ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START && this.levelLoadStatusManager != null) {
         this.levelLoadStatusManager.loadingPacketsReceived();
      }

   }

   private void startWaitingForNewLevel(LocalPlayer localPlayer, ClientLevel clientLevel, ReceivingLevelScreen.Reason reason) {
      this.levelLoadStatusManager = new LevelLoadStatusManager(localPlayer, clientLevel, this.minecraft.levelRenderer);
      Minecraft var10000 = this.minecraft;
      LevelLoadStatusManager var10003 = this.levelLoadStatusManager;
      Objects.requireNonNull(var10003);
      var10000.setScreen(new ReceivingLevelScreen(var10003::levelReady, reason));
   }

   public void handleMapItemData(ClientboundMapItemDataPacket clientboundMapItemDataPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundMapItemDataPacket, this, this.minecraft);
      MapRenderer mapRenderer = this.minecraft.gameRenderer.getMapRenderer();
      MapId mapId = clientboundMapItemDataPacket.mapId();
      MapItemSavedData mapItemSavedData = this.minecraft.level.getMapData(mapId);
      if (mapItemSavedData == null) {
         mapItemSavedData = MapItemSavedData.createForClient(clientboundMapItemDataPacket.scale(), clientboundMapItemDataPacket.locked(), this.minecraft.level.dimension());
         this.minecraft.level.overrideMapData(mapId, mapItemSavedData);
      }

      clientboundMapItemDataPacket.applyToMap(mapItemSavedData);
      mapRenderer.update(mapId, mapItemSavedData);
   }

   public void handleLevelEvent(ClientboundLevelEventPacket clientboundLevelEventPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundLevelEventPacket, this, this.minecraft);
      if (clientboundLevelEventPacket.isGlobalEvent()) {
         this.minecraft.level.globalLevelEvent(clientboundLevelEventPacket.getType(), clientboundLevelEventPacket.getPos(), clientboundLevelEventPacket.getData());
      } else {
         this.minecraft.level.levelEvent(clientboundLevelEventPacket.getType(), clientboundLevelEventPacket.getPos(), clientboundLevelEventPacket.getData());
      }

   }

   public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket clientboundUpdateAdvancementsPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundUpdateAdvancementsPacket, this, this.minecraft);
      this.advancements.update(clientboundUpdateAdvancementsPacket);
   }

   public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket clientboundSelectAdvancementsTabPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSelectAdvancementsTabPacket, this, this.minecraft);
      ResourceLocation resourceLocation = clientboundSelectAdvancementsTabPacket.getTab();
      if (resourceLocation == null) {
         this.advancements.setSelectedTab((AdvancementHolder)null, false);
      } else {
         AdvancementHolder advancementHolder = this.advancements.get(resourceLocation);
         this.advancements.setSelectedTab(advancementHolder, false);
      }

   }

   public void handleCommands(ClientboundCommandsPacket clientboundCommandsPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundCommandsPacket, this, this.minecraft);
      this.commands = new CommandDispatcher(clientboundCommandsPacket.getRoot(CommandBuildContext.simple(this.registryAccess, this.enabledFeatures)));
   }

   public void handleStopSoundEvent(ClientboundStopSoundPacket clientboundStopSoundPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundStopSoundPacket, this, this.minecraft);
      this.minecraft.getSoundManager().stop(clientboundStopSoundPacket.getName(), clientboundStopSoundPacket.getSource());
   }

   public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket clientboundCommandSuggestionsPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundCommandSuggestionsPacket, this, this.minecraft);
      this.suggestionsProvider.completeCustomSuggestions(clientboundCommandSuggestionsPacket.id(), clientboundCommandSuggestionsPacket.toSuggestions());
   }

   public void handleUpdateRecipes(ClientboundUpdateRecipesPacket clientboundUpdateRecipesPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundUpdateRecipesPacket, this, this.minecraft);
      this.recipeManager.replaceRecipes(clientboundUpdateRecipesPacket.getRecipes());
      ClientRecipeBook clientRecipeBook = this.minecraft.player.getRecipeBook();
      clientRecipeBook.setupCollections(this.recipeManager.getOrderedRecipes(), this.minecraft.level.registryAccess());
      this.searchTrees.updateRecipes(clientRecipeBook, this.registryAccess);
   }

   public void handleLookAt(ClientboundPlayerLookAtPacket clientboundPlayerLookAtPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPlayerLookAtPacket, this, this.minecraft);
      Vec3 vec3 = clientboundPlayerLookAtPacket.getPosition(this.level);
      if (vec3 != null) {
         this.minecraft.player.lookAt(clientboundPlayerLookAtPacket.getFromAnchor(), vec3);
      }

   }

   public void handleTagQueryPacket(ClientboundTagQueryPacket clientboundTagQueryPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundTagQueryPacket, this, this.minecraft);
      if (!this.debugQueryHandler.handleResponse(clientboundTagQueryPacket.getTransactionId(), clientboundTagQueryPacket.getTag())) {
         LOGGER.debug("Got unhandled response to tag query {}", clientboundTagQueryPacket.getTransactionId());
      }

   }

   public void handleAwardStats(ClientboundAwardStatsPacket clientboundAwardStatsPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundAwardStatsPacket, this, this.minecraft);
      ObjectIterator var2 = clientboundAwardStatsPacket.stats().object2IntEntrySet().iterator();

      while(var2.hasNext()) {
         Entry<Stat<?>> entry = (Entry)var2.next();
         Stat<?> stat = (Stat)entry.getKey();
         int i = entry.getIntValue();
         this.minecraft.player.getStats().setValue(this.minecraft.player, stat, i);
      }

      Screen var7 = this.minecraft.screen;
      if (var7 instanceof StatsScreen) {
         StatsScreen statsScreen = (StatsScreen)var7;
         statsScreen.onStatsUpdated();
      }

   }

   public void handleAddOrRemoveRecipes(ClientboundRecipePacket clientboundRecipePacket) {
      ClientRecipeBook clientRecipeBook;
      PacketUtils.ensureRunningOnSameThread(clientboundRecipePacket, this, this.minecraft);
      clientRecipeBook = this.minecraft.player.getRecipeBook();
      clientRecipeBook.setBookSettings(clientboundRecipePacket.getBookSettings());
      State state = clientboundRecipePacket.getState();
      Optional var10000;
      Iterator var4;
      ResourceLocation resourceLocation;
      label45:
      switch(state) {
      case REMOVE:
         var4 = clientboundRecipePacket.getRecipes().iterator();

         while(true) {
            if (!var4.hasNext()) {
               break label45;
            }

            resourceLocation = (ResourceLocation)var4.next();
            var10000 = this.recipeManager.byKey(resourceLocation);
            Objects.requireNonNull(clientRecipeBook);
            var10000.ifPresent(clientRecipeBook::remove);
         }
      case INIT:
         var4 = clientboundRecipePacket.getRecipes().iterator();

         while(var4.hasNext()) {
            resourceLocation = (ResourceLocation)var4.next();
            var10000 = this.recipeManager.byKey(resourceLocation);
            Objects.requireNonNull(clientRecipeBook);
            var10000.ifPresent(clientRecipeBook::add);
         }

         var4 = clientboundRecipePacket.getHighlights().iterator();

         while(true) {
            if (!var4.hasNext()) {
               break label45;
            }

            resourceLocation = (ResourceLocation)var4.next();
            var10000 = this.recipeManager.byKey(resourceLocation);
            Objects.requireNonNull(clientRecipeBook);
            var10000.ifPresent(clientRecipeBook::addHighlight);
         }
      case ADD:
         var4 = clientboundRecipePacket.getRecipes().iterator();

         while(var4.hasNext()) {
            resourceLocation = (ResourceLocation)var4.next();
            this.recipeManager.byKey(resourceLocation).ifPresent((recipeHolder) -> {
               clientRecipeBook.add(recipeHolder);
               clientRecipeBook.addHighlight(recipeHolder);
               if (recipeHolder.value().showNotification()) {
                  RecipeToast.addOrUpdate(this.minecraft.getToasts(), recipeHolder);
               }

            });
         }
      }

      clientRecipeBook.getCollections().forEach((recipeCollection) -> {
         recipeCollection.updateKnownRecipes(clientRecipeBook);
      });
      if (this.minecraft.screen instanceof RecipeUpdateListener) {
         ((RecipeUpdateListener)this.minecraft.screen).recipesUpdated();
      }

   }

   public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket clientboundUpdateMobEffectPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundUpdateMobEffectPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundUpdateMobEffectPacket.getEntityId());
      if (entity instanceof LivingEntity) {
         Holder<MobEffect> holder = clientboundUpdateMobEffectPacket.getEffect();
         MobEffectInstance mobEffectInstance = new MobEffectInstance(holder, clientboundUpdateMobEffectPacket.getEffectDurationTicks(), clientboundUpdateMobEffectPacket.getEffectAmplifier(), clientboundUpdateMobEffectPacket.isEffectAmbient(), clientboundUpdateMobEffectPacket.isEffectVisible(), clientboundUpdateMobEffectPacket.effectShowsIcon(), (MobEffectInstance)null);
         if (!clientboundUpdateMobEffectPacket.shouldBlend()) {
            mobEffectInstance.skipBlending();
         }

         ((LivingEntity)entity).forceAddEffect(mobEffectInstance, (Entity)null);
      }
   }

   public void handleUpdateTags(ClientboundUpdateTagsPacket clientboundUpdateTagsPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundUpdateTagsPacket, this, this.minecraft);
      TagCollector tagCollector = new TagCollector();
      Map var10000 = clientboundUpdateTagsPacket.getTags();
      Objects.requireNonNull(tagCollector);
      var10000.forEach(tagCollector::append);
      tagCollector.updateTags(this.registryAccess, this.connection.isMemoryConnection());
      List<ItemStack> list = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
      this.searchTrees.updateCreativeTags(list);
   }

   public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket clientboundPlayerCombatEndPacket) {
   }

   public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket clientboundPlayerCombatEnterPacket) {
   }

   public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket clientboundPlayerCombatKillPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPlayerCombatKillPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundPlayerCombatKillPacket.playerId());
      if (entity == this.minecraft.player) {
         if (this.minecraft.player.shouldShowDeathScreen()) {
            this.minecraft.setScreen(new DeathScreen(clientboundPlayerCombatKillPacket.message(), this.level.getLevelData().isHardcore()));
         } else {
            this.minecraft.player.respawn();
         }
      }

   }

   public void handleChangeDifficulty(ClientboundChangeDifficultyPacket clientboundChangeDifficultyPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundChangeDifficultyPacket, this, this.minecraft);
      this.levelData.setDifficulty(clientboundChangeDifficultyPacket.getDifficulty());
      this.levelData.setDifficultyLocked(clientboundChangeDifficultyPacket.isLocked());
   }

   public void handleSetCamera(ClientboundSetCameraPacket clientboundSetCameraPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetCameraPacket, this, this.minecraft);
      Entity entity = clientboundSetCameraPacket.getEntity(this.level);
      if (entity != null) {
         this.minecraft.setCameraEntity(entity);
      }

   }

   public void handleInitializeBorder(ClientboundInitializeBorderPacket clientboundInitializeBorderPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundInitializeBorderPacket, this, this.minecraft);
      WorldBorder worldBorder = this.level.getWorldBorder();
      worldBorder.setCenter(clientboundInitializeBorderPacket.getNewCenterX(), clientboundInitializeBorderPacket.getNewCenterZ());
      long l = clientboundInitializeBorderPacket.getLerpTime();
      if (l > 0L) {
         worldBorder.lerpSizeBetween(clientboundInitializeBorderPacket.getOldSize(), clientboundInitializeBorderPacket.getNewSize(), l);
      } else {
         worldBorder.setSize(clientboundInitializeBorderPacket.getNewSize());
      }

      worldBorder.setAbsoluteMaxSize(clientboundInitializeBorderPacket.getNewAbsoluteMaxSize());
      worldBorder.setWarningBlocks(clientboundInitializeBorderPacket.getWarningBlocks());
      worldBorder.setWarningTime(clientboundInitializeBorderPacket.getWarningTime());
   }

   public void handleSetBorderCenter(ClientboundSetBorderCenterPacket clientboundSetBorderCenterPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetBorderCenterPacket, this, this.minecraft);
      this.level.getWorldBorder().setCenter(clientboundSetBorderCenterPacket.getNewCenterX(), clientboundSetBorderCenterPacket.getNewCenterZ());
   }

   public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket clientboundSetBorderLerpSizePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetBorderLerpSizePacket, this, this.minecraft);
      this.level.getWorldBorder().lerpSizeBetween(clientboundSetBorderLerpSizePacket.getOldSize(), clientboundSetBorderLerpSizePacket.getNewSize(), clientboundSetBorderLerpSizePacket.getLerpTime());
   }

   public void handleSetBorderSize(ClientboundSetBorderSizePacket clientboundSetBorderSizePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetBorderSizePacket, this, this.minecraft);
      this.level.getWorldBorder().setSize(clientboundSetBorderSizePacket.getSize());
   }

   public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket clientboundSetBorderWarningDistancePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetBorderWarningDistancePacket, this, this.minecraft);
      this.level.getWorldBorder().setWarningBlocks(clientboundSetBorderWarningDistancePacket.getWarningBlocks());
   }

   public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket clientboundSetBorderWarningDelayPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetBorderWarningDelayPacket, this, this.minecraft);
      this.level.getWorldBorder().setWarningTime(clientboundSetBorderWarningDelayPacket.getWarningDelay());
   }

   public void handleTitlesClear(ClientboundClearTitlesPacket clientboundClearTitlesPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundClearTitlesPacket, this, this.minecraft);
      this.minecraft.gui.clear();
      if (clientboundClearTitlesPacket.shouldResetTimes()) {
         this.minecraft.gui.resetTitleTimes();
      }

   }

   public void handleServerData(ClientboundServerDataPacket clientboundServerDataPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundServerDataPacket, this, this.minecraft);
      if (this.serverData != null) {
         this.serverData.motd = clientboundServerDataPacket.motd();
         Optional var10000 = clientboundServerDataPacket.iconBytes().map(ServerData::validateIcon);
         ServerData var10001 = this.serverData;
         Objects.requireNonNull(var10001);
         var10000.ifPresent(var10001::setIconBytes);
         ServerList.saveSingleServer(this.serverData);
      }
   }

   public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket clientboundCustomChatCompletionsPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundCustomChatCompletionsPacket, this, this.minecraft);
      this.suggestionsProvider.modifyCustomCompletions(clientboundCustomChatCompletionsPacket.action(), clientboundCustomChatCompletionsPacket.entries());
   }

   public void setActionBarText(ClientboundSetActionBarTextPacket clientboundSetActionBarTextPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetActionBarTextPacket, this, this.minecraft);
      this.minecraft.gui.setOverlayMessage(clientboundSetActionBarTextPacket.text(), false);
   }

   public void setTitleText(ClientboundSetTitleTextPacket clientboundSetTitleTextPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetTitleTextPacket, this, this.minecraft);
      this.minecraft.gui.setTitle(clientboundSetTitleTextPacket.text());
   }

   public void setSubtitleText(ClientboundSetSubtitleTextPacket clientboundSetSubtitleTextPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetSubtitleTextPacket, this, this.minecraft);
      this.minecraft.gui.setSubtitle(clientboundSetSubtitleTextPacket.text());
   }

   public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket clientboundSetTitlesAnimationPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetTitlesAnimationPacket, this, this.minecraft);
      this.minecraft.gui.setTimes(clientboundSetTitlesAnimationPacket.getFadeIn(), clientboundSetTitlesAnimationPacket.getStay(), clientboundSetTitlesAnimationPacket.getFadeOut());
   }

   public void handleTabListCustomisation(ClientboundTabListPacket clientboundTabListPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundTabListPacket, this, this.minecraft);
      this.minecraft.gui.getTabList().setHeader(clientboundTabListPacket.header().getString().isEmpty() ? null : clientboundTabListPacket.header());
      this.minecraft.gui.getTabList().setFooter(clientboundTabListPacket.footer().getString().isEmpty() ? null : clientboundTabListPacket.footer());
   }

   public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket clientboundRemoveMobEffectPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundRemoveMobEffectPacket, this, this.minecraft);
      Entity var3 = clientboundRemoveMobEffectPacket.getEntity(this.level);
      if (var3 instanceof LivingEntity) {
         LivingEntity livingEntity = (LivingEntity)var3;
         livingEntity.removeEffectNoUpdate(clientboundRemoveMobEffectPacket.effect());
      }

   }

   public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket clientboundPlayerInfoRemovePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPlayerInfoRemovePacket, this, this.minecraft);
      Iterator var2 = clientboundPlayerInfoRemovePacket.profileIds().iterator();

      while(var2.hasNext()) {
         UUID uUID = (UUID)var2.next();
         this.minecraft.getPlayerSocialManager().removePlayer(uUID);
         PlayerInfo playerInfo = (PlayerInfo)this.playerInfoMap.remove(uUID);
         if (playerInfo != null) {
            this.listedPlayers.remove(playerInfo);
         }
      }

   }

   public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket clientboundPlayerInfoUpdatePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPlayerInfoUpdatePacket, this, this.minecraft);
      Iterator var2 = clientboundPlayerInfoUpdatePacket.newEntries().iterator();

      net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry;
      PlayerInfo playerInfo;
      while(var2.hasNext()) {
         entry = (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry)var2.next();
         playerInfo = new PlayerInfo((GameProfile)Objects.requireNonNull(entry.profile()), this.enforcesSecureChat());
         if (this.playerInfoMap.putIfAbsent(entry.profileId(), playerInfo) == null) {
            this.minecraft.getPlayerSocialManager().addPlayer(playerInfo);
         }
      }

      var2 = clientboundPlayerInfoUpdatePacket.entries().iterator();

      while(true) {
         while(var2.hasNext()) {
            entry = (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry)var2.next();
            playerInfo = (PlayerInfo)this.playerInfoMap.get(entry.profileId());
            if (playerInfo == null) {
               LOGGER.warn("Ignoring player info update for unknown player {} ({})", entry.profileId(), clientboundPlayerInfoUpdatePacket.actions());
            } else {
               Iterator var5 = clientboundPlayerInfoUpdatePacket.actions().iterator();

               while(var5.hasNext()) {
                  net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action action = (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action)var5.next();
                  this.applyPlayerInfoUpdate(action, entry, playerInfo);
               }
            }
         }

         return;
      }
   }

   private void applyPlayerInfoUpdate(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action action, net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo) {
      switch(action) {
      case INITIALIZE_CHAT:
         this.initializeChatSession(entry, playerInfo);
         break;
      case UPDATE_GAME_MODE:
         if (playerInfo.getGameMode() != entry.gameMode() && this.minecraft.player != null && this.minecraft.player.getUUID().equals(entry.profileId())) {
            this.minecraft.player.onGameModeChanged(entry.gameMode());
         }

         playerInfo.setGameMode(entry.gameMode());
         break;
      case UPDATE_LISTED:
         if (entry.listed()) {
            this.listedPlayers.add(playerInfo);
         } else {
            this.listedPlayers.remove(playerInfo);
         }
         break;
      case UPDATE_LATENCY:
         playerInfo.setLatency(entry.latency());
         break;
      case UPDATE_DISPLAY_NAME:
         playerInfo.setTabListDisplayName(entry.displayName());
      }

   }

   private void initializeChatSession(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo) {
      GameProfile gameProfile = playerInfo.getProfile();
      SignatureValidator signatureValidator = this.minecraft.getProfileKeySignatureValidator();
      if (signatureValidator == null) {
         LOGGER.warn("Ignoring chat session from {} due to missing Services public key", gameProfile.getName());
         playerInfo.clearChatSession(this.enforcesSecureChat());
      } else {
         Data data = entry.chatSession();
         if (data != null) {
            try {
               RemoteChatSession remoteChatSession = data.validate(gameProfile, signatureValidator);
               playerInfo.setChatSession(remoteChatSession);
            } catch (ValidationException var7) {
               LOGGER.error("Failed to validate profile key for player: '{}'", gameProfile.getName(), var7);
               playerInfo.clearChatSession(this.enforcesSecureChat());
            }
         } else {
            playerInfo.clearChatSession(this.enforcesSecureChat());
         }

      }
   }

   private boolean enforcesSecureChat() {
      return this.minecraft.canValidateProfileKeys() && this.serverEnforcesSecureChat;
   }

   public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket clientboundPlayerAbilitiesPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPlayerAbilitiesPacket, this, this.minecraft);
      Player player = this.minecraft.player;
      player.getAbilities().flying = clientboundPlayerAbilitiesPacket.isFlying();
      player.getAbilities().instabuild = clientboundPlayerAbilitiesPacket.canInstabuild();
      player.getAbilities().invulnerable = clientboundPlayerAbilitiesPacket.isInvulnerable();
      player.getAbilities().mayfly = clientboundPlayerAbilitiesPacket.canFly();
      player.getAbilities().setFlyingSpeed(clientboundPlayerAbilitiesPacket.getFlyingSpeed());
      player.getAbilities().setWalkingSpeed(clientboundPlayerAbilitiesPacket.getWalkingSpeed());
   }

   public void handleSoundEvent(ClientboundSoundPacket clientboundSoundPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSoundPacket, this, this.minecraft);
      this.minecraft.level.playSeededSound(this.minecraft.player, clientboundSoundPacket.getX(), clientboundSoundPacket.getY(), clientboundSoundPacket.getZ(), clientboundSoundPacket.getSound(), clientboundSoundPacket.getSource(), clientboundSoundPacket.getVolume(), clientboundSoundPacket.getPitch(), clientboundSoundPacket.getSeed());
   }

   public void handleSoundEntityEvent(ClientboundSoundEntityPacket clientboundSoundEntityPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSoundEntityPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundSoundEntityPacket.getId());
      if (entity != null) {
         this.minecraft.level.playSeededSound(this.minecraft.player, entity, clientboundSoundEntityPacket.getSound(), clientboundSoundEntityPacket.getSource(), clientboundSoundEntityPacket.getVolume(), clientboundSoundEntityPacket.getPitch(), clientboundSoundEntityPacket.getSeed());
      }
   }

   public void handleBossUpdate(ClientboundBossEventPacket clientboundBossEventPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundBossEventPacket, this, this.minecraft);
      this.minecraft.gui.getBossOverlay().update(clientboundBossEventPacket);
   }

   public void handleItemCooldown(ClientboundCooldownPacket clientboundCooldownPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundCooldownPacket, this, this.minecraft);
      if (clientboundCooldownPacket.duration() == 0) {
         this.minecraft.player.getCooldowns().removeCooldown(clientboundCooldownPacket.item());
      } else {
         this.minecraft.player.getCooldowns().addCooldown(clientboundCooldownPacket.item(), clientboundCooldownPacket.duration());
      }

   }

   public void handleMoveVehicle(ClientboundMoveVehiclePacket clientboundMoveVehiclePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundMoveVehiclePacket, this, this.minecraft);
      Entity entity = this.minecraft.player.getRootVehicle();
      if (entity != this.minecraft.player && entity.isControlledByLocalInstance()) {
         entity.absMoveTo(clientboundMoveVehiclePacket.getX(), clientboundMoveVehiclePacket.getY(), clientboundMoveVehiclePacket.getZ(), clientboundMoveVehiclePacket.getYRot(), clientboundMoveVehiclePacket.getXRot());
         this.connection.send(new ServerboundMoveVehiclePacket(entity));
      }

   }

   public void handleOpenBook(ClientboundOpenBookPacket clientboundOpenBookPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundOpenBookPacket, this, this.minecraft);
      ItemStack itemStack = this.minecraft.player.getItemInHand(clientboundOpenBookPacket.getHand());
      BookViewScreen.BookAccess bookAccess = BookViewScreen.BookAccess.fromItem(itemStack);
      if (bookAccess != null) {
         this.minecraft.setScreen(new BookViewScreen(bookAccess));
      }

   }

   public void handleCustomPayload(CustomPacketPayload customPacketPayload) {
      if (customPacketPayload instanceof PathfindingDebugPayload) {
         PathfindingDebugPayload pathfindingDebugPayload = (PathfindingDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.pathfindingRenderer.addPath(pathfindingDebugPayload.entityId(), pathfindingDebugPayload.path(), pathfindingDebugPayload.maxNodeDistance());
      } else if (customPacketPayload instanceof NeighborUpdatesDebugPayload) {
         NeighborUpdatesDebugPayload neighborUpdatesDebugPayload = (NeighborUpdatesDebugPayload)customPacketPayload;
         ((NeighborsUpdateRenderer)this.minecraft.debugRenderer.neighborsUpdateRenderer).addUpdate(neighborUpdatesDebugPayload.time(), neighborUpdatesDebugPayload.pos());
      } else if (customPacketPayload instanceof StructuresDebugPayload) {
         StructuresDebugPayload structuresDebugPayload = (StructuresDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.structureRenderer.addBoundingBox(structuresDebugPayload.mainBB(), structuresDebugPayload.pieces(), structuresDebugPayload.dimension());
      } else if (customPacketPayload instanceof WorldGenAttemptDebugPayload) {
         WorldGenAttemptDebugPayload worldGenAttemptDebugPayload = (WorldGenAttemptDebugPayload)customPacketPayload;
         ((WorldGenAttemptRenderer)this.minecraft.debugRenderer.worldGenAttemptRenderer).addPos(worldGenAttemptDebugPayload.pos(), worldGenAttemptDebugPayload.scale(), worldGenAttemptDebugPayload.red(), worldGenAttemptDebugPayload.green(), worldGenAttemptDebugPayload.blue(), worldGenAttemptDebugPayload.alpha());
      } else if (customPacketPayload instanceof PoiTicketCountDebugPayload) {
         PoiTicketCountDebugPayload poiTicketCountDebugPayload = (PoiTicketCountDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.brainDebugRenderer.setFreeTicketCount(poiTicketCountDebugPayload.pos(), poiTicketCountDebugPayload.freeTicketCount());
      } else if (customPacketPayload instanceof PoiAddedDebugPayload) {
         PoiAddedDebugPayload poiAddedDebugPayload = (PoiAddedDebugPayload)customPacketPayload;
         BrainDebugRenderer.PoiInfo poiInfo = new BrainDebugRenderer.PoiInfo(poiAddedDebugPayload.pos(), poiAddedDebugPayload.poiType(), poiAddedDebugPayload.freeTicketCount());
         this.minecraft.debugRenderer.brainDebugRenderer.addPoi(poiInfo);
      } else if (customPacketPayload instanceof PoiRemovedDebugPayload) {
         PoiRemovedDebugPayload poiRemovedDebugPayload = (PoiRemovedDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.brainDebugRenderer.removePoi(poiRemovedDebugPayload.pos());
      } else if (customPacketPayload instanceof VillageSectionsDebugPayload) {
         VillageSectionsDebugPayload villageSectionsDebugPayload = (VillageSectionsDebugPayload)customPacketPayload;
         VillageSectionsDebugRenderer villageSectionsDebugRenderer = this.minecraft.debugRenderer.villageSectionsDebugRenderer;
         Set var10000 = villageSectionsDebugPayload.villageChunks();
         Objects.requireNonNull(villageSectionsDebugRenderer);
         var10000.forEach(villageSectionsDebugRenderer::setVillageSection);
         var10000 = villageSectionsDebugPayload.notVillageChunks();
         Objects.requireNonNull(villageSectionsDebugRenderer);
         var10000.forEach(villageSectionsDebugRenderer::setNotVillageSection);
      } else if (customPacketPayload instanceof GoalDebugPayload) {
         GoalDebugPayload goalDebugPayload = (GoalDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.goalSelectorRenderer.addGoalSelector(goalDebugPayload.entityId(), goalDebugPayload.pos(), goalDebugPayload.goals());
      } else if (customPacketPayload instanceof BrainDebugPayload) {
         BrainDebugPayload brainDebugPayload = (BrainDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.brainDebugRenderer.addOrUpdateBrainDump(brainDebugPayload.brainDump());
      } else if (customPacketPayload instanceof BeeDebugPayload) {
         BeeDebugPayload beeDebugPayload = (BeeDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateBeeInfo(beeDebugPayload.beeInfo());
      } else if (customPacketPayload instanceof HiveDebugPayload) {
         HiveDebugPayload hiveDebugPayload = (HiveDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateHiveInfo(hiveDebugPayload.hiveInfo(), this.level.getGameTime());
      } else if (customPacketPayload instanceof GameTestAddMarkerDebugPayload) {
         GameTestAddMarkerDebugPayload gameTestAddMarkerDebugPayload = (GameTestAddMarkerDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.gameTestDebugRenderer.addMarker(gameTestAddMarkerDebugPayload.pos(), gameTestAddMarkerDebugPayload.color(), gameTestAddMarkerDebugPayload.text(), gameTestAddMarkerDebugPayload.durationMs());
      } else if (customPacketPayload instanceof GameTestClearMarkersDebugPayload) {
         this.minecraft.debugRenderer.gameTestDebugRenderer.clear();
      } else if (customPacketPayload instanceof RaidsDebugPayload) {
         RaidsDebugPayload raidsDebugPayload = (RaidsDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.raidDebugRenderer.setRaidCenters(raidsDebugPayload.raidCenters());
      } else if (customPacketPayload instanceof GameEventDebugPayload) {
         GameEventDebugPayload gameEventDebugPayload = (GameEventDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.gameEventListenerRenderer.trackGameEvent(gameEventDebugPayload.gameEventType(), gameEventDebugPayload.pos());
      } else if (customPacketPayload instanceof GameEventListenerDebugPayload) {
         GameEventListenerDebugPayload gameEventListenerDebugPayload = (GameEventListenerDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.gameEventListenerRenderer.trackListener(gameEventListenerDebugPayload.listenerPos(), gameEventListenerDebugPayload.listenerRange());
      } else if (customPacketPayload instanceof BreezeDebugPayload) {
         BreezeDebugPayload breezeDebugPayload = (BreezeDebugPayload)customPacketPayload;
         this.minecraft.debugRenderer.breezeDebugRenderer.add(breezeDebugPayload.breezeInfo());
      } else {
         this.handleUnknownCustomPayload(customPacketPayload);
      }

   }

   private void handleUnknownCustomPayload(CustomPacketPayload customPacketPayload) {
      LOGGER.warn("Unknown custom packet payload: {}", customPacketPayload.type().id());
   }

   public void handleAddObjective(ClientboundSetObjectivePacket clientboundSetObjectivePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetObjectivePacket, this, this.minecraft);
      String string = clientboundSetObjectivePacket.getObjectiveName();
      if (clientboundSetObjectivePacket.getMethod() == 0) {
         this.scoreboard.addObjective(string, ObjectiveCriteria.DUMMY, clientboundSetObjectivePacket.getDisplayName(), clientboundSetObjectivePacket.getRenderType(), false, (NumberFormat)clientboundSetObjectivePacket.getNumberFormat().orElse((Object)null));
      } else {
         Objective objective = this.scoreboard.getObjective(string);
         if (objective != null) {
            if (clientboundSetObjectivePacket.getMethod() == 1) {
               this.scoreboard.removeObjective(objective);
            } else if (clientboundSetObjectivePacket.getMethod() == 2) {
               objective.setRenderType(clientboundSetObjectivePacket.getRenderType());
               objective.setDisplayName(clientboundSetObjectivePacket.getDisplayName());
               objective.setNumberFormat((NumberFormat)clientboundSetObjectivePacket.getNumberFormat().orElse((Object)null));
            }
         }
      }

   }

   public void handleSetScore(ClientboundSetScorePacket clientboundSetScorePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetScorePacket, this, this.minecraft);
      String string = clientboundSetScorePacket.objectiveName();
      ScoreHolder scoreHolder = ScoreHolder.forNameOnly(clientboundSetScorePacket.owner());
      Objective objective = this.scoreboard.getObjective(string);
      if (objective != null) {
         ScoreAccess scoreAccess = this.scoreboard.getOrCreatePlayerScore(scoreHolder, objective, true);
         scoreAccess.set(clientboundSetScorePacket.score());
         scoreAccess.display((Component)clientboundSetScorePacket.display().orElse((Object)null));
         scoreAccess.numberFormatOverride((NumberFormat)clientboundSetScorePacket.numberFormat().orElse((Object)null));
      } else {
         LOGGER.warn("Received packet for unknown scoreboard objective: {}", string);
      }

   }

   public void handleResetScore(ClientboundResetScorePacket clientboundResetScorePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundResetScorePacket, this, this.minecraft);
      String string = clientboundResetScorePacket.objectiveName();
      ScoreHolder scoreHolder = ScoreHolder.forNameOnly(clientboundResetScorePacket.owner());
      if (string == null) {
         this.scoreboard.resetAllPlayerScores(scoreHolder);
      } else {
         Objective objective = this.scoreboard.getObjective(string);
         if (objective != null) {
            this.scoreboard.resetSinglePlayerScore(scoreHolder, objective);
         } else {
            LOGGER.warn("Received packet for unknown scoreboard objective: {}", string);
         }
      }

   }

   public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket clientboundSetDisplayObjectivePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetDisplayObjectivePacket, this, this.minecraft);
      String string = clientboundSetDisplayObjectivePacket.getObjectiveName();
      Objective objective = string == null ? null : this.scoreboard.getObjective(string);
      this.scoreboard.setDisplayObjective(clientboundSetDisplayObjectivePacket.getSlot(), objective);
   }

   public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket clientboundSetPlayerTeamPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetPlayerTeamPacket, this, this.minecraft);
      net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Action action = clientboundSetPlayerTeamPacket.getTeamAction();
      PlayerTeam playerTeam;
      if (action == net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Action.ADD) {
         playerTeam = this.scoreboard.addPlayerTeam(clientboundSetPlayerTeamPacket.getName());
      } else {
         playerTeam = this.scoreboard.getPlayerTeam(clientboundSetPlayerTeamPacket.getName());
         if (playerTeam == null) {
            LOGGER.warn("Received packet for unknown team {}: team action: {}, player action: {}", new Object[]{clientboundSetPlayerTeamPacket.getName(), clientboundSetPlayerTeamPacket.getTeamAction(), clientboundSetPlayerTeamPacket.getPlayerAction()});
            return;
         }
      }

      Optional<Parameters> optional = clientboundSetPlayerTeamPacket.getParameters();
      optional.ifPresent((parameters) -> {
         playerTeam.setDisplayName(parameters.getDisplayName());
         playerTeam.setColor(parameters.getColor());
         playerTeam.unpackOptions(parameters.getOptions());
         Visibility visibility = Visibility.byName(parameters.getNametagVisibility());
         if (visibility != null) {
            playerTeam.setNameTagVisibility(visibility);
         }

         CollisionRule collisionRule = CollisionRule.byName(parameters.getCollisionRule());
         if (collisionRule != null) {
            playerTeam.setCollisionRule(collisionRule);
         }

         playerTeam.setPlayerPrefix(parameters.getPlayerPrefix());
         playerTeam.setPlayerSuffix(parameters.getPlayerSuffix());
      });
      net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Action action2 = clientboundSetPlayerTeamPacket.getPlayerAction();
      Iterator var6;
      String string;
      if (action2 == net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Action.ADD) {
         var6 = clientboundSetPlayerTeamPacket.getPlayers().iterator();

         while(var6.hasNext()) {
            string = (String)var6.next();
            this.scoreboard.addPlayerToTeam(string, playerTeam);
         }
      } else if (action2 == net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Action.REMOVE) {
         var6 = clientboundSetPlayerTeamPacket.getPlayers().iterator();

         while(var6.hasNext()) {
            string = (String)var6.next();
            this.scoreboard.removePlayerFromTeam(string, playerTeam);
         }
      }

      if (action == net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Action.REMOVE) {
         this.scoreboard.removePlayerTeam(playerTeam);
      }

   }

   public void handleParticleEvent(ClientboundLevelParticlesPacket clientboundLevelParticlesPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundLevelParticlesPacket, this, this.minecraft);
      if (clientboundLevelParticlesPacket.getCount() == 0) {
         double d = (double)(clientboundLevelParticlesPacket.getMaxSpeed() * clientboundLevelParticlesPacket.getXDist());
         double e = (double)(clientboundLevelParticlesPacket.getMaxSpeed() * clientboundLevelParticlesPacket.getYDist());
         double f = (double)(clientboundLevelParticlesPacket.getMaxSpeed() * clientboundLevelParticlesPacket.getZDist());

         try {
            this.level.addParticle(clientboundLevelParticlesPacket.getParticle(), clientboundLevelParticlesPacket.isOverrideLimiter(), clientboundLevelParticlesPacket.getX(), clientboundLevelParticlesPacket.getY(), clientboundLevelParticlesPacket.getZ(), d, e, f);
         } catch (Throwable var17) {
            LOGGER.warn("Could not spawn particle effect {}", clientboundLevelParticlesPacket.getParticle());
         }
      } else {
         for(int i = 0; i < clientboundLevelParticlesPacket.getCount(); ++i) {
            double g = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getXDist();
            double h = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getYDist();
            double j = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getZDist();
            double k = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getMaxSpeed();
            double l = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getMaxSpeed();
            double m = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getMaxSpeed();

            try {
               this.level.addParticle(clientboundLevelParticlesPacket.getParticle(), clientboundLevelParticlesPacket.isOverrideLimiter(), clientboundLevelParticlesPacket.getX() + g, clientboundLevelParticlesPacket.getY() + h, clientboundLevelParticlesPacket.getZ() + j, k, l, m);
            } catch (Throwable var16) {
               LOGGER.warn("Could not spawn particle effect {}", clientboundLevelParticlesPacket.getParticle());
               return;
            }
         }
      }

   }

   public void handleUpdateAttributes(ClientboundUpdateAttributesPacket clientboundUpdateAttributesPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundUpdateAttributesPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundUpdateAttributesPacket.getEntityId());
      if (entity != null) {
         if (!(entity instanceof LivingEntity)) {
            throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + String.valueOf(entity) + ")");
         } else {
            AttributeMap attributeMap = ((LivingEntity)entity).getAttributes();
            Iterator var4 = clientboundUpdateAttributesPacket.getValues().iterator();

            while(true) {
               while(var4.hasNext()) {
                  AttributeSnapshot attributeSnapshot = (AttributeSnapshot)var4.next();
                  AttributeInstance attributeInstance = attributeMap.getInstance(attributeSnapshot.attribute());
                  if (attributeInstance == null) {
                     LOGGER.warn("Entity {} does not have attribute {}", entity, attributeSnapshot.attribute().getRegisteredName());
                  } else {
                     attributeInstance.setBaseValue(attributeSnapshot.base());
                     attributeInstance.removeModifiers();
                     Iterator var7 = attributeSnapshot.modifiers().iterator();

                     while(var7.hasNext()) {
                        AttributeModifier attributeModifier = (AttributeModifier)var7.next();
                        attributeInstance.addTransientModifier(attributeModifier);
                     }
                  }
               }

               return;
            }
         }
      }
   }

   public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket clientboundPlaceGhostRecipePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPlaceGhostRecipePacket, this, this.minecraft);
      AbstractContainerMenu abstractContainerMenu = this.minecraft.player.containerMenu;
      if (abstractContainerMenu.containerId == clientboundPlaceGhostRecipePacket.getContainerId()) {
         this.recipeManager.byKey(clientboundPlaceGhostRecipePacket.getRecipe()).ifPresent((recipeHolder) -> {
            if (this.minecraft.screen instanceof RecipeUpdateListener) {
               RecipeBookComponent recipeBookComponent = ((RecipeUpdateListener)this.minecraft.screen).getRecipeBookComponent();
               recipeBookComponent.setupGhostRecipe(recipeHolder, abstractContainerMenu.slots);
            }

         });
      }
   }

   public void handleLightUpdatePacket(ClientboundLightUpdatePacket clientboundLightUpdatePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundLightUpdatePacket, this, this.minecraft);
      int i = clientboundLightUpdatePacket.getX();
      int j = clientboundLightUpdatePacket.getZ();
      ClientboundLightUpdatePacketData clientboundLightUpdatePacketData = clientboundLightUpdatePacket.getLightData();
      this.level.queueLightUpdate(() -> {
         this.applyLightData(i, j, clientboundLightUpdatePacketData);
      });
   }

   private void applyLightData(int i, int j, ClientboundLightUpdatePacketData clientboundLightUpdatePacketData) {
      LevelLightEngine levelLightEngine = this.level.getChunkSource().getLightEngine();
      BitSet bitSet = clientboundLightUpdatePacketData.getSkyYMask();
      BitSet bitSet2 = clientboundLightUpdatePacketData.getEmptySkyYMask();
      Iterator<byte[]> iterator = clientboundLightUpdatePacketData.getSkyUpdates().iterator();
      this.readSectionList(i, j, levelLightEngine, LightLayer.SKY, bitSet, bitSet2, iterator);
      BitSet bitSet3 = clientboundLightUpdatePacketData.getBlockYMask();
      BitSet bitSet4 = clientboundLightUpdatePacketData.getEmptyBlockYMask();
      Iterator<byte[]> iterator2 = clientboundLightUpdatePacketData.getBlockUpdates().iterator();
      this.readSectionList(i, j, levelLightEngine, LightLayer.BLOCK, bitSet3, bitSet4, iterator2);
      levelLightEngine.setLightEnabled(new ChunkPos(i, j), true);
   }

   public void handleMerchantOffers(ClientboundMerchantOffersPacket clientboundMerchantOffersPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundMerchantOffersPacket, this, this.minecraft);
      AbstractContainerMenu abstractContainerMenu = this.minecraft.player.containerMenu;
      if (clientboundMerchantOffersPacket.getContainerId() == abstractContainerMenu.containerId && abstractContainerMenu instanceof MerchantMenu) {
         MerchantMenu merchantMenu = (MerchantMenu)abstractContainerMenu;
         merchantMenu.setOffers(clientboundMerchantOffersPacket.getOffers());
         merchantMenu.setXp(clientboundMerchantOffersPacket.getVillagerXp());
         merchantMenu.setMerchantLevel(clientboundMerchantOffersPacket.getVillagerLevel());
         merchantMenu.setShowProgressBar(clientboundMerchantOffersPacket.showProgress());
         merchantMenu.setCanRestock(clientboundMerchantOffersPacket.canRestock());
      }

   }

   public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket clientboundSetChunkCacheRadiusPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetChunkCacheRadiusPacket, this, this.minecraft);
      this.serverChunkRadius = clientboundSetChunkCacheRadiusPacket.getRadius();
      this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
      this.level.getChunkSource().updateViewRadius(clientboundSetChunkCacheRadiusPacket.getRadius());
   }

   public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket clientboundSetSimulationDistancePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetSimulationDistancePacket, this, this.minecraft);
      this.serverSimulationDistance = clientboundSetSimulationDistancePacket.simulationDistance();
      this.level.setServerSimulationDistance(this.serverSimulationDistance);
   }

   public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket clientboundSetChunkCacheCenterPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundSetChunkCacheCenterPacket, this, this.minecraft);
      this.level.getChunkSource().updateViewCenter(clientboundSetChunkCacheCenterPacket.getX(), clientboundSetChunkCacheCenterPacket.getZ());
   }

   public void handleBlockChangedAck(ClientboundBlockChangedAckPacket clientboundBlockChangedAckPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundBlockChangedAckPacket, this, this.minecraft);
      this.level.handleBlockChangedAck(clientboundBlockChangedAckPacket.sequence());
   }

   public void handleBundlePacket(ClientboundBundlePacket clientboundBundlePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundBundlePacket, this, this.minecraft);
      Iterator var2 = clientboundBundlePacket.subPackets().iterator();

      while(var2.hasNext()) {
         Packet<? super ClientGamePacketListener> packet = (Packet)var2.next();
         packet.handle(this);
      }

   }

   public void handleProjectilePowerPacket(ClientboundProjectilePowerPacket clientboundProjectilePowerPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundProjectilePowerPacket, this, this.minecraft);
      Entity entity = this.level.getEntity(clientboundProjectilePowerPacket.getId());
      if (entity instanceof AbstractHurtingProjectile) {
         AbstractHurtingProjectile abstractHurtingProjectile = (AbstractHurtingProjectile)entity;
         abstractHurtingProjectile.accelerationPower = clientboundProjectilePowerPacket.getAccelerationPower();
      }

   }

   public void handleChunkBatchStart(ClientboundChunkBatchStartPacket clientboundChunkBatchStartPacket) {
      this.chunkBatchSizeCalculator.onBatchStart();
   }

   public void handleChunkBatchFinished(ClientboundChunkBatchFinishedPacket clientboundChunkBatchFinishedPacket) {
      this.chunkBatchSizeCalculator.onBatchFinished(clientboundChunkBatchFinishedPacket.batchSize());
      this.send(new ServerboundChunkBatchReceivedPacket(this.chunkBatchSizeCalculator.getDesiredChunksPerTick()));
   }

   public void handleDebugSample(ClientboundDebugSamplePacket clientboundDebugSamplePacket) {
      this.minecraft.getDebugOverlay().logRemoteSample(clientboundDebugSamplePacket.sample(), clientboundDebugSamplePacket.debugSampleType());
   }

   public void handlePongResponse(ClientboundPongResponsePacket clientboundPongResponsePacket) {
      this.pingDebugMonitor.onPongReceived(clientboundPongResponsePacket);
   }

   private void readSectionList(int i, int j, LevelLightEngine levelLightEngine, LightLayer lightLayer, BitSet bitSet, BitSet bitSet2, Iterator<byte[]> iterator) {
      for(int k = 0; k < levelLightEngine.getLightSectionCount(); ++k) {
         int l = levelLightEngine.getMinLightSection() + k;
         boolean bl = bitSet.get(k);
         boolean bl2 = bitSet2.get(k);
         if (bl || bl2) {
            levelLightEngine.queueSectionData(lightLayer, SectionPos.of(i, l, j), bl ? new DataLayer((byte[])((byte[])iterator.next()).clone()) : new DataLayer());
            this.level.setSectionDirtyWithNeighbors(i, l, j);
         }
      }

   }

   public Connection getConnection() {
      return this.connection;
   }

   public boolean isAcceptingMessages() {
      return this.connection.isConnected() && !this.closed;
   }

   public Collection<PlayerInfo> getListedOnlinePlayers() {
      return this.listedPlayers;
   }

   public Collection<PlayerInfo> getOnlinePlayers() {
      return this.playerInfoMap.values();
   }

   public Collection<UUID> getOnlinePlayerIds() {
      return this.playerInfoMap.keySet();
   }

   @Nullable
   public PlayerInfo getPlayerInfo(UUID uUID) {
      return (PlayerInfo)this.playerInfoMap.get(uUID);
   }

   @Nullable
   public PlayerInfo getPlayerInfo(String string) {
      Iterator var2 = this.playerInfoMap.values().iterator();

      PlayerInfo playerInfo;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         playerInfo = (PlayerInfo)var2.next();
      } while(!playerInfo.getProfile().getName().equals(string));

      return playerInfo;
   }

   public GameProfile getLocalGameProfile() {
      return this.localGameProfile;
   }

   public ClientAdvancements getAdvancements() {
      return this.advancements;
   }

   public CommandDispatcher<SharedSuggestionProvider> getCommands() {
      return this.commands;
   }

   public ClientLevel getLevel() {
      return this.level;
   }

   public DebugQueryHandler getDebugQueryHandler() {
      return this.debugQueryHandler;
   }

   public UUID getId() {
      return this.id;
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public Frozen registryAccess() {
      return this.registryAccess;
   }

   public void markMessageAsProcessed(PlayerChatMessage playerChatMessage, boolean bl) {
      MessageSignature messageSignature = playerChatMessage.signature();
      if (messageSignature != null && this.lastSeenMessages.addPending(messageSignature, bl) && this.lastSeenMessages.offset() > 64) {
         this.sendChatAcknowledgement();
      }

   }

   private void sendChatAcknowledgement() {
      int i = this.lastSeenMessages.getAndClearOffset();
      if (i > 0) {
         this.send(new ServerboundChatAckPacket(i));
      }

   }

   public void sendChat(String string) {
      Instant instant = Instant.now();
      long l = SaltSupplier.getLong();
      Update update = this.lastSeenMessages.generateAndApplyUpdate();
      MessageSignature messageSignature = this.signedMessageEncoder.pack(new SignedMessageBody(string, instant, l, update.lastSeen()));
      this.send(new ServerboundChatPacket(string, instant, l, messageSignature, update.update()));
   }

   public void sendCommand(String string) {
      SignableCommand<SharedSuggestionProvider> signableCommand = SignableCommand.of(this.parseCommand(string));
      if (signableCommand.arguments().isEmpty()) {
         this.send(new ServerboundChatCommandPacket(string));
      } else {
         Instant instant = Instant.now();
         long l = SaltSupplier.getLong();
         Update update = this.lastSeenMessages.generateAndApplyUpdate();
         ArgumentSignatures argumentSignatures = ArgumentSignatures.signCommand(signableCommand, (stringx) -> {
            SignedMessageBody signedMessageBody = new SignedMessageBody(stringx, instant, l, update.lastSeen());
            return this.signedMessageEncoder.pack(signedMessageBody);
         });
         this.send(new ServerboundChatCommandSignedPacket(string, instant, l, argumentSignatures, update.update()));
      }
   }

   public boolean sendUnsignedCommand(String string) {
      if (!SignableCommand.hasSignableArguments(this.parseCommand(string))) {
         this.send(new ServerboundChatCommandPacket(string));
         return true;
      } else {
         return false;
      }
   }

   private ParseResults<SharedSuggestionProvider> parseCommand(String string) {
      return this.commands.parse(string, this.suggestionsProvider);
   }

   public void tick() {
      if (this.connection.isEncrypted()) {
         ProfileKeyPairManager profileKeyPairManager = this.minecraft.getProfileKeyPairManager();
         if (profileKeyPairManager.shouldRefreshKeyPair()) {
            profileKeyPairManager.prepareKeyPair().thenAcceptAsync((optional) -> {
               optional.ifPresent(this::setKeyPair);
            }, this.minecraft);
         }
      }

      this.sendDeferredPackets();
      if (this.minecraft.getDebugOverlay().showNetworkCharts()) {
         this.pingDebugMonitor.tick();
      }

      this.debugSampleSubscriber.tick();
      this.telemetryManager.tick();
      if (this.levelLoadStatusManager != null) {
         this.levelLoadStatusManager.tick();
      }

   }

   public void setKeyPair(ProfileKeyPair profileKeyPair) {
      if (this.minecraft.isLocalPlayer(this.localGameProfile.getId())) {
         if (this.chatSession == null || !this.chatSession.keyPair().equals(profileKeyPair)) {
            this.chatSession = LocalChatSession.create(profileKeyPair);
            this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.getId());
            this.send(new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData()));
         }
      }
   }

   @Nullable
   public ServerData getServerData() {
      return this.serverData;
   }

   public FeatureFlagSet enabledFeatures() {
      return this.enabledFeatures;
   }

   public boolean isFeatureEnabled(FeatureFlagSet featureFlagSet) {
      return featureFlagSet.isSubsetOf(this.enabledFeatures());
   }

   public Scoreboard scoreboard() {
      return this.scoreboard;
   }

   public PotionBrewing potionBrewing() {
      return this.potionBrewing;
   }

   public void updateSearchTrees() {
      this.searchTrees.rebuildAfterLanguageChange();
   }

   public SessionSearchTrees searchTrees() {
      return this.searchTrees;
   }

   public ServerLinks serverLinks() {
      return this.serverLinks;
   }
}
