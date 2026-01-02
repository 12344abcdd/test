package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   @Nullable
   private String filter;

   public SocialInteractionsPlayerList(SocialInteractionsScreen socialInteractionsScreen, Minecraft minecraft, int i, int j, int k, int l) {
      super(minecraft, i, j, k, l);
      this.socialInteractionsScreen = socialInteractionsScreen;
   }

   protected void renderListBackground(GuiGraphics guiGraphics) {
   }

   protected void renderListSeparators(GuiGraphics guiGraphics) {
   }

   protected void enableScissor(GuiGraphics guiGraphics) {
      guiGraphics.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
   }

   public void updatePlayerList(Collection<UUID> collection, double d, boolean bl) {
      Map<UUID, PlayerEntry> map = new HashMap();
      this.addOnlinePlayers(collection, map);
      this.updatePlayersFromChatLog(map, bl);
      this.updateFiltersAndScroll(map.values(), d);
   }

   private void addOnlinePlayers(Collection<UUID> collection, Map<UUID, PlayerEntry> map) {
      ClientPacketListener clientPacketListener = this.minecraft.player.connection;
      Iterator var4 = collection.iterator();

      while(var4.hasNext()) {
         UUID uUID = (UUID)var4.next();
         PlayerInfo playerInfo = clientPacketListener.getPlayerInfo(uUID);
         if (playerInfo != null) {
            boolean bl = playerInfo.hasVerifiableChat();
            Minecraft var10004 = this.minecraft;
            SocialInteractionsScreen var10005 = this.socialInteractionsScreen;
            String var10007 = playerInfo.getProfile().getName();
            Objects.requireNonNull(playerInfo);
            map.put(uUID, new PlayerEntry(var10004, var10005, uUID, var10007, playerInfo::getSkin, bl));
         }
      }

   }

   private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> map, boolean bl) {
      Collection<GameProfile> collection = collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog());
      Iterator var4 = collection.iterator();

      while(true) {
         PlayerEntry playerEntry;
         do {
            if (!var4.hasNext()) {
               return;
            }

            GameProfile gameProfile = (GameProfile)var4.next();
            if (bl) {
               playerEntry = (PlayerEntry)map.computeIfAbsent(gameProfile.getId(), (uUID) -> {
                  PlayerEntry playerEntry = new PlayerEntry(this.minecraft, this.socialInteractionsScreen, gameProfile.getId(), gameProfile.getName(), this.minecraft.getSkinManager().lookupInsecure(gameProfile), true);
                  playerEntry.setRemoved(true);
                  return playerEntry;
               });
               break;
            }

            playerEntry = (PlayerEntry)map.get(gameProfile.getId());
         } while(playerEntry == null);

         playerEntry.setHasRecentMessages(true);
      }
   }

   private static Collection<GameProfile> collectProfilesFromChatLog(ChatLog chatLog) {
      Set<GameProfile> set = new ObjectLinkedOpenHashSet();

      for(int i = chatLog.end(); i >= chatLog.start(); --i) {
         LoggedChatEvent loggedChatEvent = chatLog.lookup(i);
         if (loggedChatEvent instanceof LoggedChatMessage.Player) {
            LoggedChatMessage.Player player = (LoggedChatMessage.Player)loggedChatEvent;
            if (player.message().hasSignature()) {
               set.add(player.profile());
            }
         }
      }

      return set;
   }

   private void sortPlayerEntries() {
      this.players.sort(Comparator.comparing((playerEntry) -> {
         if (this.minecraft.isLocalPlayer(playerEntry.getPlayerId())) {
            return 0;
         } else if (this.minecraft.getReportingContext().hasDraftReportFor(playerEntry.getPlayerId())) {
            return 1;
         } else if (playerEntry.getPlayerId().version() == 2) {
            return 4;
         } else {
            return playerEntry.hasRecentMessages() ? 2 : 3;
         }
      }).thenComparing((playerEntry) -> {
         if (!playerEntry.getPlayerName().isBlank()) {
            int i = playerEntry.getPlayerName().codePointAt(0);
            if (i == 95 || i >= 97 && i <= 122 || i >= 65 && i <= 90 || i >= 48 && i <= 57) {
               return 0;
            }
         }

         return 1;
      }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
   }

   private void updateFiltersAndScroll(Collection<PlayerEntry> collection, double d) {
      this.players.clear();
      this.players.addAll(collection);
      this.sortPlayerEntries();
      this.updateFilteredPlayers();
      this.replaceEntries(this.players);
      this.setScrollAmount(d);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf((playerEntry) -> {
            return !playerEntry.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter);
         });
         this.replaceEntries(this.players);
      }

   }

   public void setFilter(String string) {
      this.filter = string;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(PlayerInfo playerInfo, SocialInteractionsScreen.Page page) {
      UUID uUID = playerInfo.getProfile().getId();
      Iterator var4 = this.players.iterator();

      PlayerEntry playerEntry;
      while(var4.hasNext()) {
         playerEntry = (PlayerEntry)var4.next();
         if (playerEntry.getPlayerId().equals(uUID)) {
            playerEntry.setRemoved(false);
            return;
         }
      }

      if ((page == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(uUID)) && (Strings.isNullOrEmpty(this.filter) || playerInfo.getProfile().getName().toLowerCase(Locale.ROOT).contains(this.filter))) {
         boolean bl = playerInfo.hasVerifiableChat();
         Minecraft var10002 = this.minecraft;
         SocialInteractionsScreen var10003 = this.socialInteractionsScreen;
         UUID var10004 = playerInfo.getProfile().getId();
         String var10005 = playerInfo.getProfile().getName();
         Objects.requireNonNull(playerInfo);
         playerEntry = new PlayerEntry(var10002, var10003, var10004, var10005, playerInfo::getSkin, bl);
         this.addEntry(playerEntry);
         this.players.add(playerEntry);
      }

   }

   public void removePlayer(UUID uUID) {
      Iterator var2 = this.players.iterator();

      PlayerEntry playerEntry;
      do {
         if (!var2.hasNext()) {
            return;
         }

         playerEntry = (PlayerEntry)var2.next();
      } while(!playerEntry.getPlayerId().equals(uUID));

      playerEntry.setRemoved(true);
   }
}
