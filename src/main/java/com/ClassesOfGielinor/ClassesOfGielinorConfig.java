package com.ClassesOfGielinor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;


@ConfigGroup("ClassesOfGielinor")
public interface ClassesOfGielinorConfig extends Config
{
	//Config Variables
	enum ClassListEnum{
		None,
		Barbarian,
		Bard,
		Juggler,
		Cleric,
		Druid,
		Fighter,
		Necromancer,
		Paladin,
		Ranger,
		Rogue,
	}
	@ConfigItem(
			position = 1,
			keyName = "playerClass",
			name = "Chosen Class",
			description = "The class that your character will be locked to."
	)
	default ClassListEnum playerClass() { return ClassListEnum.None; }

	@ConfigItem(
			position = 2,
			keyName = "enableNonClassItems",
			name = "Force Allow Non-Class Items",
			description = "Overrides class options and allows the use of items unsuitable for your class when enabled."
	)
	default boolean enableNonClassItems(){ return false; }

	@ConfigItem(
			position = 3,
			keyName = "classIsSpellcaster",
			name = "Force Spellcaster",
			description = "Overrides class options and enables spells for your character."
	)
	default boolean classIsSpellcaster(){ return false; }
}
