package com.godofthings.torchmaster.common;

import com.godofthings.torchmaster.common.logic.entityblocking.ITEBLightRegistry;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ModCaps {
   public static Capability<ITEBLightRegistry> TEB_REGISTRY = CapabilityManager.get(new CapabilityToken<ITEBLightRegistry>() {
   });
}
