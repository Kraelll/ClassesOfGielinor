package com.ClassesOfGielinor;

/*
Before anything else, I must say that a lot of this project would not have been possible without the help and insightful
programming notes/documentation of many other contributors in various plugins. The RuneLite community is filled with very
talented people, and I am very grateful for their documentation and clear code which allowed me to learn how to program this
plugin in a language that I was only vaguely familiar with.

I have documented specific names of developers who have majorly helped me where applicable, and if anybody decides to reuse
any of the code I have created then I strongly encourage you to check out their plugins and other projects!

Thanks for taking the time to read through my (awful) code!

- Nat (Kraelll)
*/

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemComposition;
import net.runelite.client.chat.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Classes of Gielinor",
	description = "Adds character classes to Old School Runescape!"
)

public class ClassesOfGielinorPlugin extends Plugin
{
	//Public variables
	public boolean spellCaster = false;
	public boolean disableNonClassItems = false;
	public boolean prayerAllowed = false;

	//Arrays
	public String[][] validClassItems = new String[21][21];
	public String[][] permClassItems = new String[21][21];
	/* Array Structure:
		[x][0] - x will always be a class ID, and [0] will always be the class' string name for easy identification
		[x]][1..n] - n will be an entry in to the array for items names, and will go from 1-n
	 */

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClassesOfGielinorConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Classes of Gielinor plugin started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Classes of Gielinor plugin stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			getClassRestrictions();
			setAllowedItems(config.playerClass().toString());
			setClassPermanentItems(config.playerClass().toString());
			return;
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		getClassRestrictions();
		setAllowedItems(config.playerClass().toString());
		setClassPermanentItems(config.playerClass().toString());

		String currentClass = config.playerClass().toString();
		String msgStr = "";

		if (currentClass == "None") {
			msgStr = "You do not have a class set.";
		}
		else{
			msgStr = "Your class has been set to " + currentClass + ".";
		}
		sendChatMessage(msgStr);

		if(spellCaster == true || config.classIsSpellcaster() == true)
		{
			msgStr = "You feel magical energies empower you.";
		}
		else{
			msgStr = "";
		}

		if (msgStr == "")
		{
			//Do not broadcast
		}
		else{
			sendChatMessage(msgStr);
		}

		if(prayerAllowed == true || config.forceAllowPrayer() == true)
		{
			sendChatMessage("The blessing of the Gods is with you.");
		}

 		return;
	}


	private void getClassRestrictions()
	{
		switch(config.playerClass())
		{
			case None:
			default:
				spellCaster = true;
				disableNonClassItems = false;
				setAllowedItems(config.playerClass().toString());
				break;
			case Cleric:
			case Wizard: {
				spellCaster = true;
				disableNonClassItems = true;
				prayerAllowed = true;
				setAllowedItems(config.playerClass().toString());
				break;
			}

			case Necromancer:{
				spellCaster = true;
				disableNonClassItems = true;
				prayerAllowed = false;
				setAllowedItems(config.playerClass().toString());
				break;
			}
			case Chef:
			case Lumberjack:
			case Ranger: {
				spellCaster = false;
				disableNonClassItems = true;
				prayerAllowed = false;
				setAllowedItems(config.playerClass().toString());
				break;
			}

			case Paladin: {
				spellCaster = false;
				disableNonClassItems = true;
				prayerAllowed = true;
				setAllowedItems(config.playerClass().toString());
				break;

			}
		}
		return;
	}

	private void setAllowedItems(String PlayerClass)
	{
		//I apologise for how hard coded this section is. Unfortunately by the nature of the restrictions there is no way to automate it.
		//(Unless I use an XML file of some kind)
		int classID = -1;

		switch(PlayerClass)
		{
			case "None":{
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;
				validClassItems[classID][1] = "!ALL!";
				validClassItems[classID][2] = "----";
				validClassItems[classID][3] = "----";
				validClassItems[classID][4] = "----";
				validClassItems[classID][5] = "----";
				validClassItems[classID][6] = "----";
				validClassItems[classID][7] = "----";
				validClassItems[classID][8] = "----";
				validClassItems[classID][9] = "----";
				validClassItems[classID][10] = "----";
				validClassItems[classID][11] = "----";
				validClassItems[classID][12] = "----";
				validClassItems[classID][13] = "----";
				validClassItems[classID][14] = "----";
				validClassItems[classID][15] = "----";
				validClassItems[classID][16] = "----";
				validClassItems[classID][17] = "----";
				validClassItems[classID][18] = "----";
				validClassItems[classID][19] = "----";
				validClassItems[classID][20] = "----";
				break;
			}

			case "Chef":{
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "dagger";
				validClassItems[classID][2] = "knife";
				validClassItems[classID][3] = "spoon";
				validClassItems[classID][4] = "whisk";
				validClassItems[classID][5] = "Spork";
				validClassItems[classID][6] = "Spatula";
				validClassItems[classID][7] = "Frying pan";
				validClassItems[classID][8] = "Skewer";
				validClassItems[classID][9] = "Rolling pin";
				validClassItems[classID][10] = "Kitchen knife";
				validClassItems[classID][11] = "Meat tenderiser";
				validClassItems[classID][12] = "Cleaver";
				validClassItems[classID][13] = "Rubber chicken";
				validClassItems[classID][14] = "Carrot sword";
				validClassItems[classID][15] = "Candy cane";
				validClassItems[classID][16] = "----";
				validClassItems[classID][17] = "----";
				validClassItems[classID][18] = "----";
				validClassItems[classID][19] = "----";
				validClassItems[classID][20] = "----";
				break;
			}

			case "Cleric":{
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "dagger";
				validClassItems[classID][2] = "sword";
				validClassItems[classID][3] = "Holy water";
				validClassItems[classID][4] = "mace";
				validClassItems[classID][5] = "flail";
				validClassItems[classID][6] = "sickle";
				validClassItems[classID][7] = "banner";
				validClassItems[classID][8] = "halberd";
				validClassItems[classID][9] = "godsword";
				validClassItems[classID][10] = "----";
				validClassItems[classID][11] = "----";
				validClassItems[classID][12] = "----";
				validClassItems[classID][13] = "----";
				validClassItems[classID][14] = "----";
				validClassItems[classID][15] = "----";
				validClassItems[classID][16] = "----";
				validClassItems[classID][17] = "----";
				validClassItems[classID][18] = "----";
				validClassItems[classID][19] = "----";
				validClassItems[classID][20] = "----";
				break;
			}

			case "Lumberjack": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "axe";
				validClassItems[classID][2] = "thrownaxe";
				validClassItems[classID][3] = "----";
				validClassItems[classID][4] = "----";
				validClassItems[classID][5] = "----";
				validClassItems[classID][6] = "----";
				validClassItems[classID][7] = "----";
				validClassItems[classID][8] = "----";
				validClassItems[classID][9] = "----";
				validClassItems[classID][10] = "----";
				validClassItems[classID][11] = "----";
				validClassItems[classID][12] = "----";
				validClassItems[classID][13] = "----";
				validClassItems[classID][14] = "----";
				validClassItems[classID][15] = "----";
				validClassItems[classID][16] = "----";
				validClassItems[classID][17] = "----";
				validClassItems[classID][18] = "----";
				validClassItems[classID][19] = "----";
				validClassItems[classID][20] = "----";
				break;
			}

			case "Necromancer": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "Staff";
				validClassItems[classID][2] = "staff";
				validClassItems[classID][3] = "sceptre";
				validClassItems[classID][4] = "crozier";
				validClassItems[classID][5] = "Dawnbringer";
				validClassItems[classID][6] = "----";
				validClassItems[classID][7] = "----";
				validClassItems[classID][8] = "----";
				validClassItems[classID][9] = "----";
				validClassItems[classID][10] = "----";
				validClassItems[classID][11] = "----";
				validClassItems[classID][12] = "----";
				validClassItems[classID][13] = "----";
				validClassItems[classID][14] = "----";
				validClassItems[classID][15] = "----";
				validClassItems[classID][16] = "----";
				validClassItems[classID][17] = "----";
				validClassItems[classID][18] = "----";
				validClassItems[classID][19] = "----";
				validClassItems[classID][20] = "----";
				break;
			}

			case "Ranger": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "bow";
				validClassItems[classID][2] = "sword";
				validClassItems[classID][3] = "dagger";
				validClassItems[classID][4] = "arrow";
				validClassItems[classID][5] = "bolt";
				validClassItems[classID][6] = "Chinchompa";
				validClassItems[classID][7] = "crossbow";
				validClassItems[classID][8] = "knife";
				validClassItems[classID][9] = "----";
				validClassItems[classID][10] = "----";
				validClassItems[classID][11] = "----";
				validClassItems[classID][12] = "----";
				validClassItems[classID][13] = "----";
				validClassItems[classID][14] = "----";
				validClassItems[classID][15] = "----";
				validClassItems[classID][16] = "----";
				validClassItems[classID][17] = "----";
				validClassItems[classID][18] = "----";
				validClassItems[classID][19] = "----";
				validClassItems[classID][20] = "----";
				break;
			}

			case "Wizard": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "Staff";
				validClassItems[classID][2] = "staff";
				validClassItems[classID][3] = "sceptre";
				validClassItems[classID][4] = "crozier";
				validClassItems[classID][5] = "Dawnbringer";
				validClassItems[classID][6] = "wand";
				validClassItems[classID][7] = "----";
				validClassItems[classID][8] = "----";
				validClassItems[classID][9] = "----";
				validClassItems[classID][10] = "----";
				validClassItems[classID][11] = "----";
				validClassItems[classID][12] = "----";
				validClassItems[classID][13] = "----";
				validClassItems[classID][14] = "----";
				validClassItems[classID][15] = "----";
				validClassItems[classID][16] = "----";
				validClassItems[classID][17] = "----";
				validClassItems[classID][18] = "----";
				validClassItems[classID][19] = "----";
				validClassItems[classID][20] = "----";
				break;
			}

			case "Paladin":{
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;
				validClassItems[classID][1] = "godsword";
				validClassItems[classID][2] = "bulwark";
				validClassItems[classID][3] = "mace";
				validClassItems[classID][4] = "flail";
				validClassItems[classID][5] = "2h";
				validClassItems[classID][6] = "hasta";
				validClassItems[classID][7] = "spear";
				validClassItems[classID][8] = "bludgeon";
				validClassItems[classID][9] = "banner";
				validClassItems[classID][10] = "mjolnir";
				validClassItems[classID][11] = "----";
				validClassItems[classID][12] = "----";
				validClassItems[classID][13] = "----";
				validClassItems[classID][14] = "----";
				validClassItems[classID][15] = "----";
				validClassItems[classID][16] = "----";
				validClassItems[classID][17] = "----";
				validClassItems[classID][18] = "----";
				validClassItems[classID][19] = "----";
				validClassItems[classID][20] = "----";
				break;
			}

		}
		return;
	}

	private int getClassID(String PlayerClass)
	{
		int classID = 0;

		//Switch has been organised in to numerical order
		switch(PlayerClass)
		{
			case "None":
				classID = 0;
				break;
			case "Chef":
				classID = 1;
				break;
			case "Cleric":
				classID = 2;
				break;
			case "Lumberjack":
				classID = 3;
				break;
			case "Necromancer":
				classID = 4;
				break;
			case "Ranger":
				classID = 5;
				break;
			case "Wizard":
				classID = 6;
				break;
			case "Paladin":
				classID = 7;
				break;
		}
		return classID;
	}

	private int getAllowedItems(String currentItem)
	{
		//Check through the array to detect if the weapon name appears anywhere in the allowed items list for that class.
		int classID = getClassID(config.playerClass().toString());

		int i;
		int itemAllowed = 0;
		String ArrayItem = "";
		currentItem = currentItem.toUpperCase();

		while(itemAllowed <= 0)
		{
			for(i = 1; i < validClassItems.length; i++)
			{
				ArrayItem = validClassItems[classID][i].toUpperCase();
				if(currentItem.contains(ArrayItem))
				{
					itemAllowed = 1;
				}
				else
				{
					//Do nothing, leave item allowed 0
				}
			}

			if(itemAllowed != 1)
			{
				itemAllowed = 2;
			}
		}

		return itemAllowed;
	}

	private void setClassPermanentItems(String ClassName)
	{
		switch(ClassName)
		{
			case "Chef":{
				int ClassID = getClassID(ClassName);
				permClassItems[ClassID][0] = ClassName;

				permClassItems[ClassID][1] = "Chef's hat";
				permClassItems[ClassID][2] = "Golden chef's hat";
				permClassItems[ClassID][3] = "Golden apron";
				permClassItems[ClassID][4] = "----";
				permClassItems[ClassID][5] = "----";
				permClassItems[ClassID][6] = "----";
				permClassItems[ClassID][7] = "----";
				permClassItems[ClassID][8] = "----";
				permClassItems[ClassID][9] = "----";
				permClassItems[ClassID][10] = "----";
				permClassItems[ClassID][11] = "----";
				permClassItems[ClassID][12] = "----";
				permClassItems[ClassID][13] = "----";
				permClassItems[ClassID][14] = "----";
				permClassItems[ClassID][15] = "----";
				permClassItems[ClassID][16] = "----";
				permClassItems[ClassID][17] = "----";
				permClassItems[ClassID][18] = "----";
				permClassItems[ClassID][19] = "----";
				permClassItems[ClassID][20] = "----";
			}

			case "Lumberjack":{
				int ClassID = getClassID(ClassName);
				permClassItems[ClassID][0] = ClassName;

				permClassItems[ClassID][1] = "axe";
				permClassItems[ClassID][2] = "Lumberjack";
				permClassItems[ClassID][3] = "----";
				permClassItems[ClassID][4] = "----";
				permClassItems[ClassID][5] = "----";
				permClassItems[ClassID][6] = "----";
				permClassItems[ClassID][7] = "----";
				permClassItems[ClassID][8] = "----";
				permClassItems[ClassID][9] = "----";
				permClassItems[ClassID][10] = "----";
				permClassItems[ClassID][11] = "----";
				permClassItems[ClassID][12] = "----";
				permClassItems[ClassID][13] = "----";
				permClassItems[ClassID][14] = "----";
				permClassItems[ClassID][15] = "----";
				permClassItems[ClassID][16] = "----";
				permClassItems[ClassID][17] = "----";
				permClassItems[ClassID][18] = "----";
				permClassItems[ClassID][19] = "----";
				permClassItems[ClassID][20] = "----";
			}

			case "Ranger":{
				int ClassID = getClassID(ClassName);
				permClassItems[ClassID][0] = ClassName;

				permClassItems[ClassID][1] = "Ranger";
				permClassItems[ClassID][2] = "Robin hood hat";
				permClassItems[ClassID][3] = "----";
				permClassItems[ClassID][4] = "----";
				permClassItems[ClassID][5] = "----";
				permClassItems[ClassID][6] = "----";
				permClassItems[ClassID][7] = "----";
				permClassItems[ClassID][8] = "----";
				permClassItems[ClassID][9] = "----";
				permClassItems[ClassID][10] = "----";
				permClassItems[ClassID][11] = "----";
				permClassItems[ClassID][12] = "----";
				permClassItems[ClassID][13] = "----";
				permClassItems[ClassID][14] = "----";
				permClassItems[ClassID][15] = "----";
				permClassItems[ClassID][16] = "----";
				permClassItems[ClassID][17] = "----";
				permClassItems[ClassID][18] = "----";
				permClassItems[ClassID][19] = "----";
				permClassItems[ClassID][20] = "----";
			}

			case "Wizard":{
				int ClassID = getClassID(ClassName);
				permClassItems[ClassID][0] = ClassName;

				permClassItems[ClassID][1] = "Wizard hat";
				permClassItems[ClassID][2] = "Blue wizard hat";
				permClassItems[ClassID][3] = "Master wand";
				permClassItems[ClassID][4] = "----";
				permClassItems[ClassID][5] = "----";
				permClassItems[ClassID][6] = "----";
				permClassItems[ClassID][7] = "----";
				permClassItems[ClassID][8] = "----";
				permClassItems[ClassID][9] = "----";
				permClassItems[ClassID][10] = "----";
				permClassItems[ClassID][11] = "----";
				permClassItems[ClassID][12] = "----";
				permClassItems[ClassID][13] = "----";
				permClassItems[ClassID][14] = "----";
				permClassItems[ClassID][15] = "----";
				permClassItems[ClassID][16] = "----";
				permClassItems[ClassID][17] = "----";
				permClassItems[ClassID][18] = "----";
				permClassItems[ClassID][19] = "----";
				permClassItems[ClassID][20] = "----";
			}
		}
	}

	private int getClassPermanentItems(String currentItem)
	{
		//Check through the array to detect if the weapon name appears anywhere in the allowed items list for that class.
		int classID = getClassID(config.playerClass().toString());

		int i;
		int permFlag = 0;
		String ArrayItem = "";
		currentItem = currentItem.toUpperCase();

		while(permFlag <= 0)
		{
			for(i = 1; i < permClassItems.length; i++)
			{
				ArrayItem = permClassItems[classID][i].toUpperCase();
				if(currentItem.contains(ArrayItem))
				{
					permFlag = 1;
				}
				else
				{
					//Do nothing, leave permanent flag as 0
				}
			}

			if(permFlag != 1)
			{
				permFlag = 2;
			}
		}

		return permFlag;
	}

	public String getCurrentItemName(MenuOptionClicked event)
	{
		//Detect the name of the item that was just clicked on
		//Many thanks to the ItemStats plugin for pointing me in the right direction with the ItemComposition class.

		ItemComposition currentItem = itemManager.getItemComposition(event.getId());
		String itemName = currentItem.getName();
		return itemName;
	}

	private boolean entryMatches(MenuOptionClicked entry, String option)
	{
		return entry.getMenuOption().equals(option);
	}

	private void sendChatMessage(String message)
	{
		//Thanks to the Daily Task Indicators plugin for this!
		final String finalMsg = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append(message)
		        .build();

		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(finalMsg)
				.build());
	}

	@Provides
	ClassesOfGielinorConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ClassesOfGielinorConfig.class);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		//A lot of thanks to Spedwards for the code here. Without their Group Iron Man source code I would never have
		//figured out how to program this kind of method myself.

		String target = Text.removeTags(event.getMenuTarget());

		if ((entryMatches(event,"Cast")))
		{
			if(spellCaster == true || config.classIsSpellcaster() == true)
			{
				//Do nothing as class is a spellcaster
				return;
			}
			else
			{
				//Consume the click and warn the player that they are not a spellcasting class
				event.consume();
				sendChatMessage("The magical arts are a mystery to you. You cannot cast spells.");
				return;
		    }
		}

		if ((entryMatches(event,"Wield"))) {
			String itemName = getCurrentItemName(event);

			if(getAllowedItems(itemName) == 1)
			{
				//Do nothing, item is allowed
			}
			else{
				//Consume the click as they cannot use that item
				event.consume();
				String msgStr = "As a " + config.playerClass().toString() + " you cannot wield the " + itemName;
				sendChatMessage(msgStr);
			}
			return;

		}

		if ((entryMatches(event,"Activate")))
		{
			if(prayerAllowed == true || config.forceAllowPrayer()==true)
			{
				//Player allowed to used prayer
			}
			else
			{
				//Prayer has been disabled for the player
				event.consume();
				String msgStr = "Why would a " + config.playerClass().toString() + " pray to the Gods? I'll pass.";
				sendChatMessage(msgStr);
			}
		}

		//This is disabled as it currently does not work. itemName returns as "Toolkit" on the "Remove" action.
		/*if ((entryMatches(event,"Remove")))
		{
			int PlayerClass = getClassID(config.playerClass().toString());
			String itemName = getCurrentItemName(event);

			if(getClassPermanentItems(itemName) == 1)
			{
				//Do nothing, item removal is allowed
			}
			else
			{
				//Item is a permanent item
				event.consume();
				String msgStr = "But the " + itemName + " is so fitting... it would be wrong to take it off.";
				sendChatMessage(msgStr);
			}
		}*/
	}
}
