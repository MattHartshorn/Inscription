package net.samongi.Inscription.Glyphs.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Glyphs.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Glyphs.Attributes.Base.MultiplierAttributeType;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ExperienceBonusAttributeType extends MultiplierAttributeType {

    private static final long serialVersionUID = -6051996265080614309L;
    private static final String TYPE_IDENTIFIER = "EXPERIENCE_BONUS";

    private ExperienceBonusAttributeType(String type_name, String description) {

        super(type_name, description);
    }

    //--------------------------------------------------------------------------------------------------------------------//
    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData playerData) {
                CacheData cached_data = playerData.getData(ExperienceBonusAttributeType.TYPE_IDENTIFIER);
                if (cached_data == null) {
                    cached_data = new ExperienceBonusAttributeType.Data();
                }
                if (!(cached_data instanceof ExperienceBonusAttributeType.Data)) {
                    return;
                }

                Inscription.logger.finer("Caching attribute for " + typeDescription);
                ExperienceBonusAttributeType.Data data = (ExperienceBonusAttributeType.Data) cached_data;

                double multiplier = getMultiplier(this.getGlyph());
                double currentValue = data.get();
                double newValue = currentValue + multiplier;

                data.set(newValue > 1 ? 1 : newValue);
                Inscription.logger.finer("  +C Added '" + multiplier + "' bonus " + currentValue + "->" + newValue);

                playerData.setData(data);
                Inscription.logger.finer("Finished caching for " + typeDescription);
            }

            @Override public String getLoreLine() {
                String chanceString = getMultiplierString(this.getGlyph());

                String infoLine = ChatColor.BLUE + "+" + chanceString + "x" + ChatColor.YELLOW + " extra experience.";
                return this.getType().getDescriptionLoreLine() + infoLine;
            }
        };
    }

    public static class Data implements CacheData {

        /* Data members of the the data */
        private double m_globalExperienceBonus = 0.0;

        /* *** Setters *** */
        public void set(double amount) {
            this.m_globalExperienceBonus = amount;
        }

        /* *** Getters *** */
        public double get() {
            return this.m_globalExperienceBonus;
        }

        @Override public void clear() {
            this.m_globalExperienceBonus = 0.0;
        }

        @Override public String getType() {
            return TYPE_IDENTIFIER;
        }

        @Override public String getData() {
            // TODO This returns the data as a string
            return "";
        }
    }

    public static class Constructor extends AttributeTypeConstructor {

        @Override public AttributeType construct(ConfigurationSection section) throws InvalidConfigurationException {
            String type = section.getString("type");
            if (type == null || !type.toUpperCase().equals(TYPE_IDENTIFIER))
                return null;

            String name = section.getString("name");
            if (name == null)
                return null;

            String descriptor = section.getString("descriptor");
            if (descriptor == null)
                return null;

            double minMultiplier = section.getDouble("min-multiplier");
            double maxMultiplier = section.getDouble("max-multiplier");
            if (minMultiplier > maxMultiplier) {
                Inscription.logger.warning(section.getName() + " : min multiplier is bigger than max chance");
                return null;
            }

            ExperienceBonusAttributeType attributeType = new ExperienceBonusAttributeType(name, descriptor);
            attributeType.setMin(minMultiplier);
            attributeType.setMax(maxMultiplier);

            double rarityMultiplier = section.getDouble("rarity-multiplier", 1);
            attributeType.setRarityMultiplier(rarityMultiplier);

            int modelIncrement = section.getInt("model", 0);
            attributeType.setModelIncrement(modelIncrement);

            attributeType.baseExperience = AttributeType.getIntMap(section.getConfigurationSection("base-experience"));
            attributeType.levelExperience = AttributeType.getIntMap(section.getConfigurationSection("level-experience"));

            return attributeType;
        }
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onExperienceChange(PlayerExpChangeEvent event) {
                    Player player = event.getPlayer();
                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
                    CacheData cacheData = playerData.getData(ExperienceBonusAttributeType.TYPE_IDENTIFIER);
                    if (!(cacheData instanceof ExperienceBonusAttributeType.Data)) {
                        return;
                    }
                    ExperienceBonusAttributeType.Data data = (ExperienceBonusAttributeType.Data) cacheData;

                    double experienceMultiplier = 1 + data.get();
                    int experience = event.getAmount();
                    double multipliedExperience = experience * experienceMultiplier;
                    double fractionalExperience = multipliedExperience - Math.floor(multipliedExperience);

                    Random rand = new Random();
                    if (rand.nextDouble() < fractionalExperience) {
                        multipliedExperience += 1;
                    }
                    event.setAmount((int) multipliedExperience);
                    Inscription.logger.finest("" + "[PlayerExpChangeEvent] Extra Experience: " + experienceMultiplier + " " + experience + " -> " + multipliedExperience);
                }
            };
        }
    }

}
