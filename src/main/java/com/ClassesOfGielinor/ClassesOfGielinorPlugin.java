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
	public int PrevClass = 0;

	//Arrays
	public String[][] validClassItems = new String[21][21];
	public String[][] permClassItems = new String[21][21];
	/* Array Structure:
		[x][0] - x will always be a class ID, and [0] will always be the class' string name for easy identification
		[x]][1..n] - n will be an entry in to the array for items names, and will go from 1-n
		Each class will have 20 available "valid item" slots which can be changed in the code.
	 */
	public String[] toolItems = new String[9];

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
			getClassRestrictions(config.playerClass().toString());
			setAllowedItems(config.playerClass().toString());
			setClassPermanentItems(config.playerClass().toString());
			setToolItems();
			return;
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		String currentClass = config.playerClass().toString();

		getClassRestrictions(currentClass);
		setAllowedItems(currentClass);
		setClassPermanentItems(currentClass);

		String msgStr = "";

		if (PrevClass != getClassID(currentClass))  		//This prevents the updater spamming the chat.
		{
			if (currentClass == "None") {
				msgStr = "You do not have a class set.";
			} else {
				msgStr = "Your class has been set to " + currentClass + ".";
				PrevClass = getClassID(config.playerClass().toString());
			}
			sendChatMessage(msgStr);

			if (spellCaster || config.classIsSpellcaster()) {
				msgStr = "You feel magical energies empower you.";
			} else {
				msgStr = "";
			}

			if (msgStr == "") {
				//Do not broadcast
			} else {
				sendChatMessage(msgStr);
			}

			if (prayerAllowed || config.forceAllowPrayer()) {
				sendChatMessage("The blessing of the Gods is with you.");
			}

			return;
		}
	}

	private void setToolItems()
	{
		toolItems[0] = "Glassblowing pipe";
		toolItems[1] = "Secateurs";
		toolItems[2] = " Rod";
		toolItems[3] = "Harpoon";
		toolItems[4] = " Axe";
		toolItems[5] = "Machete";
		toolItems[6] = "Pickaxe";
		toolItems[7] = "Butterfly Net";
		toolItems[8] = "Teasing stick";
	}

	private boolean compareToTools(String currentItem)
	{
		int i;
		int FoundFlag = 0;
		String ArrayItem;
		currentItem = currentItem.toUpperCase();

		while(FoundFlag <= 0)
		{
			for(i=0; i < toolItems.length; i++)
			{
				ArrayItem = toolItems[i].toUpperCase();
				if(ArrayItem.contains(currentItem))
				{
					FoundFlag = 1;
				}
			}
			FoundFlag = 2;
		}

		if (FoundFlag == 1)
		{
			return true;
		}
		else
		{
			return false;
		}

	}

	private boolean compareToItemArrays(String entity, String[][] setArray)
	{
		//Check through the array to detect if the item name appears anywhere in the items list for that class.
		int classID = getClassID(config.playerClass().toString());

		int FoundFlag = 0;
		int i;
		String ArrayItem;
		entity = entity.toUpperCase();

		while(FoundFlag <= 0)
		{
			for(i = 1; i < setArray.length; i++)
			{
				ArrayItem = setArray[classID][i].toUpperCase();
				if(entity.contains(ArrayItem) || ArrayItem == "!ALL!")
				{
					FoundFlag = 1;
				}
			}

			if(FoundFlag != 1)
			{
				FoundFlag = 2;
			}
		}

		if(FoundFlag == 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	private void getClassRestrictions(String PlayerClass)
	{
		switch(PlayerClass)
		{
			case "None":
			default:
				spellCaster = true;
				disableNonClassItems = false;
				prayerAllowed = true;
				setAllowedItems(config.playerClass().toString());
				break;

			case "Bard":
			case "Cleric":
			case "Necromancer":
			case "Druid":
			case "Paladin":
			case "Juggler"{
				spellCaster = true;
				disableNonClassItems = true;
				prayerAllowed = true;
				setAllowedItems(config.playerClass().toString());
				break;
			}

			case "Barbarian":
			case "Rogue":
			case "Fighter"{
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

			case "Juggler":{
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;
				validClassItems[classID][1] = "sword";
				validClassItems[classID][2] = "thrownaxe";
				validClassItems[classID][3] = "thrownknife";
				validClassItems[classID][4] = "darts";
				validClassItems[classID][5] = "wand";
				validClassItems[classID][6] = "book";
				validClassItems[classID][7] = "defender";
				validClassItems[classID][8] = "whip";
				validClassItems[classID][9] = "bludgeon";
				validClassItems[classID][10] = "abyssaldagger";
				validClassItems[classID][11] = "lance";
				validClassItems[classID][12] = "dragoncrossbow";
				validClassItems[classID][13] = "armadylcrossbow";
				validClassItems[classID][14] = "nightmarestaff";
				validClassItems[classID][15] = "sanguinestistaff";
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
				validClassItems[classID][1] = "sword";
				validClassItems[classID][2] = "hammer";
				validClassItems[classID][3] = "wand";
				validClassItems[classID][4] = "book";
				validClassItems[classID][5] = "trident";
				validClassItems[classID][6] = "saradomingodsword";
				validClassItems[classID][7] = "saradominsword";
				validClassItems[classID][8] = "saradominstaff";
				validClassItems[classID][9] = "lance";
				validClassItems[classID][10] = "staffoflight";
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
				validClassItems[classID][1] = "sword";
				validClassItems[classID][2] = "axe";
				validClassItems[classID][3] = "elestaff";
				validClassItems[classID][4] = "book";
				validClassItems[classID][5] = "whip";
				validClassItems[classID][6] = "bludgeon";
				validClassItems[classID][7] = "abyssaldagger";
				validClassItems[classID][8] = "zamorakgodsword";
				validClassItems[classID][9] = "zamorakspear";
				validClassItems[classID][10] = "zamorakstaff";
				validClassItems[classID][11] = "staffofthedead";
				validClassItems[classID][12] = "ancientstaff";
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
				validClassItems[classID][2] = "crossbow";
				validClassItems[classID][3] = "scimitar";
				validClassItems[classID][4] = "arrow";
				validClassItems[classID][5] = "bolt";
				validClassItems[classID][6] = "shield";
				validClassItems[classID][7] = "whip";
				validClassItems[classID][8] = "maul";
				validClassItems[classID][9] = "lance";
				validClassItems[classID][10] = "ballistas";
				validClassItems[classID][11] = "blowpipe";
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
				validClassItems[classID][1] = "hammer";
				validClassItems[classID][2] = "longsword";
				validClassItems[classID][3] = "elestaff";
				validClassItems[classID][4] = "shield";
				validClassItems[classID][5] = "lance";
				validClassItems[classID][6] = "saradomingodsword";
				validClassItems[classID][7] = "saradominstaff";
				validClassItems[classID][8] = "saradominsword";
				validClassItems[classID][9] = "staffoflight";
				validClassItems[classID][10] = "light";
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

			case "Barbarian": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;
				validClassItems[classID][1] = "axe";
				validClassItems[classID][2] = "maul";
				validClassItems[classID][3] = "thrownaxe";
				validClassItems[classID][4] = "thrownknife";
				validClassItems[classID][5] = "shield";
				validClassItems[classID][6] = "anchor";
				validClassItems[classID][7] = "lance";
				validClassItems[classID][8] = "maul";
				validClassItems[classID][9] = "bludgeon";
				validClassItems[classID][10] = "ballistas";
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

			case "Bard": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;
				validClassItems[classID][1] = "rapier";
				validClassItems[classID][2] = "dagger";
				validClassItems[classID][3] = "wand";
				validClassItems[classID][4] = "book";
				validClassItems[classID][5] = "lance";
				validClassItems[classID][6] = "bladeofsaeldor";
				validClassItems[classID][7] = "trident";
				validClassItems[classID][8] = "sanguinestistaff";
				validClassItems[classID][9] = "nightmarestaff";
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

			case "Druid": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;
				validClassItems[classID][1] = "sword";
				validClassItems[classID][2] = "mace";
				validClassItems[classID][3] = "elestaff";
				validClassItems[classID][4] = "shield";
				validClassItems[classID][5] = "lance";
				validClassItems[classID][6] = "trident";
				validClassItems[classID][7] = "guthixstaff";
				validClassItems[classID][8] = "nightmarestaff";
				validClassItems[classID][9] = "sanguinestistaff";
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

			case "Fighter": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;
				validClassItems[classID][1] = "longsword";
				validClassItems[classID][2] = "sword";
				validClassItems[classID][3] = "scimitar";
				validClassItems[classID][4] = "bow";
				validClassItems[classID][5] = "arrow";
				validClassItems[classID][6] = "defender";
				validClassItems[classID][7] = "whip";
				validClassItems[classID][8] = "lance";
				validClassItems[classID][9] = "maul";
				validClassItems[classID][10] = "scythe";
				validClassItems[classID][11] = "ballistas";
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

			case "Rogue": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;
				validClassItems[classID][1] = "dagger";
				validClassItems[classID][2] = "rapier";
				validClassItems[classID][3] = "crossbow";
				validClassItems[classID][4] = "defender";
				validClassItems[classID][5] = "bolt";
				validClassItems[classID][6] = "whip";
				validClassItems[classID][7] = "scythe";
				validClassItems[classID][8] = "lance";
				validClassItems[classID][9] = "ballistas";
				validClassItems[classID][10] = "blowpipe";
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
			case "Juggler":
				classID = 1;
				break;
			case "Cleric":
				classID = 2;
				break;
			case "Necromancer":
				classID = 3;
				break;
			case "Ranger":
				classID = 4;
				break;
			case "Paladin":
				classID = 5;
				break;
			case "Barbarian":
				classID = 6;
				break;
			case "Bard":
				classID = 7;
				break;
			case "Druid":
				classID = 8;
				break;
			case "Fighter":
				classID = 9;
				break;
			case "Rogue":
				classID = 10;
				break;
		}
		return classID;
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

			if(compareToItemArrays(itemName,validClassItems) || compareToTools(itemName) || config.enableNonClassItems())
			{
				//Do nothing, item is allowed
			}
			else{
				//Consume the click as they cannot use that item
				event.consume();
				String msgStr = "As a " + config.playerClass().toString() + " you cannot wield the " + itemName + ".";
				sendChatMessage(msgStr);
			}
			return;

		}

		if ((entryMatches(event,"Activate")) || (entryMatches(event,"Pray-at")))
		{
			if(prayerAllowed || config.forceAllowPrayer())
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
