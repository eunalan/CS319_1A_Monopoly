package entities;

import java.util.ArrayList;

public class EasyStrategy implements PlayStrategy {
    @Override
    public boolean shouldBuy(Property property, int money,
                             ArrayList<Property> properties) {
        return false;
    }

    @Override
    public int getBid(Property property, int highestBid, int money, int poorLimit, ArrayList<Property> properties) {
        return 0;
    }

    @Override
    public int doMortgage(ArrayList<Property> properties) {

        return 0;
    }

    @Override
    public void doRedeem(ArrayList<Property> properties) {

    }

    @Override
    public void doTrade() {

    }

    @Override
    public int getMortgageLimit() {
        return 0;
    }

    @Override
    public int getRedeemLimit() {
        return 0;
    }

    @Override
    public int getPoorLimit() {
        return 0;
    }
}
