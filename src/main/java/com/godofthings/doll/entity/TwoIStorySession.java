package com.godofthings.doll.entity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class TwoIStorySession {
   public static final long LIFETIME_TICKS = 36000L;
   public static final int CHOICE_ROUNDS = 3;
   private static final AtomicLong NEXT_ID = new AtomicLong(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
   private final UUID player;
   private final long id;
   private final int chapter;
   private final long expiresAt;
   private int expectedRound;

   public TwoIStorySession(UUID player, int chapter, long now) {
      this.player = player;
      this.id = NEXT_ID.updateAndGet(value -> value == Long.MAX_VALUE ? 1L : value + 1L);
      this.chapter = chapter;
      this.expiresAt = now + 36000L;
   }

   public long id() {
      return this.id;
   }

   public int chapter() {
      return this.chapter;
   }

   public int expectedRound() {
      return this.expectedRound;
   }

   public boolean finished() {
      return this.expectedRound >= 3;
   }

   public boolean expired(long now) {
      return this.finished() || now >= this.expiresAt;
   }

   public boolean accept(UUID caller, long token, int choice, long now) {
      if (this.player.equals(caller) && token == this.id && choice >= 0 && choice < 9 && choiceRound(choice) == this.expectedRound && !this.expired(now)) {
         this.expectedRound++;
         return true;
      } else {
         return false;
      }
   }

   public static int choiceRound(int code) {
      return code / 3;
   }

   public static int choiceIndex(int code) {
      return code % 3;
   }

   public static int bondDelta(int choice) {
      return switch (choice) {
         case 0 -> 1;
         case 1 -> -1;
         case 2 -> 0;
         default -> throw new IllegalArgumentException("Invalid 2i story choice");
      };
   }

   public static int nextChapterFor(int bond, int completedMask, int lastChapter) {
      if ((completedMask & 1) == 0) {
         return 0;
      } else {
         int[] unlocked = bond >= 70 ? new int[]{1, 3, 4, 5, 2} : (bond >= 40 ? new int[]{1, 3, 4, 5} : (bond >= 20 ? new int[]{1, 3, 4} : new int[]{1, 3}));

         for (int chapter : unlocked) {
            if ((completedMask & 1 << chapter) == 0) {
               return chapter;
            }
         }

         for (int index = 0; index < unlocked.length; index++) {
            if (unlocked[index] == lastChapter) {
               return unlocked[(index + 1) % unlocked.length];
            }
         }

         return unlocked[0];
      }
   }
}
