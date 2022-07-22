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
import net.runelite.client.util.GameEventManager;
import net.runelite.client.util.Text;
import net.runelite.api.coords.WorldPoint;
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
	String msgStr = "";

	//Arrays
	public String[][] validClassItems = new String[21][21];
	public String[][] permClassItems = new String[21][21];
	/* Array Structure:
		[x][0] - x will always be a class ID, and [0] will always be the class' string name for easy identification
		[x]][1..n] - n will be an entry in to the array for items names, and will go from 1-n
		Each class will have 20 available "valid item" slots which can be changed in the code.
	 */
	public String[][] questItems = new String[128][2];
	/* Array Structure:
		[x][0] = Name of Item
		[x][1] = Name of Quest
	 */

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

			//Set item arrays
			setClassPermanentItems(config.playerClass().toString());
			setQuestItems();
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

		msgStr = "";

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
				msgStr = "Your lack of faith prevents you from obtaining blessings.";
				sendChatMessage(msgStr);
			}
		}
		return;
	}

	private void setQuestItems()
	{
		// Item Names
		questItems[0][0] = "Ogre bellows";
		questItems[1][0] = "Spiked boots";
		questItems[2][0] = "Trowel";
		questItems[3][0] = "Battered book";
		questItems[4][0] = "Battered key";
		questItems[5][0] = "Fake beard";
		questItems[6][0] = "Red hot sauce";
		questItems[7][0] = "Red vine worm";
		questItems[8][0] = "Dramen branch";
		questItems[9][0] = "Redberry pie";
		questItems[10][0] = "Black mushroom";
		questItems[11][0] = "Strange implement";
		questItems[12][0] = "Black mushroom ink";
		questItems[13][0] = "Dramen branch";
		questItems[14][0] = "Karamjan rum";
		questItems[15][0] = "Hangover cure";
		questItems[16][0] = "Ghostspeak amulet";
		questItems[17][0] = "Broken glass";
		questItems[18][0] = "Rogue's purse";
		questItems[19][0] = "Glarial's pebble";
		questItems[20][0] = "A key";
		questItems[21][0] = "Glarial's amulet";
		questItems[22][0] = "Door key";
		questItems[23][0] = "H.A.M. robes";
		questItems[24][0] = "Fuse";
		questItems[25][0] = "Commorb";
		questItems[26][0] = "Goutweed";
		questItems[27][0] = "Storeroom key";
		questItems[28][0] = "Seal of passage";
		questItems[29][0] = "Nettle tea";
		questItems[30][0] = "Ice gloves";
		questItems[31][0] = "Airtight pot";
		questItems[32][0] = "Animate rock scroll";
		questItems[33][0] = "M'speak amulet";
		questItems[34][0] = "Monkey greegree";
		questItems[35][0] = "Barrel of naphtha";
		questItems[36][0] = "Barrel of coal-tar";
		questItems[37][0] = "Barrel";
		questItems[38][0] = "Ogre bow";
		questItems[39][0] = "Silverlight";
		questItems[40][0] = "Catspeak amulet";
		questItems[41][0] = "Silver sickle (b)";
		questItems[42][0] = "Mourner gear";
		questItems[43][0] = "Excalibur";
		questItems[44][0] = "Dramen staff";
		questItems[45][0] = "Snake charm";
		questItems[46][0] = "Ugthanki dung";
		questItems[47][0] = "Enchanted lyre";
		questItems[48][0] = "Snake charm";
		questItems[49][0] = "Ring of visibility";
		questItems[50][0] = "Gnome amulet";
		questItems[51][0] = "Crystal-mine key";
		questItems[52][0] = "Blurite ore";
		questItems[53][0] = "Beaten book";
		questItems[54][0] = "Dwarven helmet";
		questItems[55][0] = "Klank's gauntlets";
		questItems[56][0] = "Crystal bow/shield";
		questItems[57][0] = "Brooch";
		questItems[58][0] = "Ring of charos (a)";
		questItems[59][0] = "Lunar staff";
		questItems[60][0] = "Kitten";
		questItems[61][0] = "Eagle cape";
		questItems[62][0] = "Fake beak";
		questItems[63][0] = "Camel mask";
		questItems[64][0] = "Bomber jacket";
		questItems[65][0] = "Bomber cap";
		questItems[66][0] = "Origami balloon";
		questItems[67][0] = "'perfect' ring";
		questItems[68][0] = "'perfect' necklace";
		questItems[69][0] = "Shoes";
		questItems[70][0] = "Kharidian headpiece";
		questItems[71][0] = "Desert disguise";
		questItems[72][0] = "Khazard armour";
		questItems[73][0] = "Pet rock";
		questItems[74][0] = "Fremennik blade";
		questItems[75][0] = "Hazeel's mark";
		questItems[76][0] = "Carnillean armour";
		questItems[77][0] = "Blurite sword";
		questItems[78][0] = "Hazard suit";
		questItems[79][0] = "Slave robes";
		questItems[80][0] = "Builder's outfit";
		questItems[81][0] = "Golden helmet";
		questItems[82][0] = "Gadderhammer";
		questItems[83][0] = "Silvthrill rod";
		questItems[84][0] = "Rod of ivandis";
		questItems[85][0] = "Beads of the dead";
		questItems[86][0] = "Zamorak robes";
		questItems[87][0] = "Dark dagger";
		questItems[88][0] = "Crystal pendant";
		questItems[89][0] = "Fixed device";
		questItems[90][0] = "Rat pole";
		questItems[91][0] = "Darklight";
		questItems[92][0] = "Doctors/nurse hat";
		questItems[93][0] = "Bedsheet";
		questItems[94][0] = "Initiate sallet";
		questItems[95][0] = "Lunar armour/gear";
		questItems[96][0] = "Camulet";
		questItems[97][0] = "Crystal seed";
		questItems[98][0] = "Crystal trinket";
		questItems[99][0] = "Fishing pass";
		questItems[100][0] = "Blessed gold bowl";
		questItems[101][0] = "Mouse toy";
		questItems[102][0] = "Prayer book";
		questItems[103][0] = "Wrought iron key";
		questItems[104][0] = "Beacon ring";
		questItems[105][0] = "Dwarven rock cake";
		questItems[106][0] = "Barrows gloves";
		questItems[107][0] = "Locating crystal";
		questItems[108][0] = "Bervirius notes";
		questItems[109][0] = "Black prism";
		questItems[110][0] = "Ammo mould";
		questItems[111][0] = "Steel gauntlets";
		questItems[112][0] = "Blackjack";
		questItems[113][0] = "Magic secateurs";
		questItems[114][0] = "Karambwan vessel";
		questItems[115][0] = "Steel key ring";
		questItems[116][0] = "Ectophial";
		questItems[117][0] = "Teleport crystal";
		questItems[118][0] = "Book o' piracy";
		questItems[119][0] = "Sled";
		questItems[120][0] = "Armadyl pendant";
		questItems[121][0] = "Ancient mace";
		questItems[122][0] = "Bull roarer";
		questItems[123][0] = "Holy wrench";
		questItems[124][0] = "Ava's device";
		questItems[125][0] = "Iban's staff";
		questItems[126][0] = "Keris";
		questItems[127][0] = "Barrelchest anchor";
		questItems[128][0] = "Ring of charos";
		questItems[129][0] = "-----";

		//Quest Names
		questItems[0][1] = "Big Chompy Bird Hunting";
		questItems[1][1] = "Death Plateau";
		questItems[2][1] = "The Digsite";
		questItems[3][1] = "Elemental Workshop";
		questItems[4][1] = "Elemental Workshop";
		questItems[5][1] = "The Feud";
		questItems[6][1] = "The Feud";
		questItems[7][1] = "Fishing Contest";
		questItems[8][1] = "The Fremennik Trials";
		questItems[9][1] = "The Giant Dwarf";
		questItems[10][1] = "The Golem";
		questItems[11][1] = "The Golem";
		questItems[12][1] = "The Golem";
		questItems[13][1] = "Lost City";
		questItems[14][1] = "Pirate's Treasure";
		questItems[15][1] = "Plague City";
		questItems[16][1] = "The Restless Ghost";
		questItems[17][1] = "Sea Slug";
		questItems[18][1] = "Shades of Mort'ton";
		questItems[19][1] = "Waterfall Quest";
		questItems[20][1] = "Waterfall Quest";
		questItems[21][1] = "Waterfall Quest";
		questItems[22][1] = "Witch's House";
		questItems[23][1] = "The Lost Tribe";
		questItems[24][1] = "Cabin Fever";
		questItems[25][1] = "Wanted!";
		questItems[26][1] = "Eadgar's Ruse";
		questItems[27][1] = "Eadgar's Ruse";
		questItems[28][1] = "Lunar Diplomacy";
		questItems[29][1] = "Ghosts Ahoy";
		questItems[30][1] = "Heroes Quest";
		questItems[31][1] = "One Small Favour";
		questItems[32][1] = "One Small Favour";
		questItems[33][1] = "Monkey Madness";
		questItems[34][1] = "Monkey Madness";
		questItems[35][1] = "Regicide";
		questItems[36][1] = "Regicide";
		questItems[37][1] = "Regicide";
		questItems[38][1] = "Big Chompy Bird Hunting";
		questItems[39][1] = "Demon Slayer";
		questItems[40][1] = "Icthlarin's Little Helper";
		questItems[41][1] = "Nature Spirit";
		questItems[42][1] = "Mourning's End Part I";
		questItems[43][1] = "Merlin's Crystal";
		questItems[44][1] = "Lost City";
		questItems[45][1] = "Rat Catchers, The Feud";
		questItems[46][1] = "The Feud";
		questItems[47][1] = "The Fremennik Trials";
		questItems[48][1] = "The Feud";
		questItems[49][1] = "Desert Treasure";
		questItems[50][1] = "Tree Gnome Village";
		questItems[51][1] = "Haunted Mine";
		questItems[52][1] = "The Knight's Sword";
		questItems[53][1] = "Elemental Workshop Part II";
		questItems[54][1] = "Grim Tales";
		questItems[55][1] = "Underground Pass";
		questItems[56][1] = "Roving Elves";
		questItems[57][1] = "The Lost Tribe";
		questItems[58][1] = "Garden of Tranquillity";
		questItems[59][1] = "Lunar Diplomacy";
		questItems[60][1] = "Gertrude's Cat";
		questItems[61][1] = "Eagles' Peak";
		questItems[62][1] = "Eagles' Peak";
		questItems[63][1] = "Enakhra's Lament";
		questItems[64][1] = "Enlightened Journey";
		questItems[65][1] = "Enlightened Journey";
		questItems[66][1] = "Enlightened Journey";
		questItems[67][1] = "Family Crest";
		questItems[68][1] = "Family Crest";
		questItems[69][1] = "Spirit of the Elid";
		questItems[70][1] = "The Feud";
		questItems[71][1] = "The Feud";
		questItems[72][1] = "Fight Arena";
		questItems[73][1] = "The Fremennik Trials";
		questItems[74][1] = "The Fremennik Trials";
		questItems[75][1] = "Hazeel Cult";
		questItems[76][1] = "Hazeel Cult";
		questItems[77][1] = "The Knight's Sword";
		questItems[78][1] = "Sheep Herder";
		questItems[79][1] = "The Tourist Trap";
		questItems[80][1] = "Tower of Life";
		questItems[81][1] = "Between A Rock...";
		questItems[82][1] = "In Aid of the Myreque";
		questItems[83][1] = "In Aid of the Myreque";
		questItems[84][1] = "In Aid of the Myreque";
		questItems[85][1] = "Shilo Village";
		questItems[86][1] = "Underground Pass";
		questItems[87][1] = "Legend's Quest";
		questItems[88][1] = "Regicide";
		questItems[89][1] = "Mourning's End Part I";
		questItems[90][1] = "Rat Catchers";
		questItems[91][1] = "Shadow of the Storm";
		questItems[92][1] = "A Tail of Two Cats";
		questItems[93][1] = "Ghosts Ahoy";
		questItems[94][1] = "Recruitment Drive";
		questItems[95][1] = "Lunar Diplomacy";
		questItems[96][1] = "Enakhra's Lament";
		questItems[97][1] = "The Eyes of Glouphrie";
		questItems[98][1] = "Mourning's End Part II";
		questItems[99][1] = "Fishing Contest";
		questItems[100][1] = "Legend's Quest";
		questItems[101][1] = "A Tail of Two Cats";
		questItems[102][1] = "The Great Brain Robbery";
		questItems[103][1] = "The Tourist Trap";
		questItems[104][1] = "What Lies Below";
		questItems[105][1] = "Recipe For Disaster";
		questItems[106][1] = "Recipe For Disaster";
		questItems[107][1] = "Shilo Village";
		questItems[108][1] = "Shilo Village";
		questItems[109][1] = "Zogre Flesh Eaters";
		questItems[110][1] = "Dwarf Cannon";
		questItems[111][1] = "Family Crest";
		questItems[112][1] = "The Feud";
		questItems[113][1] = "A Fairytale Part I - Growing Pains";
		questItems[114][1] = "Tai Bwo Wannai Trio";
		questItems[115][1] = "One Small Favour";
		questItems[116][1] = "Ghosts Ahoy";
		questItems[117][1] = "Mourning's End Part I";
		questItems[118][1] = "Cabin Fever";
		questItems[119][1] = "Troll Romance";
		questItems[120][1] = "Temple of Ikov";
		questItems[121][1] = "Another Slice of H.A.M.";
		questItems[122][1] = "Legend's Quest";
		questItems[123][1] = "Rum Deal";
		questItems[124][1] = "Animal Magnetism";
		questItems[125][1] = "Underground Pass";
		questItems[126][1] = "Contact!";
		questItems[127][1] = "The Great Brain Robbery";
		questItems[128][1] = "Creature of Fenkenstrain";
		questItems[129][1] = "Dwarf Cannon";

	}

	private boolean compareToQuestItems(String currentItem)
	{
		int i;
		int FoundFlag = 0;
		String ArrayItem;
		currentItem = currentItem.toUpperCase();



		for(i=0; i < questItems.length; i++)
		{
			ArrayItem = questItems[i][0].toUpperCase();
			if(ArrayItem.contains(currentItem))
			{
				FoundFlag = 1;
			}
		}


		if (FoundFlag > 0)
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

	private boolean checkAllowedItemsArray(String entity)
	{
		int classID = getClassID(config.playerClass().toString());
		int found = 0;
		entity = entity.toUpperCase();

		for(int counter = 1; counter < validClassItems.length; counter++)
		{
			String currentArrayItem = validClassItems[classID][counter].toUpperCase();
			if(entity.contains(currentArrayItem) || currentArrayItem == "!ALL!")
			{
				found = 1;
			}
			else
			{
				//Do nothing
			}
		}

		if(found > 0)
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
			case "Artificer":
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
		msgStr = "You feel the approval of ";
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
		worldAltars[1][1] = "2415,9680,0";
		worldAltars[1][2] = "Zamorak";

		//Ourania Cave entrance;
		worldAltars[2][0] = "Chaos Altar";
		worldAltars[2][1] = "2455,3231,0";
		worldAltars[2][2] = "Zamorak";

		//Wilderness Chaos Temple;
		worldAltars[3][0] = "Chaos Altar";
		worldAltars[3][1] = "3240,3608,0";
		worldAltars[3][2] = "Zamorak";

		//Black Knight's Fortress;
		worldAltars[4][0] = "Chaos Altar";
		worldAltars[4][1] = "3027,3510,1";
		worldAltars[4][2] = "Zamorak";

		//Chaos Temple near Goblin Village;
		worldAltars[5][0] = "Chaos Altar";
		worldAltars[5][1] = "2933,3513,0";
		worldAltars[5][2] = "Zamorak";

		//Deep wilderness Chaos Temple;
		worldAltars[6][0] = "Chaos Altar";
		worldAltars[6][1] = "2947,3821,0";
		worldAltars[6][2] = "Zamorak";

		//Yanille Agility dungeon;
		worldAltars[7][0] = "Chaos Altar";
		worldAltars[7][1] = "2571,9500,0";
		worldAltars[7][2] = "Zamorak";

		//Tutorial Island;
		worldAltars[8][0] = "Altar";
		worldAltars[8][1] = "3121,3106,0";
		worldAltars[8][2] = "Saradomin";

		//Lumbridge church;
		worldAltars[9][0] = "Altar";
		worldAltars[9][1] = "3243,3207,0";
		worldAltars[9][2] = "Saradomin";

		//Between Rimmington,Port Sarim and Thurgo;
		worldAltars[10][0] = "Altar";
		worldAltars[10][1] = "2995,3177,0";
		worldAltars[10][2] = "Saradomin";

		//Duel Arena lobby;
		worldAltars[11][0] = "Altar";
		worldAltars[11][1] = "3377,3285,0";
		worldAltars[11][2] = "Saradomin";

		//Varrock Palace;
		worldAltars[12][0] = "Altar";
		worldAltars[12][1] = "3208,3495,1";
		worldAltars[12][2] = "Saradomin";

		//Seers' Village;
		worldAltars[13][0] = "Altar";
		worldAltars[13][1] = "2694,3463,0";
		worldAltars[13][2] = "Saradomin";

		//East Ardougne;
		worldAltars[14][0] = "Altar";
		worldAltars[14][1] = "2617,3309,0";
		worldAltars[14][2] = "Saradomin";

		//West Ardougne;
		worldAltars[15][0] = "Altar";
		worldAltars[15][1] = "2530,3286,0";
		worldAltars[15][2] = "Saradomin";

		//Ardougne Monastery;
		worldAltars[16][0] = "Altar";
		worldAltars[16][1] = "2606,3208,0";
		worldAltars[16][2] = "Saradomin";

		//Paterdomus;
		worldAltars[17][0] = "Altar";
		worldAltars[17][1] = "3416,3489,0";
		worldAltars[17][2] = "Saradomin";

		//Entrana;
		worldAltars[18][0] = "Altar";
		worldAltars[18][1] = "2853,3349,0";
		worldAltars[18][2] = "Saradomin";

		//Edgeville Monastery;
		worldAltars[19][0] = "Altar";
		worldAltars[19][1] = "3051,3498,1";
		worldAltars[19][2] = "Saradomin";

		//Well of Voyage;
		worldAltars[20][0] = "Altar";
		worldAltars[20][1] = "2341,9629,0";
		worldAltars[20][2] = "Saradomin";

		//Lletya;
		worldAltars[21][0] = "Altar";
		worldAltars[21][1] = "2356,3172,1";
		worldAltars[21][2] = "Seren";

		//Tower of Voices;
		worldAltars[22][0] = "Altar";
		worldAltars[22][1] = "3263,6091,2";
		worldAltars[22][2] = "Seren";

		//Prifddinas;
		worldAltars[23][0] = "Altar";
		worldAltars[23][1] = "3246,6116,0";
		worldAltars[23][2] = "Seren";

		//Gorlah;
		worldAltars[24][0] = "Altar";
		worldAltars[24][1] = "2284,3427,0";
		worldAltars[24][2] = "Seren";

		//North-eastern Varrock;
		worldAltars[25][0] = "Altar";
		worldAltars[25][1] = "3254,3487,0";
		worldAltars[25][2] = "Saradomin";

		//Kourend Castle;
		worldAltars[26][0] = "Altar";
		worldAltars[26][1] = "1617,3673,2";
		worldAltars[26][2] = "Saradomin";

		//Witchaven;
		worldAltars[27][0] = "Altar";
		worldAltars[27][1] = "2728,3283,0";
		worldAltars[27][2] = "Saradomin";

		//Camelot;
		worldAltars[28][0] = "Altar";
		worldAltars[28][1] = "2750,3496,1";
		worldAltars[28][2] = "Saradomin";

		//Heroes' Guild;
		worldAltars[29][0] = "Altar";
		worldAltars[29][1] = "2889,3511,1";
		worldAltars[29][2] = "Saradomin";

		//Sophanem;
		worldAltars[30][0] = "Altar";
		worldAltars[30][1] = "3281,2774,0";
		worldAltars[30][2] = "Icthlarin";

		//Hosidius' church;
		worldAltars[31][0] = "Altar";
		worldAltars[31][1] = "1733,3572,0";
		worldAltars[31][2] = "Saradomin";

		//Hosidius monk's camp;
		worldAltars[32][0] = "Altar";
		worldAltars[32][1] = "1743,3500,0";
		worldAltars[32][2] = "Saradomin";

		//Arceuus church;
		worldAltars[33][0] = "Altar";
		worldAltars[33][1] = "1689,3794,2";
		worldAltars[33][2] = "None";

		//Lovakengj;
		worldAltars[34][0] = "Altar";
		worldAltars[34][1] = "1547,3808,0";
		worldAltars[34][2] = "Unknown";

		//Molch;
		worldAltars[35][0] = "Altar";
		worldAltars[35][1] = "1282,3677,0";
		worldAltars[35][2] = "Xeric";

		//Xeric's Shrine in Kebos Swamp;
		worldAltars[36][0] = "Altar";
		worldAltars[36][1] = "1310,3619,0";
		worldAltars[36][2] = "Xeric";

		//Myths' Guild;
		worldAltars[37][0] = "Altar";
		worldAltars[37][1] = "2457,2839,2";
		worldAltars[37][2] = "Unknown";

		//Forthos Dungeon;
		worldAltars[38][0] = "Altar";
		worldAltars[38][1] = "1802,9950,0";
		worldAltars[38][2] = "Ranul";

		//Ferox Enclave;
		worldAltars[39][0] = "Altar";
		worldAltars[39][1] = "3126,3636,0";
		worldAltars[39][2] = "Zaros";

		//Citharede Abbey on Desert Plateau;
		worldAltars[40][0] = "Altar";
		worldAltars[40][1] = "3421,3180,0";
		worldAltars[40][2] = "Saradomin";

		//Clan Hall chapel;
		worldAltars[41][0] = "Altar";
		worldAltars[41][1] = "1175,5467,0";
		worldAltars[41][2] = "None";

		//Taverley stone circle;
		worldAltars[42][0] = "Altar of Guthix";
		worldAltars[42][1] = "2925,3483,0";
		worldAltars[42][2] = "Guthix";

		//Nature grotto in Mort Myre Swamp;
		worldAltars[43][0] = "Altar of nature";
		worldAltars[43][1] = "3442,9740,1";
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
		worldAltars[49][1] = "3738,3307,0";
		worldAltars[49][2] = "Zamorak";

		//Forthos dungeon;
		worldAltars[50][0] = "Broken sun altar";
		worldAltars[50][1] = "1795,9951,0";
		worldAltars[50][2] = "Ralos";

		//Woodcutting Guild;
		worldAltars[51][0] = "Shrine";
		worldAltars[51][1] = "1613,3515,0";
		worldAltars[51][2] = "Evil Chicken";

		//Tai Bwo Wannai;
		worldAltars[52][0] = "Tribal Statue";
		worldAltars[52][1] = "2796,3090,0";
		worldAltars[52][2] = "Karamjan gods";

		//Darkmeyer;
		worldAltars[53][0] = "Statue";
		worldAltars[53][1] = "3605,3355,0";
		worldAltars[53][2] = "Unknown";

		//Temple of Marimbo;
		worldAltars[54][0] = "Gorilla Statue";
		worldAltars[54][1] = "2798,2800,1";
		worldAltars[54][2] = "Marimbo";

		//Nardah;
		worldAltars[55][0] = "Elidinis Statuette";
		worldAltars[55][1] = "3427,2930,0";
		worldAltars[55][2] = "Elidinis";

		//Ourania Altar;
		worldAltars[56][0] = "Chaos Altar";
		worldAltars[56][1] = "2455,3231,0";
		worldAltars[56][2] = "Zamorak";

		//Ferox Enclave;
		worldAltars[57][0] = "Altar";
		worldAltars[57][1] = "3177,3626,0";
		worldAltars[57][2] = "Zaros";

		//Shayzien Hut;
		worldAltars[58][0] = "Altar";
		worldAltars[58][1] = "1498,3562,0";
		worldAltars[58][2] = "Saradomin";

		//South Varrock;
		worldAltars[59][0] = "Chaos Altar";
		worldAltars[59][1] = "3259,3381,0";
		worldAltars[59][2] = "Zamorak";
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

			case "Artificer": {
				classID = getClassID(PlayerClass);
				validClassItems[classID][0] = PlayerClass;

				validClassItems[classID][1] = "axe";
				validClassItems[classID][2] = "thrownaxe";
				validClassItems[classID][3] = " saw";
				validClassItems[classID][4] = "hammer";
				validClassItems[classID][5] = "knife";
				validClassItems[classID][6] = "knives";
				validClassItems[classID][7] = " axe";
				validClassItems[classID][8] = "Colossal blade";
				validClassItems[classID][9] = "Glassblowing pipe";
				validClassItems[classID][10] = "Pickaxe";
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
				validClassItems[classID][18] = "harpoon";
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
				validClassItems[classID][9] = "Teasing stick";
				validClassItems[classID][10] = "Machete";
				validClassItems[classID][11] = "Butterfly Net";
				validClassItems[classID][12] = "Harpoon";
				validClassItems[classID][13] = " axe";
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
				validClassItems[classID][16] = "silverlight";
				validClassItems[classID][17] = "darklight";
				validClassItems[classID][18] = "arclight";
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
				validClassItems[classID][9] = "Machete";
				validClassItems[classID][10] = "Pickaxe";
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
				validClassItems[classID][6] = "Butterfly Net";
				validClassItems[classID][7] = " axe";
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
				validClassItems[classID][13] = "silverlight";
				validClassItems[classID][14] = "darklight";
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
				validClassItems[classID][6] = "silverlight";
				validClassItems[classID][7] = "darklight";
				validClassItems[classID][8] = "arclight";
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
			case "Artificer":
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
		   a necromancer may say "Praise be to Zamorak, may his chaos reign eternal!" whereas a barbarian may say "For Bandos, and glory!"

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

			case "Artificer":{
				int ClassID = getClassID(ClassName);
				permClassItems[ClassID][0] = ClassName;

				permClassItems[ClassID][1] = "Smiths tunic";
				permClassItems[ClassID][2] = "Smiths trousers";
				permClassItems[ClassID][3] = "Smiths gloves";
				permClassItems[ClassID][4] = "Smiths boots";
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

		ItemComposition currentItem = itemManager.getItemComposition(event.getItemId());
		String itemName = currentItem.getName();

		//Used for debugging:
		//String itemID = String.valueOf(currentItem.getId());
		//msgStr = "You clicked: " + itemName + " (" + itemID + ")";
		//sendChatMessage(msgStr);
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

			if(checkAllowedItemsArray(itemName) || config.enableNonClassItems())
			{
				//Do nothing, item is allowed for character's class

			}
			else
			{
				event.consume();
				msgStr = "As a " + config.playerClass().toString() + " you cannot wield the " + itemName + ".";
				sendChatMessage(msgStr);
			}
			return;
		}

		//Prayer Activation Observer
		if ((entryMatches(event,"Activate")))
		{
			//Get prayer name
			String prayerName = event.getMenuTarget();
			msgStr = prayerName + "clicked";
			sendChatMessage(msgStr);

			if(prayerAllowed || config.forceAllowPrayer())
			{
				//Do nothing, character's class is permitted to use prayer

			}
			else
			{
				if(prayerName.contains("Protect from") && config.allowProtectPrayers())
				{
					//Allow protection prayers is enabled. Permit use of these prayers.
				}
				else
				{
					//Prayer has been disabled for the player
					event.consume();
					msgStr = "Why would a " + config.playerClass().toString() + " pray to the Gods? I'll pass.";
					sendChatMessage(msgStr);
				}
			}
		}

		//Praying at Altars Observer
		if ((entryMatches(event,"Pray-at")) || (entryMatches(event,"Pray")))
		{
			msgStr = "";

			//Get Player's co-ordinates
			WorldPoint currentCoords = client.getLocalPlayer().getWorldLocation();
			int currentX = currentCoords.getX();
			int currentY = currentCoords.getY();
			int altarFoundFlag = 0;
			int altarMatch = 0;

			//Create proximity threshold
			int MinXProx = currentX - 2;
			int MaxXProx = currentX + 2;
			int MinYProx = currentY - 2;
			int MaxYProx = currentY + 2;

			//Check Altars
			for(int i = 0; i < worldAltars.length; i++)
			{
				if (altarFoundFlag == 0)
				{
					String[] altarCoords = worldAltars[i][1].split(",");

					int altarX = Integer.parseInt(altarCoords[0]);
					int altarY = Integer.parseInt(altarCoords[1]);

					if ((altarX <= MaxXProx) && (altarX >= MinXProx) && (altarY <= MaxYProx) && (altarY >= MinYProx))
					{
						altarFoundFlag = 1;
						altarMatch = i;
					}
					else
					{
						//Do nothing, they are not at this altar.
					}
				}
			}
			if(altarMatch > 0)
			{
				String altarDiety = worldAltars[altarMatch][2];
				int GodFound = 0;

				for (int x = 0; x < playerDeities.length; x++)
				{
					if(playerDeities[x].contains(altarDiety))
					{
						GodFound = 1;
					}
					else
					{
						//Do nothing, this altar does not belong to this diety.
					}
				}

				if (GodFound > 0)
				{
					//God found: player can pray at this altar
					msgStr = "You feel the embrace of " + altarDiety + " as you approach the altar.";
					sendChatMessage(msgStr);

				}
				else
				{
					//No god was found: player cannot pray at this altar.
					event.consume();
					msgStr = "You feel nothing. You do not have the blessing of " + altarDiety;
					sendChatMessage(msgStr);

				}
			}
		}
	}
}

