package board;

import card.*;
import entities.*;
import event.AdvanceEvent;
import event.CardEvent;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

@Getter
@Setter
public class Board {
    private Space[] spaces;
    private String[] propertyGroupColors;
    private ArrayList<Card> chanceCards;
    private ArrayList<Card> communityChestCards;
    private Thief thief;

    public Board(File map) {
        spaces = new Space[40];
        // Maybe also read the cards from a file and instantiate them here
        chanceCards = null;
        communityChestCards = null;
        propertyGroupColors = null;
        thief = null;
        try {
            Scanner scan = new Scanner(map);
            // Using \Z as the delimiter matches to the end of file except a newline at end
            String json = scan.useDelimiter("\\Z").next();

            // Create a json object containing the map and then access the spaces
            JSONObject jsonMap = new JSONObject(json);
            int propertyGroupCount = jsonMap.getInt("propertyGroupCount");
            System.out.println("Prop group count: " + propertyGroupCount);


            int[] numberOfPropertiesInGroups = new int[propertyGroupCount];
            JSONArray numberOfPropertiesInGroupsJson = jsonMap.getJSONArray("numberOfPropertiesInGroups");
            for (int i = 0; i < numberOfPropertiesInGroupsJson.length(); i++) {
                numberOfPropertiesInGroups[i] = numberOfPropertiesInGroupsJson.getInt(i);
            }
            Property.setNumberOfPropertiesInGroups(numberOfPropertiesInGroups);

            // transport and utility properties don't have colors so we subtract 2
            propertyGroupColors = new String[propertyGroupCount - 2];
            JSONArray colorsJson = jsonMap.getJSONArray("propertyGroupColors");
            for (int i = 0; i < colorsJson.length(); i++) {
                propertyGroupColors[i] = colorsJson.getString(i);
            }


            JSONArray mapSpaces = jsonMap.getJSONArray("spaces");

            for (int i = 0; i < mapSpaces.length(); i++) {
                JSONObject currentSpace =  mapSpaces.getJSONObject(i);
                switch (currentSpace.getString("type")) {
                    case "PropertySpace":
                        Property p = null;
                        JSONObject titleDeedCard = currentSpace.getJSONObject("titleDeedCard");
                        switch (currentSpace.getString("propertyType")) {
                            case "LAND":
                                JSONArray rentsJson = titleDeedCard.getJSONArray("rents");
                                int[] rents = new int[6];
                                for (int j = 0; j < rentsJson.length(); j++) {
                                    rents[j] = rentsJson.getInt(j);
                                }
                                p = new LandProperty(currentSpace.getString("name"), currentSpace.getInt("value"), titleDeedCard.getInt("mortgageValue"),
                                        rents, currentSpace.getInt("propertyGroup"), titleDeedCard.getInt("houseCost"), titleDeedCard.getInt("hotelCost"));
                                break;
                            case "UTILITY":
                                JSONArray multipliersJson = titleDeedCard.getJSONArray("multipliers");
                                int[] multipliers = new int[2];
                                for (int j = 0; j < multipliersJson.length(); j++) {
                                    multipliers[j] = multipliersJson.getInt(j);
                                }
                                // PROPERTY GROUP EKLENCEK
                                p = new UtilityProperty(currentSpace.getString("name"), currentSpace.getInt("value"), titleDeedCard.getInt("mortgageValue"), multipliers, currentSpace.getInt("propertyGroup"));

                                break;
                            case "TRANSPORT":
                                JSONArray transportRentsJson = titleDeedCard.getJSONArray("rents");
                                int[] transportRents = new int[4];
                                for (int j = 0; j < transportRentsJson.length(); j++) {
                                    transportRents[j] = transportRentsJson.getInt(j);
                                }
                                //PROPERTY GROUP
                                p = new TransportProperty(currentSpace.getString("name"), currentSpace.getInt("value"), titleDeedCard.getInt("mortgageValue"), transportRents, currentSpace.getInt("propertyGroup"));
                                break;
                        }
                        spaces[i] = new PropertySpace(currentSpace.getString("name"), i, currentSpace.getString("propertyType"), p);
                        break;
                    case "CardSpace":
                        spaces[i] = new CardSpace(currentSpace.getString("cardType"), i);
                        break;
                    case "TaxSpace":
                        spaces[i] = new TaxSpace(currentSpace.getString("taxType"), i);
                        break;
                    case "GoSpace":
                        spaces[i] = new GoSpace(i);
                        break;
                    case "JailSpace":
                        spaces[i] = new JailSpace(i);
                        break;
                    case "WheelOfFortuneSpace":
                        spaces[i] = new WheelOfFortuneSpace(i);
                        break;
                    case "GoToJailSpace":
                        spaces[i] = new GoToJailSpace(i);
                        break;
                }
            }

            chanceCards = new ArrayList<>();
            JSONObject cardsJSON = jsonMap.getJSONObject("cards");
            JSONArray chanceCardsJSON = cardsJSON.getJSONArray("chanceCards");
            for (int i = 0; i < chanceCardsJSON.length(); i++) {
                JSONObject card = chanceCardsJSON.getJSONObject(i);
                JSONObject cardEvent = card.getJSONObject("cardEvent");
                CardEvent e = null;
                switch (cardEvent.getString("type")) {
                    case "ADVANCE":
                        String targetSpace = cardEvent.getString("targetSpace");
                        for (Space s : spaces) {
                            if (targetSpace.equals(s.getName())) {
                                e = new AdvanceEvent(s, 1 == cardEvent.getInt("canCollectSalary"));
                                chanceCards.add(new Card(card.getString("cardText"), e));
                            }
                        }
                }
            }
            communityChestCards = new ArrayList<>();
            JSONArray communityChestCardsJSON = cardsJSON.getJSONArray("communityChestCards");
            for (int i = 0; i < communityChestCardsJSON.length(); i++) {
                JSONObject card = communityChestCardsJSON.getJSONObject(i);
                JSONObject cardEvent = card.getJSONObject("cardEvent");
                CardEvent e = null;
                switch (cardEvent.getString("type")) {
                    case "ADVANCE":
                        String targetSpace = cardEvent.getString("targetSpace");
                        for (Space s : spaces) {
                            if (targetSpace.equals(s.getName())) {
                                e = new AdvanceEvent(s, 1 == cardEvent.getInt("canCollectSalary"));
                                communityChestCards.add(new Card(card.getString("cardText"), e));
                            }
                        }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Space getSpace(int index) {return spaces[index];}

    public void deployThief(Player target) {
        thief = new Thief(target);
    }

    public Card drawCard(CardSpace.CardType type) {
        Card drawn;
        if(type == CardSpace.CardType.CHANCE) {
            drawn = chanceCards.get(0);
            chanceCards.remove(0);
            chanceCards.add(drawn);
        } else {
            drawn = communityChestCards.get(0);
            communityChestCards.remove(0);
            communityChestCards.add(drawn);
        }
        return drawn;
    }
}
