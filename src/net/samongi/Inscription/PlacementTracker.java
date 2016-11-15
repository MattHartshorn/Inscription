package net.samongi.Inscription;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Keeps tabs on blocks that have been placed.
 *
 */
public class PlacementTracker implements Serializable
{

  private static final long serialVersionUID = -2972844610158182014L;

  private static class RegionLocation
  {

    private final int xCoord;
    private final int yCoord;
    private final String world;

    private RegionLocation(World world, int xCoord, int yCoord)
    {
      this.world = world.getName();
      this.xCoord = xCoord;
      this.yCoord = yCoord;
    }
    @Override
    public int hashCode()
    {
      return 0;
    }
    public boolean equals(RegionLocation other)
    {
      return false;
    }
    @Override
    public boolean equals(Object other)
    {
      if (other instanceof RegionLocation) return this.equals((RegionLocation) other);
      return false;
    }

  }

  private Set<Location> placed_locations = new HashSet<>();
  private Set<Material> tracked_materials = new HashSet<>();

  public boolean isPlaced(Location loc)
  {
    return this.placed_locations.contains(loc);
  }
  public void addPlaced(Location loc)
  {
    this.placed_locations.add(loc);
  }
  public void removePlaced(Location loc)
  {
    this.placed_locations.remove(loc);
  }
  /**
   * Resets all locations that are being tracked as placed.
   * This simply empties the hashset. (makes a new one and replaced the old set)
   */
  public void clearPlaced()
  {
    this.placed_locations = new HashSet<>();
  }
  /**
   * Will cleanup all placed locations, making sure they still contain a
   * material that is being tracked.
   * This will obviously remove any locations that have materials that are no
   * longer being tracked.
   */
  public void cleanPlaced()
  {
    Set<Location> to_remove = new HashSet<>();
    for (Location l : this.placed_locations)
    {
      Material m = l.getBlock().getType();
      if (!this.tracked_materials.contains(m)) to_remove.add(l);
    }
    this.placed_locations.removeAll(to_remove);
  }

  public boolean isTracked(Material mat)
  {
    return this.tracked_materials.contains(mat);
  }
  public void addTracked(Material mat)
  {
    this.tracked_materials.add(mat);
  }
  public void removeTracked(Material mat)
  {
    this.tracked_materials.remove(mat);
  }
  public void clearTracked()
  {
    this.tracked_materials = new HashSet<>();
  }

  public void onBlockBreak(BlockBreakEvent event)
  {
    if (event.isCancelled()) return;
    Location l = event.getBlock().getLocation();
    Material m = event.getBlock().getType();

    if (!this.isTracked(m)) return;
    if (!this.isPlaced(l)) return;
    this.removePlaced(l);
  }
  public void onBlockPlace(BlockPlaceEvent event)
  {
    if (event.isCancelled()) return;
    Location l = event.getBlock().getLocation();
    Material m = event.getBlock().getType();

    if (this.isTracked(m)) this.addPlaced(l);
  }
  public void onBlockPistonExtend(BlockPistonExtendEvent event)
  {
    if (event.isCancelled()) return;
    List<Block> moved_blocks = event.getBlocks();
    BlockFace direction = event.getDirection();
    for (Block b : moved_blocks)
    {
      Material m = b.getType();
      Location l = b.getLocation();
      if (!this.isTracked(m)) continue; // checking to see if the block type is
                                        // tracked
      if (!this.isPlaced(l)) continue; // checking to see if the block is a
                                       // tracked placed block
      int d_x = direction.getModX();
      int d_y = direction.getModY();
      int d_z = direction.getModZ();
      // getting the new location that the placed block would be in
      Location d_l = new Location(l.getWorld(), l.getBlockX() + d_x, l.getBlockY() + d_y, l.getBlockZ() + d_z);
      // Now to remove the old location and add the new location
      this.removePlaced(l);
      this.addPlaced(d_l);
    }

  }
  public void onBlockPistonRetract(BlockPistonRetractEvent event)
  {
    if (event.isCancelled()) return;
    List<Block> moved_blocks = event.getBlocks();
    BlockFace direction = event.getDirection();
    for (Block b : moved_blocks)
    {
      Material m = b.getType();
      Location l = b.getLocation();
      if (!this.isTracked(m)) continue; // checking to see if the block type is
                                        // tracked
      if (!this.isPlaced(l)) continue; // checking to see if the block is a
                                       // tracked placed block
      int d_x = direction.getModX();
      int d_y = direction.getModY();
      int d_z = direction.getModZ();
      // getting the new location that the placed block would be in
      Location d_l = new Location(l.getWorld(), l.getBlockX() + d_x, l.getBlockY() + d_y, l.getBlockZ() + d_z);
      // Now to remove the old location and add the new location
      this.removePlaced(l);
      this.addPlaced(d_l);
    }
  }

  public static PlacementTracker load(File file)
  {
    if (!file.exists() || file.isDirectory()) return null;
    PlacementTracker read_obj = null;
    try
    {
      FileInputStream file_in = new FileInputStream(file);
      ObjectInputStream obj_in = new ObjectInputStream(file_in);

      Object o = obj_in.readObject();
      read_obj = (PlacementTracker) o;

      obj_in.close();
      file_in.close();
    }
    catch (IOException e)
    {
      return null;
    }
    catch (ClassNotFoundException e)
    {
      return null;
    }

    return read_obj;
  }
  public static boolean save(PlacementTracker tracker, File file)
  {
    try
    {
      if (!file.exists()) file.createNewFile();
      FileOutputStream file_out = new FileOutputStream(file);
      ObjectOutputStream obj_out = new ObjectOutputStream(file_out);

      obj_out.writeObject(tracker);

      obj_out.close();
      file_out.close();
    }
    catch (IOException e)
    {
      return false;
    }
    return true;
  }
}
