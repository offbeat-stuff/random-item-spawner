package io.github.offbeat_stuff.random_item_spawner;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.ItemEntity;
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

  private static void initCache() {
    if (initialized) {
      return;
    }
    var cache = new ArrayList<List<Identifier>>();
    for (int i = 0; i < 26; i++) {
      cache.add(new ArrayList<Identifier>());
    }
    for (var id : Registries.ITEM.getIds()) {
      var chr = id.getNamespace().toLowerCase().charAt(0);
      if (Character.isAlphabetic(chr)) {
        cache.get(chr - 'a').add(id);
      }
    }
    itemCache = (cache.stream().map(
                     f -> f.stream().collect(ObjectImmutableList.toList())))
                    .collect(ObjectImmutableList.toList());
    initialized = true;
  }

  public static void spawnItems(ServerWorld world) {
    initCache();
    var player = world.getClosestPlayer(0, world.getBottomY(), 0, 2, true);
    if (player == null) {
      return;
    }
    if (!player.isOnGround() || !player.isSneaking()) {
      return;
    }
    if (world.getRandom().nextInt(40) > 0) {
      return;
    }
    var playerName = player.getEntityName().toLowerCase();
    var index = world.getRandom().nextInt(playerName.length());
    var character = playerName.charAt(index);
    if (!Character.isAlphabetic(character)) {
      return;
    }
    var items = itemCache.get(character - 'a');
    var f = world.getRandom().nextInt(items.size());
    var item = Registries.ITEM.get(items.get(f));
    var itemEntity = new ItemEntity(world, player.getPos().x, player.getPos().y,
                                    player.getPos().z, item.getDefaultStack());
    world.spawnEntity(itemEntity);
  }

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    LOGGER.info("Hello Fabric world!");
  }
}