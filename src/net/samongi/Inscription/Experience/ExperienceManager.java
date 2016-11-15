package net.samongi.Inscription.Experience;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.samongi.Inscription.PlacementTracker;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Items.ItemUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

public class ExperienceManager
{

  // Experience mapping for entity related events
  private Map<EntityType, Map<String, Integer>> exp_per_kill = new HashMap<>();
  private Map<EntityType, Map<String, Integer>> exp_per_damage = new HashMap<>();

  // Experience mapping for material related events
  private Map<MaterialData, Map<String, Integer>> exp_per_break = new HashMap<>();
  private Map<MaterialData, Map<String, Integer>> exp_per_place = new HashMap<>();
  private Map<MaterialData, Map<String, Integer>> exp_per_craft = new HashMap<>();

  private PlacementTracker tracker;

  /**
   * Method that encompasses logic to be handled by the experience manager
   * when another entity damages another. Generally for player experience
   * rewards.
   * 
   * @param event
   */
  public void onEntityDamageEntity(EntityDamageByEntityEvent event)
  {
    Entity damaged = event.getEntity();
    Entity damager = event.getDamager();
    if (!(damager instanceof Player)) return;

    PlayerData data = Inscription.getInstance().getPlayerManager().getData((Player) damager);
    if (data == null)
    {
      Inscription.logger.severe("Player Data return null on call for: " + damager.getName() + ":"
          + damager.getUniqueId());
      return;
    }

    EntityType damaged_t = damaged.getType(); // Getting the type of the damage
                                              // entity.
    double damage_dealt = event.getFinalDamage();

    Map<String, Integer> exp_per = this.getExpPerDamage(damaged_t);
    if (exp_per == null) return;

    // TODO Experience event

    for (String s : exp_per.keySet())
    {
      int exp = (int) Math.ceil(exp_per.get(s) * damage_dealt);
      data.getGlyphInventory().distributeExperience(s, exp);
    }
  }
  public void onEntityDeath(EntityDeathEvent event)
  {
    LivingEntity killed = event.getEntity();
    EntityDamageEvent damage_event = killed.getLastDamageCause();
    if (!(damage_event instanceof EntityDamageByEntityEvent)) return;
    EntityDamageByEntityEvent entity_damage_event = (EntityDamageByEntityEvent) damage_event;

    Entity damaged = entity_damage_event.getEntity();
    Entity damager = entity_damage_event.getDamager();
    if (!(damager instanceof Player)) return;

    PlayerData data = Inscription.getInstance().getPlayerManager().getData((Player) damager);
    if (data == null)
    {
      Inscription.logger.severe("Player Data return null on call for: " + damager.getName() + ":"
          + damager.getUniqueId());
      return;
    }

    EntityType damaged_t = damaged.getType();

    Map<String, Integer> exp_per = this.getExpPerKill(damaged_t);
    if (exp_per == null) return;

    // TODO Experience event

    for (String s : exp_per.keySet())
    {
      int exp = exp_per.get(s);
      data.getGlyphInventory().distributeExperience(s, exp);
    }
  }

  public void onBlockBreak(BlockBreakEvent event)
  {
    Location l = event.getBlock().getLocation();
    Material m = event.getBlock().getType();
    if (this.tracker.isTracked(m) && this.tracker.isPlaced(l)) return;

    MaterialData material_data = event.getBlock().getState().getData();
    Player player = event.getPlayer();

    PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
    if (data == null)
    {
      Inscription.logger.severe("Player Data return null on call for: " + player.getName() + ":"
          + player.getUniqueId());
      return;
    }

    Map<String, Integer> exp_per = this.getExpPerBreak(material_data);
    if (exp_per == null) return;

    // TODO Experience event

    for (String s : exp_per.keySet())
    {
      int exp = exp_per.get(s);
      data.getGlyphInventory().distributeExperience(s, exp);
    }
  }
  public void onBlockPlace(BlockPlaceEvent event)
  {
    MaterialData material_data = event.getBlock().getState().getData();
    Player player = event.getPlayer();

    PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
    if (data == null)
    {
      Inscription.logger.severe("Player Data return null on call for: " + player.getName() + ":"
          + player.getUniqueId());
      return;
    }

    Map<String, Integer> exp_per = this.getExpPerPlace(material_data);
    if (exp_per == null) return;

    // TODO Experience event

    for (String s : exp_per.keySet())
    {
      int exp = exp_per.get(s);
      data.getGlyphInventory().distributeExperience(s, exp);
    }
  }

  public void onCraftItem(CraftItemEvent event)
  {
    if (event.isCancelled()) return;

    Recipe recipe = event.getRecipe(); // getting the recipe
    ItemStack result = recipe.getResult(); // getting the result of the craft
                                           // event
    Inscription.logger.fine("Heard Craft Item Event: Item Crafted Type: " + result.getType());
    MaterialData material_data = result.getData();

    // Note that this result will not be the total number of items that were
    // crafted.
    Player player = (Player) event.getWhoClicked();
    ItemStack[] current_contents = player.getInventory().getContents();
    ItemStack[] old_contents = new ItemStack[current_contents.length];
    // We're doing a copy since we can't be sure if the current contents are
    // immutable
    for (int i = 0; i < old_contents.length; i++)
      if (current_contents[i] != null) old_contents[i] = current_contents[i].clone();

    if (event.isShiftClick())
    {
      BukkitRunnable task = new BukkitRunnable()
      {

        @Override
        public void run()
        {
          final ItemStack[] new_contents = player.getInventory().getContents();
          int items_crafted = 0;
          for (int i = 0; i < new_contents.length; i++)
          {
            ItemStack new_item = new_contents[i];
            ItemStack old_item = old_contents[i];
            if (new_item != old_item)
            {
              if (!new_item.getData().equals(material_data)) continue;
              Inscription.logger.fine("Found Inv Difference: " + new_item + " =/= " + old_item);
              if (old_item == null || old_item.getType().equals(Material.AIR)) items_crafted += new_item.getAmount();
              else items_crafted += new_item.getAmount() - old_item.getAmount();
            }
          }
          // The total times the item was crafted
          int total_crafts = items_crafted / result.getAmount();
          Inscription.logger.fine("  Items Crafted: " + items_crafted);
          Inscription.logger.fine("  Result Amount: " + result.getAmount());
          Inscription.logger.fine("  Total Crafts: " + total_crafts);

          PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
          if (data == null)
          {
            Inscription.logger.fine("ERROR: Player Data return null on call for: " + player.getName() + ":"
                + player.getUniqueId());
            return;
          }
          Map<String, Integer> exp_per = Inscription.getInstance().getExperienceManager().getExpPerCraft(material_data);
          if (exp_per == null) return;

          // TODO Experience event

          for (String s : exp_per.keySet())
          {
            int exp = exp_per.get(s);
            for (int i = 0; i < total_crafts; i++)
              data.getGlyphInventory().distributeExperience(s, exp);
          }
        }
      }; // endef
      task.runTask(Inscription.getInstance()); // Running the task
    }
    else
    {
      PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
      if (data == null)
      {
        Inscription.logger.severe("Player Data return null on call for: " + player.getName() + ":"
            + player.getUniqueId());
        return;
      }
      Map<String, Integer> exp_per = Inscription.getInstance().getExperienceManager().getExpPerCraft(material_data);
      if (exp_per == null) return;
      for (String s : exp_per.keySet())
      {
        int exp = exp_per.get(s);
        data.getGlyphInventory().distributeExperience(s, exp);
      }
    }
  }

  public void onEnchantItem(EnchantItemEvent event)
  {
    // TODO
  }
  public void onAnvilRepair()
  {} // TODO Anvil events are most likely an inventory interact event

  public void setExpPerKill(EntityType type, String exp_type, int amount)
  {
    if (!this.exp_per_kill.containsKey(type)) this.exp_per_kill.put(type, new HashMap<String, Integer>());
    this.exp_per_kill.get(type).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerKill(EntityType type)
  {
    if (!this.exp_per_kill.containsKey(type)) return new HashMap<>();
    return this.exp_per_kill.get(type);
  }

  public void setExpPerDamage(EntityType type, String exp_type, int amount)
  {
    if (!this.exp_per_damage.containsKey(type)) this.exp_per_damage.put(type, new HashMap<String, Integer>());
    this.exp_per_damage.get(type).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerDamage(EntityType type)
  {
    if (!this.exp_per_damage.containsKey(type)) return new HashMap<>();
    return this.exp_per_damage.get(type);
  }

  public void setExpPerBreak(MaterialData mat, String exp_type, int amount)
  {
    if (!this.exp_per_break.containsKey(mat)) this.exp_per_break.put(mat, new HashMap<String, Integer>());
    this.exp_per_break.get(mat).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerBreak(MaterialData mat)
  {
    if (!this.exp_per_break.containsKey(mat)) return new HashMap<>();
    return this.exp_per_break.get(mat);
  }

  public void setExpPerPlace(MaterialData mat, String exp_type, int amount)
  {
    if (!this.exp_per_place.containsKey(mat)) this.exp_per_place.put(mat, new HashMap<String, Integer>());
    this.exp_per_place.get(mat).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerPlace(MaterialData mat)
  {
    if (!this.exp_per_place.containsKey(mat)) return new HashMap<>();
    return this.exp_per_place.get(mat);
  }

  public void setExpPerCraft(MaterialData mat, String exp_type, int amount)
  {
    if (!this.exp_per_craft.containsKey(mat)) this.exp_per_craft.put(mat, new HashMap<String, Integer>());
    this.exp_per_craft.get(mat).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerCraft(MaterialData mat)
  {
    if (!this.exp_per_craft.containsKey(mat)) return new HashMap<>();
    return this.exp_per_craft.get(mat);
  }

  /**
   * Parses a file with experience mappings.
   * 
   * @param config
   *          A filling with experience mappings
   */
  public void parse(ConfigFile config)
  {
    FileConfiguration root = config.getConfig();

    // All the configuration sections with experience data.
    ConfigurationSection entity_damage = root.getConfigurationSection("entity-damage");
    ConfigurationSection entity_kill = root.getConfigurationSection("entity-kill");
    ConfigurationSection material_break = root.getConfigurationSection("material-break");
    ConfigurationSection material_craft = root.getConfigurationSection("material-craft");
    ConfigurationSection material_place = root.getConfigurationSection("material-place");

    // Entity Damage configuration reading and setting
    if (entity_damage != null)
    {
      Inscription.logger.fine("Parsing Entity Damage Experience Rewards");
      Set<String> entity_damage_keys = entity_damage.getKeys(false);
      for (String k : entity_damage_keys)
      {
        Inscription.logger.fine("  Parsing: " + k);
        EntityType t = EntityType.valueOf(k);
        if (t == null) continue;

        ConfigurationSection key_section = entity_damage.getConfigurationSection(k);
        if (key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for (String exp : key_section_keys)
        {
          this.setExpPerDamage(t, exp, key_section.getInt(exp));
        }
      }
    }

    // Entity Kills configuration reading and setting
    if (entity_kill != null)
    {
      Inscription.logger.fine("Parsing Entity Kill Experience Rewards");
      Set<String> entity_kill_keys = entity_kill.getKeys(false);
      for (String k : entity_kill_keys)
      {
        Inscription.logger.fine("  Parsing: " + k);
        EntityType t = EntityType.valueOf(k);
        if (t == null) continue;

        ConfigurationSection key_section = entity_kill.getConfigurationSection(k);
        if (key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for (String exp : key_section_keys)
        {
          this.setExpPerKill(t, exp, key_section.getInt(exp));
        }
      }
    }
    // Material Breaking configuration reading and setting
    if (material_break != null)
    {
      Inscription.logger.fine("Parsing Material Break Experience Rewards");
      Set<String> material_break_keys = material_break.getKeys(false);
      for (String k : material_break_keys)
      {
        Inscription.logger.fine("  Parsing: " + k);
        MaterialData t = ItemUtil.getMaterialData(k);
        if (t == null) continue;

        ConfigurationSection key_section = material_break.getConfigurationSection(k);
        if (key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for (String exp : key_section_keys)
        {
          this.setExpPerBreak(t, exp, key_section.getInt(exp));
        }
      }
    }
    // Material Crafting configuration reading and setting
    if (material_craft != null)
    {
      Inscription.logger.fine("Parsing Material Place Experience Rewards");
      Set<String> material_craft_keys = material_craft.getKeys(false);
      for (String k : material_craft_keys)
      {
        Inscription.logger.fine("  Parsing: " + k);
        MaterialData t = ItemUtil.getMaterialData(k);
        if (t == null) continue;

        ConfigurationSection key_section = material_craft.getConfigurationSection(k);
        if (key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for (String exp : key_section_keys)
        {
          this.setExpPerCraft(t, exp, key_section.getInt(exp));
        }
      }
    }
    // Material Placing configuration reading and setting
    if (material_place != null)
    {
      Inscription.logger.fine("Parsing Material Craft Experience Rewards");
      Set<String> material_place_keys = material_place.getKeys(false);
      for (String k : material_place_keys)
      {
        Inscription.logger.fine("  Parsing: " + k);
        MaterialData t = ItemUtil.getMaterialData(k);
        if (t == null) continue;

        ConfigurationSection key_section = material_place.getConfigurationSection(k);
        if (key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for (String exp : key_section_keys)
        {
          this.setExpPerPlace(t, exp, key_section.getInt(exp));
        }
      }
    }
  } // endef

  public PlacementTracker getTracker()
  {
    return this.tracker;
  }
  public boolean hasTracker()
  {
    return this.tracker != null;
  }

  /**
   * Will configure the block tracker in the ExperienceManager using the
   * provided fileconfiguration
   * 
   * @param config
   *          The file configuration to use.
   */
  public void configureTracker(FileConfiguration config)
  {
    List<String> tracked_s = config.getStringList("placement-tracking");
    Set<Material> tracked_m = new HashSet<>();
    for (String s : tracked_s)
    {
      Material m = Material.getMaterial(s);
      if (m == null) continue;
      tracked_m.add(m);
    }
    if (this.tracker == null) this.tracker = new PlacementTracker();
    this.tracker.clearTracked();
    for (Material m : tracked_m)
      this.tracker.addTracked(m);
    this.tracker.cleanPlaced();
  }
  public void saveTracker(File file)
  {
    PlacementTracker.save(this.tracker, file);
  }
  public void loadTracker(File file)
  {
    this.tracker = PlacementTracker.load(file);
  }
}
