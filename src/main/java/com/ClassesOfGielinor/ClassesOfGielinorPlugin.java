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
import javax.swing.*;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	public String classDeity = "";
	public int PrevClass = 0;
	public String PrevAlign = "";

	//Arrays
	public String[][] validClassItems = new String[21][21];
	public String[][] permClassItems = new String[21][21];
	/* Array Structure:
		[x][0] - x will always be a class ID, and [0] will always be the class' string name for easy identification
		[x]][1..n] - n will be an entry in to the array for items names, and will go from 1-n
		Each class will have 20 available "valid item" slots which can be changed in the code.
	 */
	public String[] toolItems = new String[9];
	public String[][] classDialogue = new String[21][21]; //Contains response dialogue for each class.
	public String[][] worldAltars = new String[56][3]; //First number is dictated by how many altars there are in OSRS (listed here: https://oldschool.runescape.wiki/w/Altar)
	/* Array Structure:
		[x][0] - Altar name
		[x][1] - Altar map coordinates (comma separated: x,y,z)
		[x][2] - Altar deity
	 */
	public String[] playerDeities = new String[14]; //Total of 14 gods supported by the plugin
	/* Array Structure:
		[x] - will be populated by God Names by incrementing through 0..n where n is the number of deities relevant to their alignment
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
			//When state changed, get class' restricted items, allowed items, religion, its permanent items, and tools it can use
			getClassRestrictions(config.playerClass().toString());
			setAllowedItems(config.playerClass().toString());

			//Establish player faith and determine which gods they worship
			String deitylist = setReligion(config.playerAlignment().toString());
			setGods(deitylist);
			setAltars();
			setClassPermanentItems(config.playerClass().toString());
			setToolItems();
			return;
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		String currentClass = config.playerClass().toString();
		String currentAlignment = config.playerAlignment().toString();

		//Establish player faith and determine which gods they worship
		String deitylist = setReligion(currentAlignment.toString());
		setGods(deitylist);

		getClassRestrictions(currentClass);
		setReligion(currentAlignment);
		setAllowedItems(currentClass);
		setClassPermanentItems(currentClass);
		setClassDialogue(currentClass);

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
		}

		//Set valid worshippable gods
		if(PrevAlign != config.playerAlignment().toString())			//This also prevents the updater spamming the chat
		{
			if(playerDeities.length > 0)
			{
				commitToFaith();
				PrevAlign = currentAlignment;
			}
			else
			{
				msgStr = "There are no Gods supporting you.";
				sendChatMessage(msgStr);
			}
		}
		return;
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
				classDeity = "!ALL!";
				break;

			case "Bard":
			case "Cleric":
			case "Druid":
			case "Warlock": {
				spellCaster = true;
				disableNonClassItems = true;
				prayerAllowed = true;
				setAllowedItems(config.playerClass().toString());
				break;
			}

			case "Necromancer":
			case "Wizard":{
				spellCaster = true;
				disableNonClassItems = true;
				prayerAllowed = false;
				setAllowedItems(config.playerClass().toString());
				break;
			}

			case "Barbarian":
			case "Chef":
			case "Fighter":
			case "Lumberjack":
			case "Ranger": {
				spellCaster = false;
				disableNonClassItems = true;
				prayerAllowed = false;
				setAllowedItems(config.playerClass().toString());
				break;
			}

			case "Monk":
			case "Paladin":
			case "Rogue":{
				spellCaster = false;
				disableNonClassItems = true;
				prayerAllowed = true;
				setAllowedItems(config.playerClass().toString());
				break;

			}
		}
		return;
	}

	private String setReligion(String PlayerAlignment)
	{
		//Sets the player's religion based on their alignment.
		String Deity = "";

		switch(PlayerAlignment)
		{
			case "Lawful_Good":
			{
				Deity = "Saradomin,Seren";
				break;
			}
			case "Neutral_Good":
			{
				Deity = "Ralos,Ranul";
				break;
			}
			case "Chaotic_Good":
			{
				Deity = "Karamjani,Marimbo";
				break;
			}
			case "Lawful_Neutral":
			{
				Deity = "Armadyl";
				break;
			}
			case "True_Neutral":
			{
				Deity = "Guthix,Elidinis";
				break;
			}
			case "Chaotic_Neutral":
			{
				Deity = "Bandos,Ichthlarin";
				break;
			}
			case "Lawful_Evil":
			{
				Deity = "Zaros";
				break;
			}
			case "Neutral_Evil":
			{
				Deity = "Xeric";
				break;
			}
			case "Chaotic_Evil":
			{
				Deity = "Zamorak";
				break;
			}
		}

		return Deity;
	}

	private void setGods(String Deities)
	{
		String[] gods = Deities.split(",");
		List<String> detectedGods = Arrays.asList(gods);
		detectedGods.toArray(playerDeities);

		//Remove "Null" values from unpopulated spaces of array
		int NumOfListItems = detectedGods.size();

		for(int x = NumOfListItems; x < playerDeities.length; x++)
		{
			playerDeities[x] = "";
		}
	}

	private void commitToFaith()
	{
		String msgStr = "You feel the approval of ";
		for(int i = 0; i < playerDeities.length; i++)
		{
			if(playerDeities[i] == "")
			{
				//Don't append blank spaces to msgStr
			}
			else
			{
				//Get vals of next entry
				String NextVal = playerDeities[i+1];
				//Convert i to string for comparison of next if
				String iStr = Integer.toString(i);

				if(i == 0) //Check if there's only one entry in the list
				{
					if(NextVal =="")  //One value detected in list
					{
						msgStr = msgStr + playerDeities[i] + ".";
					}
					else //More than one value in list, begin regular format
					{
						msgStr = msgStr + playerDeities[i] + ",";
					}
				}
				else
				{
					if (NextVal == "") //Check if next entry is end of list
					{
						msgStr = msgStr + " and " + playerDeities[i];
					}
					else
					{
						msgStr = msgStr + " " + playerDeities[i] + ",";
					}
				}
				//sendChatMessage("===[DEBUG] : i = " + iStr + " | [i] = " + playerDeities[i] + " | NextVal = '" + NextVal + "'===");
			}
		}
		sendChatMessage(msgStr);
	}

	private void setAltars()
	{
		//South-east Varrock;
		worldAltars[0][0] = "Chaos Altar";
		worldAltars[0][1] = "3260,3381,0";
		worldAltars[0][2] = "Zamorak";

		//Underground Pass;
		worldAltars[1][0] = "Chaos Altar";
		worldAltars[1][1] = "NOT,SET,YET";
		worldAltars[1][2] = "Zamorak";

		//Ourania Cave entrance;
		worldAltars[2][0] = "Chaos Altar";
		worldAltars[2][1] = "2455,3231,0";
		worldAltars[2][2] = "Zamorak";

		//Wilderness Chaos Temple;
		worldAltars[3][0] = "Chaos Altar";
		worldAltars[3][1] = "Find Coords";
		worldAltars[3][2] = "Zamorak";

		//Black Knight's Fortress;
		worldAltars[4][0] = "Chaos Altar";
		worldAltars[4][1] = "Find Coords";
		worldAltars[4][2] = "Zamorak";

		//Chaos Temple near Goblin Village;
		worldAltars[5][0] = "Chaos Altar";
		worldAltars[5][1] = "Find Coords";
		worldAltars[5][2] = "Zamorak";

		//Deep wilderness Chaos Temple;
		worldAltars[6][0] = "Chaos Altar";
		worldAltars[6][1] = "Find Coords";
		worldAltars[6][2] = "Zamorak";

		//Yanille Agility dungeon;
		worldAltars[7][0] = "Chaos Altar";
		worldAltars[7][1] = "Find Coords";
		worldAltars[7][2] = "Zamorak";

		//Tutorial Island;
		worldAltars[8][0] = "Altar";
		worldAltars[8][1] = "Find Coords";
		worldAltars[8][2] = "Saradomin";

		//Lumbridge church;
		worldAltars[9][0] = "Altar";
		worldAltars[9][1] = "Find Coords";
		worldAltars[9][2] = "Saradomin";

		//Between Rimmington, Port Sarim and Thurgo;
		worldAltars[10][0] = "Altar";
		worldAltars[10][1] = "Find Coords";
		worldAltars[10][2] = "Saradomin";

		//Duel Arena lobby;
		worldAltars[11][0] = "Altar";
		worldAltars[11][1] = "Find Coords";
		worldAltars[11][2] = "Saradomin";

		//Varrock Palace;
		worldAltars[12][0] = "Altar";
		worldAltars[12][1] = "Find Coords";
		worldAltars[12][2] = "Saradomin";

		//Seers' Village;
		worldAltars[13][0] = "Altar";
		worldAltars[13][1] = "Find Coords";
		worldAltars[13][2] = "Saradomin";

		//East Ardougne;
		worldAltars[14][0] = "Altar";
		worldAltars[14][1] = "Find Coords";
		worldAltars[14][2] = "Saradomin";

		//West Ardougne;
		worldAltars[15][0] = "Altar";
		worldAltars[15][1] = "Find Coords";
		worldAltars[15][2] = "Saradomin";

		//Ardougne Monastery;
		worldAltars[16][0] = "Altar";
		worldAltars[16][1] = "Find Coords";
		worldAltars[16][2] = "Saradomin";

		//Paterdomus;
		worldAltars[17][0] = "Altar";
		worldAltars[17][1] = "Find Coords";
		worldAltars[17][2] = "Saradomin";

		//Entrana;
		worldAltars[18][0] = "Altar";
		worldAltars[18][1] = "Find Coords";
		worldAltars[18][2] = "Saradomin";

		//Edgeville Monastery;
		worldAltars[19][0] = "Altar";
		worldAltars[19][1] = "Find Coords";
		worldAltars[19][2] = "Saradomin";

		//Well of Voyage;
		worldAltars[20][0] = "Altar";
		worldAltars[20][1] = "Find Coords";
		worldAltars[20][2] = "Saradomin";

		//Lletya;
		worldAltars[21][0] = "Altar";
		worldAltars[21][1] = "Find Coords";
		worldAltars[21][2] = "Seren";

		//Tower of Voices;
		worldAltars[22][0] = "Altar";
		worldAltars[22][1] = "Find Coords";
		worldAltars[22][2] = "Seren";

		//Prifddinas;
		worldAltars[23][0] = "Altar";
		worldAltars[23][1] = "Find Coords";
		worldAltars[23][2] = "Seren";

		//Gorlah;
		worldAltars[24][0] = "Altar";
		worldAltars[24][1] = "Find Coords";
		worldAltars[24][2] = "Seren";

		//North-eastern Varrock;
		worldAltars[25][0] = "Altar";
		worldAltars[25][1] = "Find Coords";
		worldAltars[25][2] = "Saradomin";

		//Kourend Castle;
		worldAltars[26][0] = "Altar";
		worldAltars[26][1] = "Find Coords";
		worldAltars[26][2] = "Saradomin";

		//Witchaven;
		worldAltars[27][0] = "Altar";
		worldAltars[27][1] = "Find Coords";
		worldAltars[27][2] = "Saradomin";

		//Camelot;
		worldAltars[28][0] = "Altar";
		worldAltars[28][1] = "Find Coords";
		worldAltars[28][2] = "Saradomin";

		//Heroes' Guild;
		worldAltars[29][0] = "Altar";
		worldAltars[29][1] = "Find Coords";
		worldAltars[29][2] = "Saradomin";

		//Sophanem;
		worldAltars[30][0] = "Altar";
		worldAltars[30][1] = "Find Coords";
		worldAltars[30][2] = "Icthlarin";

		//Hosidius' church;
		worldAltars[31][0] = "Altar";
		worldAltars[31][1] = "Find Coords";
		worldAltars[31][2] = "Saradomin";

		//Hosidius monk's camp;
		worldAltars[32][0] = "Altar";
		worldAltars[32][1] = "Find Coords";
		worldAltars[32][2] = "Saradomin";

		//Arceuus church;
		worldAltars[33][0] = "Altar";
		worldAltars[33][1] = "Find Coords";
		worldAltars[33][2] = "None";

		//Lovakengj;
		worldAltars[34][0] = "Altar";
		worldAltars[34][1] = "Find Coords";
		worldAltars[34][2] = "Unknown";

		//Molch;
		worldAltars[35][0] = "Altar";
		worldAltars[35][1] = "Find Coords";
		worldAltars[35][2] = "Xeric";

		//Xeric's Shrine in Kebos Swamp;
		worldAltars[36][0] = "Altar";
		worldAltars[36][1] = "Find Coords";
		worldAltars[36][2] = "Xeric";

		//Myths' Guild;
		worldAltars[37][0] = "Altar";
		worldAltars[37][1] = "Find Coords";
		worldAltars[37][2] = "Unknown";

		//Forthos Dungeon;
		worldAltars[38][0] = "Altar";
		worldAltars[38][1] = "Find Coords";
		worldAltars[38][2] = "Ranul";

		//Ferox Enclave;
		worldAltars[39][0] = "Altar";
		worldAltars[39][1] = "Find Coords";
		worldAltars[39][2] = "Zaros";

		//Citharede Abbey on Desert Plateau;
		worldAltars[40][0] = "Altar";
		worldAltars[40][1] = "Find Coords";
		worldAltars[40][2] = "Saradomin";

		//Clan Hall chapel;
		worldAltars[41][0] = "Altar";
		worldAltars[41][1] = "Find Coords";
		worldAltars[41][2] = "None";

		//Taverley stone circle;
		worldAltars[42][0] = "Altar of Guthix";
		worldAltars[42][1] = "Find Coords";
		worldAltars[42][2] = "Guthix";

		//Nature grotto in Mort Myre Swamp;
		worldAltars[43][0] = "Altar of nature";
		worldAltars[43][1] = "Find Coords";
		worldAltars[43][2] = "Guthix";

		//God Wars Dungeon;
		worldAltars[44][0] = "Zamorak Altar";
		worldAltars[44][1] = "Find Coords";
		worldAltars[44][2] = "Zamorak";

		//God Wars Dungeon;
		worldAltars[45][0] = "Saradomin Altar";
		worldAltars[45][1] = "Find Coords";
		worldAltars[45][2] = "Saradomin";

		//God Wars Dungeon;
		worldAltars[46][0] = "Armadyl Altar";
		worldAltars[46][1] = "Find Coords";
		worldAltars[46][2] = "Armadyl";

		//God Wars Dungeon;
		worldAltars[47][0] = "Bandos Altar";
		worldAltars[47][1] = "Find Coords";
		worldAltars[47][2] = "Bandos";

		//Shade Catacombs;
		worldAltars[48][0] = "Altar";
		worldAltars[48][1] = "Find Coords";
		worldAltars[48][2] = "Unknown";

		//Slepe church;
		worldAltars[49][0] = "Altar of Zamorak";
		worldAltars[49][1] = "Find Coords";
		worldAltars[49][2] = "Zamorak";

		//Forthos dungeon;
		worldAltars[50][0] = "Broken sun altar";
		worldAltars[50][1] = "Find Coords";
		worldAltars[50][2] = "Ralos";

		//Woodcutting Guild;
		worldAltars[51][0] = "Shrine";
		worldAltars[51][1] = "Find Coords";
		worldAltars[51][2] = "Evil Chicken";

		//Tai Bwo Wannai;
		worldAltars[52][0] = "Tribal Statue";
		worldAltars[52][1] = "Find Coords";
		worldAltars[52][2] = "Karamjan gods";

		//Darkmeyer;
		worldAltars[53][0] = "Statue";
		worldAltars[53][1] = "Find Coords";
		worldAltars[53][2] = "Unknown";

		//Temple of Marimbo;
		worldAltars[54][0] = "Gorilla Statue";
		worldAltars[54][1] = "Find Coords";
		worldAltars[54][2] = "Marimbo";

		//Nardah;
		worldAltars[55][0] = "Elidinis Statuette";
		worldAltars[55][1] = "Find Coords";
		worldAltars[55][2] = "Elidinis";
	}

	private void setAllowedItems(String PlayerClass)
	{
		//I apologise for how hard coded this section is. Unfortunately by the nature of the restrictions there is no way to automate it.
		//(Unless I use an XML file of some kind, but I'd prefer not to upload lots of bespoke files to the plugin-hub repository.)
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
				validClassItems[classID][16] = "scimitar";
				validClassItems[classID][17] = "shield";
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
				validClassItems[classID][10] = "shield";
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
				validClassItems[classID][3] = " saw";
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
				validClassItems[classID][6] = "book of";
				validClassItems[classID][7] = "tome";
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
				validClassItems[classID][7] = "mage";
				validClassItems[classID][8] = "book of";
				validClassItems[classID][9] = "tome";
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
				validClassItems[classID][3] = "----";
				validClassItems[classID][4] = "flail";
				validClassItems[classID][5] = "2h";
				validClassItems[classID][6] = "hasta";
				validClassItems[classID][7] = "spear";
				validClassItems[classID][8] = "bludgeon";
				validClassItems[classID][9] = "banner";
				validClassItems[classID][10] = "mjolnir";
				validClassItems[classID][11] = "blessing";
				validClassItems[classID][12] = " sword";
				validClassItems[classID][13] = " longsword";
				validClassItems[classID][14] = "shield";
				validClassItems[classID][15] = "light"; //All variations of Silverlight
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
				validClassItems[classID][2] = "thrownaxe";
				validClassItems[classID][3] = "great";
				validClassItems[classID][4] = "hammer";
				validClassItems[classID][5] = "spear";
				validClassItems[classID][6] = "javelin";
				validClassItems[classID][7] = "club";
				validClassItems[classID][8] = "maul";
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

			case "Bard": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "rapier";
				validClassItems[classID][2] = "dagger";
				validClassItems[classID][3] = "scimitar";
				validClassItems[classID][4] = "lyre";
				validClassItems[classID][5] = "knife";
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

			case "Druid": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "secateurs";
				validClassItems[classID][2] = "staff";
				validClassItems[classID][3] = "Guthix";
				validClassItems[classID][4] = "sickle";
				validClassItems[classID][5] = "dagger";
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

			case "Fighter": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "longsword";
				validClassItems[classID][2] = "2h";
				validClassItems[classID][3] = "axe";
				validClassItems[classID][4] = "hammer";
				validClassItems[classID][5] = "scimitar";
				validClassItems[classID][6] = "rapier";
				validClassItems[classID][7] = "battle";
				validClassItems[classID][8] = "mace";
				validClassItems[classID][9] = "dagger";
				validClassItems[classID][10] = " sword";
				validClassItems[classID][11] = "shield";
				validClassItems[classID][12] = "defender";
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

			case "Monk": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "claws";
				validClassItems[classID][2] = "whip";
				validClassItems[classID][3] = "sickle";
				validClassItems[classID][4] = "Battlestaff";
				validClassItems[classID][5] = "holy water";
				validClassItems[classID][6] = "blessing";
				validClassItems[classID][7] = "shield";
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

			case "Rogue": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "dagger";
				validClassItems[classID][2] = "dart";
				validClassItems[classID][3] = "knife";
				validClassItems[classID][4] = "crossbow";
				validClassItems[classID][5] = "bolt";
				validClassItems[classID][6] = "blackjack";
				validClassItems[classID][7] = "rapier";
				validClassItems[classID][8] = "defender";
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

			case "Warlock": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "staff";
				validClassItems[classID][2] = "wand";
				validClassItems[classID][3] = "blessing";
				validClassItems[classID][4] = "tome";
				validClassItems[classID][5] = "book of";
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
		}
		return;
	}

	private int getClassID(String PlayerClass)
	{
		//This method is responsible for setting the classID - a variable used by most methods to determine restrictions.
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
			case "Barbarian":
				classID = 8;
				break;
			case "Bard":
				classID = 9;
				break;
			case "Druid":
				classID = 10;
				break;
			case "Fighter":
				classID = 11;
				break;
			case "Monk":
				classID = 12;
				break;
			case "Rogue":
				classID = 13;
				break;
			case "Warlock":
				classID = 14;
				break;
		}
		return classID;
	}

	private void setClassDialogue(String ClassName)
	{
		/* One idea I have is to implement different chat lines for interactions of different classes. For example, when trying to pray
		   a necromancer may say "Foolishness, no God will help me achieve my destiny." whereas a barbarian may say "Weak God not help, only beer!"

		   Could be a cool idea. Requires a lot of work, and a lot of observation in to what action is currently being used.. but could be fun.
		*/
		switch(ClassName)
		{

		}
	}

	private void setClassPermanentItems(String ClassName)
	{
		/* Permanent items are items that are considered "true to class", such as a Chef Hat for the Chef.
		   Once equipped, these items cannot be unequipped, as they are the "best in slot" for that class.

		   Quite a fun idea, even if it does limit gear setups. Perhaps it could be overridden by a tick box in Config.
		 */

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
		/* A lot of thanks to Spedwards for the code here. Without their Group Iron Man source code I would never have
		   figured out how to program this kind of method myself.

		   This method intercepts each action made against an entity and compares it to a list of events. I used this
		   to determine if the action being made is something that could be restricted; such as spells being cast, prayers
		   being used, and items being equipped.

		   Typically this method could be quite large if the plugin evolves to inspect a lot more actions, but generally
		   it should be reasonably low impact. It will also consume the action if deemed restricted, meaning the action will
		   not be taken in to account.
		*/

		String target = Text.removeTags(event.getMenuTarget());

		//Spell Cast Observer
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

		//Item Wield Observer
		if ((entryMatches(event,"Wield")))
		{
			String itemName = getCurrentItemName(event);

			if(compareToItemArrays(itemName,validClassItems) || compareToTools(itemName) || config.enableNonClassItems())
			{
				//Do nothing, item is allowed for character's class
			}
			else{
				//Consume the click as they cannot use that item
				event.consume();
				String msgStr = "As a " + config.playerClass().toString() + " you cannot wield the " + itemName + ".";
				sendChatMessage(msgStr);
			}
			return;

		}

		//Prayer Activation Observer
		if ((entryMatches(event,"Activate")))
		{
			if(prayerAllowed || config.forceAllowPrayer())
			{
				//Do nothing, character's class is permitted to use prayer
			}
			else
			{
				//Prayer has been disabled for the player
				event.consume();
				String msgStr = "Why would a " + config.playerClass().toString() + " pray to the Gods? I'll pass.";
				sendChatMessage(msgStr);
			}
		}

		//Praying at Altars Observer
		if ((entryMatches(event,"Pray-at")) || (entryMatches(event,"Pray"))) {
			/* Eventually this section could be used to determine if the player is praying at a specific altar
			   and determine whether or not they would be permitted to pray at an altar dedicated to a deity or not.

			   The best way to do this is with a new array for a list of altars with their deity and world location.
			   The method will compare the player's location to the location of the altar, and determine if the player
			   can worship that god or not. This would also need a new "deity" section for each class which it will
			   check against.

			*/
		}

	}
}
