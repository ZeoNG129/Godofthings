package com.godofthings.torchmaster.common.logic;

public class DistanceLogics {
   public static IDistanceLogic Cubic = (posX, posY, posZ, torch, range) -> {
      double minX = (double)(torch.getX() - range);
      double minY = (double)(torch.getY() - range);
      double minZ = (double)(torch.getZ() - range);
      double maxX = (double)(torch.getX() + range + 1);
      double maxY = (double)(torch.getY() + range + 1);
      double maxZ = (double)(torch.getZ() + range + 1);
      return minX <= posX && maxX >= posX && minY <= posY && maxY >= posY && minZ <= posZ && maxZ >= posZ;
   };
   public static IDistanceLogic Cylinder = (posX, posY, posZ, torch, range) -> {
      double dx = (double)torch.getX() + 0.5 - posX;
      double dy = Math.abs((double)torch.getY() + 0.5 - posY);
      double dz = (double)torch.getZ() + 0.5 - posZ;
      return dx * dx + dz * dz <= (double)range && dy <= (double)range;
   };
}
