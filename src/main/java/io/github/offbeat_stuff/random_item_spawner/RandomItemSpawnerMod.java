package io.github.offbeat_stuff.random_item_spawner;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.block.InfestedBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomItemSpawnerMod implements ModInitializer {
  // This logger is used to write text to the console and the log file.
  // It is considered best practice to use your mod id as the logger's name.
  // That way, it's clear which mod wrote info, warnings, and errors.
  public static final Logger LOGGER =
      LoggerFactory.getLogger("random-item-spawner");

  private static boolean initialized;
  private static List<List<Identifier>> itemCache;

  private static boolean isItemAllowed(Item item) {
    if (item instanceof SpawnEggItem || item instanceof OperatorOnlyBlockItem ||
        item instanceof SkullItem) {
      return false;
    }

    if (item.equals(Items.COMMAND_BLOCK_MINECART)) {
      return false;
    }

    if (item instanceof BlockItem blockItem) {
      var block = blockItem.getBlock();
      var notAllowedBlocks = List.of(
          Blocks.END_PORTAL_FRAME, Blocks.BEDROCK, Blocks.BUDDING_AMETHYST,
          Blocks.CHORUS_PLANT, Blocks.DIRT_PATH, Blocks.FARMLAND,
          Blocks.FROGSPAWN, Blocks.REINFORCED_DEEPSLATE, Blocks.LIGHT,
          Blocks.PETRIFIED_OAK_SLAB);
      if (notAllowedBlocks.contains(block)) {
        return false;
      }

      return !(block instanceof InfestedBlock) &&
          !(block instanceof SpawnerBlock);
    }
    return true;
  }

  private static String getName(Identifier id) {
    var idx = id.getPath().lastIndexOf("/");
    return id.getPath().substring(idx + 1).toLowerCase();
  }

  private static void initCache() {
    if (initialized) {
      return;
    }
    var cache = new ArrayList<List<Identifier>>();
    for (int i = 0; i < 26; i++) {
      cache.add(new ArrayList<>());
    }
    for (var id : Registries.ITEM.getIds()) {
      var item = Registries.ITEM.get(id);
      if (!isItemAllowed(item)) {
        continue;
      }
      var chr = getName(id).charAt(0);
      if (Character.isAlphabetic(chr)) {
        cache.get(chr - 'a').add(id);
      }
    }
    itemCache = (cache.stream().map(
                     f -> f.stream().collect(ObjectImmutableList.toList())))
                    .collect(ObjectImmutableList.toList());
    initialized = true;
  }

  private static int cooldown = 0;

  private static void spawnItem(ServerWorld world, PlayerEntity player) {
    var playerName = player.getEntityName().toLowerCase();
    if (playerName.isBlank()) {
      return;
    }
    var index = world.getRandom().nextInt(playerName.length());
    var character = playerName.charAt(index);
    if (!Character.isAlphabetic(character)) {
      return;
    }
    var items = itemCache.get(character - 'a');
    if (items.isEmpty()) {
      return;
    }
    var f = world.getRandom().nextInt(items.size());
    var item = Registries.ITEM.get(items.get(f));
    if (!item.getRequiredFeatures().isSubsetOf(world.getEnabledFeatures())) {
      return;
    }
    var itemEntity = new ItemEntity(world, player.getPos().x, player.getPos().y,
                                    player.getPos().z, item.getDefaultStack());
    world.spawnEntity(itemEntity);
  }

  public static void spawnItems(ServerWorld world) {
    if (!initialized) {
      return;
    }
    var player = world.getClosestPlayer(0.5, world.getBottomY(), 0.5, 2, true);
    if (player == null) {
      return;
    }
    if (!player.isOnGround() || !player.isSneaking()) {
      return;
    }
    if (cooldown > 0) {
      cooldown--;
      return;
    }
    spawnItem(world, player);
    cooldown = 400 + world.getRandom().nextInt(200);
  }

  private static void debugLog() {
    for (int i = 0; i < itemCache.size(); i++) {
      var chr = Character.toString('a' + (char)i);
      LOGGER.info("{} -> {}", chr, itemCache.get(i).size());
      LOGGER.info("{} -> {}", chr, itemCache.get(i));
    }
  }

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.
    LOGGER.info("Intializing item cache in random item spawner mod");
    initCache();
    // debugLog();
  }
}