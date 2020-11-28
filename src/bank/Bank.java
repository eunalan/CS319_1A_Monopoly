package bank;

import entities.Player;
import entities.Property;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@Getter
@Setter
public class Bank {
    private ArrayList<Property> unownedProperties;

    private Auction onGoingAuction;
    private Trade onGoingTrade;

    public Bank() {
        unownedProperties = new ArrayList<>();

    }

    public void payPlayer(@NotNull Player player, int amount) {
        player.setMoney(player.getMoney() + amount);
    }

    public void startTrade( Player offerer, Player target) {
        onGoingTrade = new Trade(offerer, target);
        //wait for a signal from UI Trade
        if(onGoingTrade.closeTrade())
            System.out.println("Successful trade.");
        else
            System.out.println("Trade canceled or failed.");
        onGoingTrade = null;
    }

    public void startAuction( Property auctionedProperty ) {
        onGoingAuction = new Auction(auctionedProperty);
        //wait for a signal from UI Trade
        if(onGoingAuction.closeAuction())
            System.out.println("Successful auction.");
        else
            System.out.println("Auction canceled or failed.");

        onGoingAuction = null;
    }

    public boolean removeFromUnownedProperties(Property property) {
        if(! unownedProperties.contains(property))
            return false;
        unownedProperties.remove( property);
        return true;
    }
}
