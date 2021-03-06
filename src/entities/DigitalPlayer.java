package entities;

import bank.Trade;
import board.PropertySpace;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class DigitalPlayer extends Player{

    private PlayStrategy strategy;
    private int mortgageLimit; //lower limit
    private int redeemLimit; //upper limit
    private int poorLimit; //upper limit to be count as poor

    //properties for trade
    private int tradeTurn;
    private Player tradePlayer;
    private int tradeType;

    /**
     * Initializes the digital player with the given name and play strategy
     * @param name
     * @param strategy
     */
    public DigitalPlayer(String name, PlayStrategy strategy) {
        super(name);
        this.strategy = strategy;
        mortgageLimit = strategy.getMortgageLimit();
        redeemLimit = strategy.getRedeemLimit();
        poorLimit = strategy.getPoorLimit();
        tradeTurn = 0;
        tradePlayer = null;
        tradeType = -1;

        System.out.println(name + " has " + strategy);
    }

    /**
     * Checks mortgage and redeem.
     * Decides to do turn actions
     * @param players
     * @return null
     */
    public Player decideOnTradeAction(Player[] players ){
        if (getProperties().size() == 0)
            return null;
        tradeTurn++;
        if(tradeTurn % 3 != 0)
            return null;
        tradeType = decideOnTrade(players);
        if( tradeType > 0) {
            return tradePlayer;
        }
        return null;
    }


    /**
     * Decides on redeem action.
     * @return true if done redeem, false if it did not
     */
    public boolean decideOnRedeemAction() {
        boolean didRedeem = false;
        //redeem action
        while( hasMortgagedProperty() && getMoney() > redeemLimit) {
            Property p = strategy.doRedeem(this);
            p.liftMortgage();
            setMoney(getMoney() + (int) (p.getValue() * 1.1));
            didRedeem = true;
        }
        return didRedeem;
    }

    /**
     * Decides on mortgage action.
     * @return true if done mortgage, false if it did not
     */
    public boolean decideOnMortgageAction() {
        boolean didMortgage = false;
        //mortgage action
        while(getMoney() < mortgageLimit) {
            Property p = strategy.doMortgage(this);
            if(p == null) break;

            p.mortgage();
            setMoney(getMoney() + p.getMortgageValue());
            didMortgage = true;
        }
        return didMortgage;
    }

    /**
     * Decides on build action.
     * @return true if done build, false if it did not
     */
    public boolean decideOnBuildAction() {
        if(! ownsAllPropertiesFromSameGroup(((PropertySpace) getCurrentSpace())
                .getAssociatedProperty())) {
            return false;
        }
        ArrayList<Property> propertyArrayList = getAllPropertiesFromSameGroup(((PropertySpace) getCurrentSpace())
                .getAssociatedProperty() );
        int totalCost = 0;

        for(Property p: propertyArrayList) {
            LandProperty landProperty = (LandProperty) p;
            if(landProperty.getNumOfHouses() == 5 ) //has hotel
                continue;
            if(landProperty.getNumOfHouses() == 4) //4 houses
            {
                if(getMoney() > 3* landProperty.getHotelCost()) {
                    landProperty.buildHouse();
                    totalCost += landProperty.getHotelCost();
                }
            } else { //less than 4 houses
                if(getMoney() > 3* landProperty.getHouseCost()) {
                    landProperty.buildHouse();
                    totalCost += landProperty.getHouseCost();
                }
            }
        }

        if(totalCost > 0) {
            setMoney(getMoney() - totalCost);
            return true;
        }
        return false;
    }

    /**
     * Does the build action
     */
    public void doBuild(){
        ArrayList<Property> properties = getAllPropertiesFromSameGroup(((PropertySpace) getCurrentSpace()).getAssociatedProperty());
        for( Property p : properties)
            ((LandProperty) p).buildHouse();
    }


    /**
     * Decides on buy action.
     * @return true if done buy, false if it did not
     */
    public boolean decideOnBuy( Property property ) {
        if (strategy.shouldBuy(property, getMoney(), getAllPropertiesFromSameGroup(property))) {
            this.addProperty(property);
            setMoney(getMoney() - property.getValue());
            property.setOwner(this);
            return true;
        }
        return false;
    }

    /**
     * Prepares a trade proposal.
     * @return trade information array
     */
    public int[] getTradeProposal() {
        if(tradePlayer == null || tradePlayer.isBankrupt() || tradeType < 1)
            return null;
        int[] proposal = strategy.doTrade( tradeType, tradePlayer, this);
        //0-2 offered, 3-5 requested
        //proposal[property index, money, card, property index, money, card]
        if(proposal[0] > getProperties().size() || proposal[0] < 0
                || proposal[3] > tradePlayer.getProperties().size() || proposal[3] < 0
                || proposal[1] > getMoney() || proposal[4] > tradePlayer.getMoney())
            return null;
        return proposal;
    }

    /**
     * Prepares a bid on auction.
     * @param property
     * @param highestBid
     * @return bid
     */
    public int bidOnAuction(Property property, int highestBid){
        return strategy.getBid(property, highestBid, getMoney(), poorLimit, this);
    }

    //should trade be done this turn
    //include strategy maybe?
    // 1 -> needs GOOJF card from other players
    // 2 -> needs money, has lots of property
    // 3 -> wants property
    // -1 -> do not need to trade

    /**
     * Decides on trade.
     * @param players
     * @return decision
     */
    private int decideOnTrade(Player[] players){
        if(isJailed()) {
            for(Player p: players)
                if( p.getGetOutOfJailFreeCount() > 0 ) {
                    tradePlayer = p;
                    return 1;
                }
        }
        if(getMoney() < poorLimit && getProperties().size() > 7) {
            //find the richest player
            int index = 0, i = 0, max = 0;
            for(Player p: players) {
                if(p == this || p.isBankrupt())
                    continue;
                if(p.getMoney() > max)
                {
                    max = p.getMoney();
                    index = i;
                }
                i++;
            }
            tradePlayer = players[index];
            return 2;
        }
        ArrayList<Integer> propertyGroups = new ArrayList<>();
        for(Property prop: getProperties()){
            if(numberOfPropertiesFromSameGroup(prop) > 1)
                propertyGroups.add(prop.getPropertyGroup());
        }
        if( ! propertyGroups.isEmpty())
            for(Player p: players) {
                if(p == this || p.isBankrupt())
                    continue;
                for(Property property: p.getProperties())
                    if(propertyGroups.contains(property.getPropertyGroup())){
                        tradePlayer = p;
                        return 3;
                    }
            }
        return -1;
    }

    /**
     * Prepares a trade answer.
     * @param trade
     * @return true or false
     */
    public boolean getTradeAnswer(Trade trade) {
        return strategy.getTradeAnswer(trade, this, 1.0);
    }

    /**
     * Returns true if there is mortgaged property, false otherwise
     * @return true or false
     */
    private boolean hasMortgagedProperty() {
        for (Property p: getProperties() ) {
            if(p.isMortgaged())
                return true;
        }
        return false;
    }

    /**
     * resets the digital player
     */
    @Override
    public void reset() {
        super.reset();
        tradeTurn = 0;
        tradePlayer = null;
        tradeType = 0;
    }
}
