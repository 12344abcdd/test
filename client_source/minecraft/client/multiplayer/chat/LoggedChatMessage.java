package net.minecraft.client.multiplayer.chat;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.ExtraCodecs;

@Environment(EnvType.CLIENT)
public interface LoggedChatMessage extends LoggedChatEvent {
   static LoggedChatMessage.Player player(GameProfile gameProfile, PlayerChatMessage playerChatMessage, ChatTrustLevel chatTrustLevel) {
      return new LoggedChatMessage.Player(gameProfile, playerChatMessage, chatTrustLevel);
   }

   static LoggedChatMessage.System system(Component component, Instant instant) {
      return new LoggedChatMessage.System(component, instant);
   }

   Component toContentComponent();

   default Component toNarrationComponent() {
      return this.toContentComponent();
   }

   boolean canReport(UUID uUID);

   @Environment(EnvType.CLIENT)
   public static record Player(GameProfile profile, PlayerChatMessage message, ChatTrustLevel trustLevel) implements LoggedChatMessage {
      public static final MapCodec<LoggedChatMessage.Player> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
         return instance.group(ExtraCodecs.GAME_PROFILE.fieldOf("profile").forGetter(LoggedChatMessage.Player::profile), PlayerChatMessage.MAP_CODEC.forGetter(LoggedChatMessage.Player::message), ChatTrustLevel.CODEC.optionalFieldOf("trust_level", ChatTrustLevel.SECURE).forGetter(LoggedChatMessage.Player::trustLevel)).apply(instance, LoggedChatMessage.Player::new);
      });
      private static final DateTimeFormatter TIME_FORMATTER;

      public Player(GameProfile gameProfile, PlayerChatMessage playerChatMessage, ChatTrustLevel chatTrustLevel) {
         this.profile = gameProfile;
         this.message = playerChatMessage;
         this.trustLevel = chatTrustLevel;
      }

      public Component toContentComponent() {
         if (!this.message.filterMask().isEmpty()) {
            Component component = this.message.filterMask().applyWithFormatting(this.message.signedContent());
            return (Component)(component != null ? component : Component.empty());
         } else {
            return this.message.decoratedContent();
         }
      }

      public Component toNarrationComponent() {
         Component component = this.toContentComponent();
         Component component2 = this.getTimeComponent();
         return Component.translatable("gui.chatSelection.message.narrate", new Object[]{this.profile.getName(), component, component2});
      }

      public Component toHeadingComponent() {
         Component component = this.getTimeComponent();
         return Component.translatable("gui.chatSelection.heading", new Object[]{this.profile.getName(), component});
      }

      private Component getTimeComponent() {
         LocalDateTime localDateTime = LocalDateTime.ofInstant(this.message.timeStamp(), ZoneOffset.systemDefault());
         return Component.literal(localDateTime.format(TIME_FORMATTER)).withStyle(new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY});
      }

      public boolean canReport(UUID uUID) {
         return this.message.hasSignatureFrom(uUID);
      }

      public UUID profileId() {
         return this.profile.getId();
      }

      public LoggedChatEvent.Type type() {
         return LoggedChatEvent.Type.PLAYER;
      }

      public GameProfile profile() {
         return this.profile;
      }

      public PlayerChatMessage message() {
         return this.message;
      }

      public ChatTrustLevel trustLevel() {
         return this.trustLevel;
      }

      static {
         TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
      }
   }

   @Environment(EnvType.CLIENT)
   public static record System(Component message, Instant timeStamp) implements LoggedChatMessage {
      public static final MapCodec<LoggedChatMessage.System> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
         return instance.group(ComponentSerialization.CODEC.fieldOf("message").forGetter(LoggedChatMessage.System::message), ExtraCodecs.INSTANT_ISO8601.fieldOf("time_stamp").forGetter(LoggedChatMessage.System::timeStamp)).apply(instance, LoggedChatMessage.System::new);
      });

      public System(Component component, Instant instant) {
         this.message = component;
         this.timeStamp = instant;
      }

      public Component toContentComponent() {
         return this.message;
      }

      public boolean canReport(UUID uUID) {
         return false;
      }

      public LoggedChatEvent.Type type() {
         return LoggedChatEvent.Type.SYSTEM;
      }

      public Component message() {
         return this.message;
      }

      public Instant timeStamp() {
         return this.timeStamp;
      }
   }
}
