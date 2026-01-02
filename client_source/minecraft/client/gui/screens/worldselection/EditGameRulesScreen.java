package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.GameRuleTypeVisitor;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Type;
import net.minecraft.world.level.GameRules.Value;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EditGameRulesScreen extends Screen {
   private static final Component TITLE = Component.translatable("editGamerule.title");
   private static final int SPACING = 8;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final Consumer<Optional<GameRules>> exitCallback;
   private final Set<EditGameRulesScreen.RuleEntry> invalidEntries = Sets.newHashSet();
   private final GameRules gameRules;
   @Nullable
   private EditGameRulesScreen.RuleList ruleList;
   @Nullable
   private Button doneButton;

   public EditGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> consumer) {
      super(TITLE);
      this.gameRules = gameRules;
      this.exitCallback = consumer;
   }

   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.ruleList = (EditGameRulesScreen.RuleList)this.layout.addToContents(new EditGameRulesScreen.RuleList(this.gameRules));
      LinearLayout linearLayout = (LinearLayout)this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.doneButton = (Button)linearLayout.addChild(Button.builder(CommonComponents.GUI_DONE, (button) -> {
         this.exitCallback.accept(Optional.of(this.gameRules));
      }).build());
      linearLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
         this.onClose();
      }).build());
      this.layout.visitWidgets((guiEventListener) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
      });
      this.repositionElements();
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.ruleList != null) {
         this.ruleList.updateSize(this.width, this.layout);
      }

   }

   public void onClose() {
      this.exitCallback.accept(Optional.empty());
   }

   private void updateDoneButton() {
      if (this.doneButton != null) {
         this.doneButton.active = this.invalidEntries.isEmpty();
      }

   }

   void markInvalid(EditGameRulesScreen.RuleEntry ruleEntry) {
      this.invalidEntries.add(ruleEntry);
      this.updateDoneButton();
   }

   void clearInvalid(EditGameRulesScreen.RuleEntry ruleEntry) {
      this.invalidEntries.remove(ruleEntry);
      this.updateDoneButton();
   }

   // $FF: synthetic method
   static Font method_57771(EditGameRulesScreen editGameRulesScreen) {
      return editGameRulesScreen.font;
   }

   @Environment(EnvType.CLIENT)
   public class RuleList extends ContainerObjectSelectionList<EditGameRulesScreen.RuleEntry> {
      private static final int ITEM_HEIGHT = 24;

      public RuleList(final GameRules gameRules) {
         super(Minecraft.getInstance(), EditGameRulesScreen.this.width, EditGameRulesScreen.this.layout.getContentHeight(), EditGameRulesScreen.this.layout.getHeaderHeight(), 24);
         Map<Category, Map<Key<?>, EditGameRulesScreen.RuleEntry>> map = Maps.newHashMap();
         GameRules.visitGameRuleTypes(new GameRuleTypeVisitor(EditGameRulesScreen.this, gameRules, map) {
            // $FF: synthetic field
            final EditGameRulesScreen field_24314;
            // $FF: synthetic field
            final GameRules val$gameRules;
            // $FF: synthetic field
            final Map val$entries;
            // $FF: synthetic field
            final EditGameRulesScreen.RuleList field_24317;

            {
               this.field_24317 = ruleList;
               this.field_24314 = editGameRulesScreen;
               this.val$gameRules = gameRules;
               this.val$entries = map;
            }

            public void visitBoolean(Key<BooleanValue> key, Type<BooleanValue> type) {
               this.addEntry(key, (component, list, string, booleanValue) -> {
                  return this.field_24317.field_24313.new BooleanRuleEntry(this.field_24317.field_24313, component, list, string, booleanValue);
               });
            }

            public void visitInteger(Key<IntegerValue> key, Type<IntegerValue> type) {
               this.addEntry(key, (component, list, string, integerValue) -> {
                  return this.field_24317.field_24313.new IntegerRuleEntry(component, list, string, integerValue);
               });
            }

            private <T extends Value<T>> void addEntry(Key<T> key, EditGameRulesScreen.EntryFactory<T> entryFactory) {
               Component component = Component.translatable(key.getDescriptionId());
               Component component2 = Component.literal(key.getId()).withStyle(ChatFormatting.YELLOW);
               T value = this.val$gameRules.getRule(key);
               String string = value.serialize();
               Component component3 = Component.translatable("editGamerule.default", new Object[]{Component.literal(string)}).withStyle(ChatFormatting.GRAY);
               String string2 = key.getDescriptionId() + ".description";
               ImmutableList list;
               String string3;
               if (I18n.exists(string2)) {
                  Builder<FormattedCharSequence> builder = ImmutableList.builder().add(component2.getVisualOrderText());
                  Component component4 = Component.translatable(string2);
                  List var10000 = EditGameRulesScreen.method_57771(this.field_24317.field_24313).split(component4, 150);
                  Objects.requireNonNull(builder);
                  var10000.forEach(builder::add);
                  list = builder.add(component3.getVisualOrderText()).build();
                  String var13 = component4.getString();
                  string3 = var13 + "\n" + component3.getString();
               } else {
                  list = ImmutableList.of(component2.getVisualOrderText(), component3.getVisualOrderText());
                  string3 = component3.getString();
               }

               ((Map)this.val$entries.computeIfAbsent(key.getCategory(), (category) -> {
                  return Maps.newHashMap();
               })).put(key, entryFactory.create(component, list, string3, value));
            }

            // $FF: synthetic method
            private static Map method_27639(Category category) {
               return Maps.newHashMap();
            }

            // $FF: synthetic method
            private EditGameRulesScreen.RuleEntry method_27642(Component component, List list, String string, IntegerValue integerValue) {
               return this.field_24317.field_24313.new IntegerRuleEntry(component, list, string, integerValue);
            }

            // $FF: synthetic method
            private EditGameRulesScreen.RuleEntry method_27641(Component component, List list, String string, BooleanValue booleanValue) {
               return this.field_24317.field_24313.new BooleanRuleEntry(this.field_24317.field_24313, component, list, string, booleanValue);
            }
         });
         map.entrySet().stream().sorted(Entry.comparingByKey()).forEach((entry) -> {
            this.addEntry(EditGameRulesScreen.this.new CategoryRuleEntry(Component.translatable(((Category)entry.getKey()).getDescriptionId()).withStyle(new ChatFormatting[]{ChatFormatting.BOLD, ChatFormatting.YELLOW})));
            ((Map)entry.getValue()).entrySet().stream().sorted(Entry.comparingByKey(Comparator.comparing(Key::getId))).forEach((entryx) -> {
               this.addEntry((EditGameRulesScreen.RuleEntry)entryx.getValue());
            });
         });
      }

      public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
         super.renderWidget(guiGraphics, i, j, f);
         EditGameRulesScreen.RuleEntry ruleEntry = (EditGameRulesScreen.RuleEntry)this.getHovered();
         if (ruleEntry != null && ruleEntry.tooltip != null) {
            EditGameRulesScreen.this.setTooltipForNextRenderPass(ruleEntry.tooltip);
         }

      }
   }

   @Environment(EnvType.CLIENT)
   public class IntegerRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final EditBox input;

      public IntegerRuleEntry(final Component component, final List<FormattedCharSequence> list, final String string, final IntegerValue integerValue) {
         super(list, component);
         this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 44, 20, component.copy().append("\n").append(string).append("\n"));
         this.input.setValue(Integer.toString(integerValue.get()));
         this.input.setResponder((stringx) -> {
            if (integerValue.tryDeserialize(stringx)) {
               this.input.setTextColor(14737632);
               EditGameRulesScreen.this.clearInvalid(this);
            } else {
               this.input.setTextColor(-65536);
               EditGameRulesScreen.this.markInvalid(this);
            }

         });
         this.children.add(this.input);
      }

      public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
         this.renderLabel(guiGraphics, j, k);
         this.input.setX(k + l - 45);
         this.input.setY(j);
         this.input.render(guiGraphics, n, o, f);
      }
   }

   @Environment(EnvType.CLIENT)
   public class BooleanRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final CycleButton<Boolean> checkbox;

      public BooleanRuleEntry(final EditGameRulesScreen editGameRulesScreen, final Component component, final List list, final String string, final BooleanValue booleanValue) {
         super(list, component);
         this.checkbox = CycleButton.onOffBuilder(booleanValue.get()).displayOnlyValue().withCustomNarration((cycleButton) -> {
            return cycleButton.createDefaultNarrationMessage().append("\n").append(string);
         }).create(10, 5, 44, 20, component, (cycleButton, boolean_) -> {
            booleanValue.set(boolean_, (MinecraftServer)null);
         });
         this.children.add(this.checkbox);
      }

      public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
         this.renderLabel(guiGraphics, j, k);
         this.checkbox.setX(k + l - 45);
         this.checkbox.setY(j);
         this.checkbox.render(guiGraphics, n, o, f);
      }
   }

   @Environment(EnvType.CLIENT)
   public abstract class GameRuleEntry extends EditGameRulesScreen.RuleEntry {
      private final List<FormattedCharSequence> label;
      protected final List<AbstractWidget> children = Lists.newArrayList();

      public GameRuleEntry(@Nullable final List<FormattedCharSequence> list, final Component component) {
         super(list);
         this.label = EditGameRulesScreen.this.minecraft.font.split(component, 175);
      }

      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      protected void renderLabel(GuiGraphics guiGraphics, int i, int j) {
         if (this.label.size() == 1) {
            guiGraphics.drawString(EditGameRulesScreen.this.minecraft.font, (FormattedCharSequence)((FormattedCharSequence)this.label.get(0)), j, i + 5, -1, false);
         } else if (this.label.size() >= 2) {
            guiGraphics.drawString(EditGameRulesScreen.this.minecraft.font, (FormattedCharSequence)((FormattedCharSequence)this.label.get(0)), j, i, -1, false);
            guiGraphics.drawString(EditGameRulesScreen.this.minecraft.font, (FormattedCharSequence)((FormattedCharSequence)this.label.get(1)), j, i + 10, -1, false);
         }

      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   private interface EntryFactory<T extends Value<T>> {
      EditGameRulesScreen.RuleEntry create(Component component, List<FormattedCharSequence> list, String string, T value);
   }

   @Environment(EnvType.CLIENT)
   public class CategoryRuleEntry extends EditGameRulesScreen.RuleEntry {
      final Component label;

      public CategoryRuleEntry(final Component component) {
         super((List)null);
         this.label = component;
      }

      public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
         guiGraphics.drawCenteredString(EditGameRulesScreen.this.minecraft.font, (Component)this.label, k + l / 2, j + 5, -1);
      }

      public List<? extends GuiEventListener> children() {
         return ImmutableList.of();
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            // $FF: synthetic field
            final EditGameRulesScreen.CategoryRuleEntry field_33845;

            {
               this.field_33845 = categoryRuleEntry;
            }

            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            public void updateNarration(NarrationElementOutput narrationElementOutput) {
               narrationElementOutput.add(NarratedElementType.TITLE, this.field_33845.label);
            }
         });
      }
   }

   @Environment(EnvType.CLIENT)
   public abstract static class RuleEntry extends ContainerObjectSelectionList.Entry<EditGameRulesScreen.RuleEntry> {
      @Nullable
      final List<FormattedCharSequence> tooltip;

      public RuleEntry(@Nullable List<FormattedCharSequence> list) {
         this.tooltip = list;
      }
   }
}
