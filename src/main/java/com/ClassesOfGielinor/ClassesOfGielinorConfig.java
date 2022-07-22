package com.ClassesOfGielinor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;


@ConfigGroup("ClassesOfGielinor")
public interface ClassesOfGielinorConfig extends Config
{
	//Config Variables
	enum ClassListEnum{
		None,
		Artificer,
		Barbarian,
		Bard,
		Chef,
		Cleric,
		Druid,
		Fighter,
		Monk,
		Necromancer,
		Paladin,
		Ranger,
		Rogue,
		Warlock,
		Wizard
	}

	enum AlignmentEnum{
		Lawful_Good,
		Neutral_Good,
		Chaotic_Good,
		Lawful_Neutral,
		True_Neutral,
		Chaotic_Neutral,
		Lawful_Evil,
		Neutral_Evil,
		Chaotic_Evil
	}

	@ConfigSection(
			position = 1,
			name = "Character Setup",
			description = "Set your character below.",
			closedByDefault = false
	)
	String characterSetup = "characterSetup";

	@ConfigItem(
			position = 2,
			keyName = "playerClass",
			name = "Chosen Class",
			description = "The class that your character will be locked to.",
			section = characterSetup
	)
	default ClassListEnum playerClass() { return ClassListEnum.None; }

	@ConfigItem(
			position = 3,
			keyName = "playerAlignment",
			name = "Alignment",
			description = "The alignment of your character.",
			section = characterSetup
	)
	default AlignmentEnum playerAlignment() { return AlignmentEnum.True_Neutral; }

	@ConfigSection(
			position = 4,
			name = "Plugin Overrides",
			description = "Enable/disable features below",
			closedByDefault = true
	)
	String overrides = "overrides";

	@ConfigItem(
			position = 5,
			keyName = "enableNonClassItems",
			name = "Force Allow Non-Class Items",
			description = "Overrides class options and allows the use of items unsuitable for your class when enabled.",
			section = overrides
	)
	default boolean enableNonClassItems(){ return false; }

	@ConfigItem(
			position = 6,
			keyName = "classIsSpellcaster",
			name = "Force Spellcaster",
			description = "Overrides class options and enables spells for your character.",
			section = overrides
	)
	default boolean classIsSpellcaster(){ return false; }

	@ConfigItem(
			position = 7,
			keyName = "forceAllowPrayer",
			name = "Force Allow Prayer",
			description = "Overrides class options and enables prayer for your character.",
			section = overrides
	)
	default boolean forceAllowPrayer(){ return false; }

	@ConfigItem(
			position = 8,
			keyName = "disablePermItems",
			name = "Disable Permanent Items",
			description = "Disables permanent class items and allows them to be unequipped.",
			section = overrides
	)
	default boolean disablePermItems(){ return false; }

	default boolean allowEnchantment(){ return false; }

	@ConfigItem(
			position = 9,
			keyName = "allowProtectPrayers",
			name = "Allow Protection Prayers",
			description = "Allows the use of Protection Prayers for all classes.",
			section = overrides
	)
	default boolean allowProtectPrayers(){ return false; }

}
