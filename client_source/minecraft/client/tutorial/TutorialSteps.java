package net.minecraft.client.tutorial;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum TutorialSteps {
   MOVEMENT("movement", MovementTutorialStepInstance::new),
   FIND_TREE("find_tree", FindTreeTutorialStepInstance::new),
   PUNCH_TREE("punch_tree", PunchTreeTutorialStepInstance::new),
   OPEN_INVENTORY("open_inventory", OpenInventoryTutorialStep::new),
   CRAFT_PLANKS("craft_planks", CraftPlanksTutorialStep::new),
   NONE("none", CompletedTutorialStepInstance::new);

   private final String name;
   private final Function<Tutorial, ? extends TutorialStepInstance> constructor;

   private <T extends TutorialStepInstance> TutorialSteps(final String string2, final Function<Tutorial, T> function) {
      this.name = string2;
      this.constructor = function;
   }

   public TutorialStepInstance create(Tutorial tutorial) {
      return (TutorialStepInstance)this.constructor.apply(tutorial);
   }

   public String getName() {
      return this.name;
   }

   public static TutorialSteps getByName(String string) {
      TutorialSteps[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         TutorialSteps tutorialSteps = var1[var3];
         if (tutorialSteps.name.equals(string)) {
            return tutorialSteps;
         }
      }

      return NONE;
   }

   // $FF: synthetic method
   private static TutorialSteps[] $values() {
      return new TutorialSteps[]{MOVEMENT, FIND_TREE, PUNCH_TREE, OPEN_INVENTORY, CRAFT_PLANKS, NONE};
   }
}
