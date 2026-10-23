package com.godofthings.doll.client;

import net.minecraft.client.gui.screens.Screen;

public interface TwoIInteractionScreen {
   int interactionEntityId();

   static int entityFor(Screen screen) {
      return screen instanceof TwoIInteractionScreen interaction ? interaction.interactionEntityId() : -1;
   }
}
