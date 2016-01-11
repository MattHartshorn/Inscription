package net.samongi.Inscription.Experience;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**Is only generated by the plugin whenever another event 
 * was triggered that responded in experience being earned.
 * This is not triggered by commands or by API calls.
 */
public class ExperienceGainedEvent extends Event implements Cancellable
{
  private static final HandlerList handlers = new HandlerList();
  
  private boolean cancelled = false;
  private Map<String, Integer> experience; // the experience amount being added.
  private final Event event; // the event that caused the experience gain
  private final Player player; // The player that will be gaining the experience
  
  public ExperienceGainedEvent(Map<String, Integer> experience, Event event, Player player)
  {
    this.experience = experience;
    this.event = event;
    this.player = player;
  }
  
  public Player getPlayer(){return this.player;}
  public Event getTriggeringEvent(){return this.event;}
  
  public Map<String, Integer> getExperience(){return this.experience;}
  public void setExperience(Map<String, Integer> experience){this.experience = experience;}
  
  @Override
  public boolean isCancelled(){return this.cancelled;}

  @Override
  public void setCancelled(boolean canceled){this.cancelled = canceled;}
  
  @Override
  public HandlerList getHandlers()
  {
    return ExperienceGainedEvent.handlers;
  }

}