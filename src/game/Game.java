package game;

import FileManagement.FileManager;
import bank.Auction;
import bank.Observer;
import bank.Trade;
import board.*;
import card.Card;
import entities.*;
import event.*;
import frontend.GameScreenController;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;

public class Game extends Observer {

    private final FileManager fileManager = FileManager.getInstance();
    @Getter private static final int LAP = 4;
    @Getter private final Board board;
    @Getter private final int lapLimit; //given -1 if game mod is survival
    @Getter private final int playerCount;

    @Getter @Setter private Player[] players;
    @Getter @Setter private Player currentPlayer;

    @Getter private int lapCount;
    @Setter private boolean loadedGame;
    private boolean restarted;
    private final GameScreenController controller;
    @Getter private final String gameName;

    public Game(String gameName, File map, int playerCount, String[] playerNames, int turnLimit, GameScreenController controller) {
        this.playerCount = playerCount;
        this.lapLimit = turnLimit;
        board = new Board(map);
        lapCount = 0;
        loadedGame = false;
        restarted = false;
        this.gameName = gameName;
        observable = null;
        this.controller = controller;

        players = new Player[LAP];
        for(int i = 0; i < playerCount; i++) {
            players[i] = new Player(playerNames[i]);
        }
        if(playerCount < LAP)
            createDigitalPlayers(playerCount);
    }

    //for loading the game, to be called by the file manager
    public Game(String gameName, Board board, Player[] players, int lapLimit, int playerCount, String currentPlayer, int lapCount,
                GameScreenController controller){
        this.board = board;
        this.players = players;
        this.lapLimit = lapLimit;
        this.playerCount = playerCount;
        this.gameName = gameName;
        for(Player p: players)
            if(p.getPlayerName().equals(currentPlayer)){
                this.currentPlayer = p;
                break;
            }

        this.lapCount = lapCount;
        System.out.println("GAME LOADED WITH lap count " +lapCount);
        this.controller = controller;
        loadedGame = true;
        restarted = false;
    }

    //copy constructor
    public Game(Game copyGame) {
        this.gameName = copyGame.gameName;
        this.controller = copyGame.controller;
        this.playerCount = copyGame.playerCount;
        this.players = copyGame.players;
        this.lapCount = copyGame.lapCount;
        this.lapLimit = copyGame.lapLimit;
        this.board = copyGame.board;
        this.currentPlayer = copyGame.currentPlayer;
        this.loadedGame = copyGame.loadedGame;
        this.restarted = copyGame.restarted;
    }

    public void gameLoop(int playerCount) {
        //player count is the offset for loaded games
        int[] dice;
        int doublesCount = 0;
        boolean digitalPlayer = false;
        Thief thief = null;


        System.out.println("LAP COUNT: " + lapCount);
        while( ! isGameEnd() ) {
            if(lapCount == 0)
                initializingLap();
            if(restarted)
                restarted = false;

            if(! loadedGame) {
                lapCount++;
            }
            controller.labelUpdate(lapCount);
            for(int i = 0; i < LAP &&  !isGameEnd() && !restarted; i++) {
                if(loadedGame){
                    i += playerCount;
                    loadedGame = false;
                }
                else {
                    currentPlayer = players[i];
                    if( currentPlayer.isBankrupt() ) //lost player
                        continue;

                    digitalPlayer = currentPlayer instanceof DigitalPlayer;
                    dice = controller.rollDice(currentPlayer.getPlayerName(), digitalPlayer);

                    currentPlayer.setCurrentDiceSum(dice[0] + dice[1]);
                    //calculating next player
                    if (dice[0] == dice[1]) {
                        doublesCount++;
                    } else
                        doublesCount = 0;

                    //checking jail conditions
                    if (!currentPlayer.isJailed() && doublesCount == 3) {
                        doublesCount = 0;
                        sendToJail(currentPlayer);
                        controller.showMessage("sent to jail!", currentPlayer);
                        continue;
                    } else if (currentPlayer.isJailed() && doublesCount > 0) {
                        currentPlayer.setJailed(false);
                        currentPlayer.setJailedLapCount( 0);
                        controller.showMessage("released from jail!", currentPlayer);
                    } else if (currentPlayer.isJailed() &&
                            currentPlayer.getJailedLapCount() >= currentPlayer.getToken().getJailTime()) {
                        currentPlayer.setJailed(false);
                        currentPlayer.setJailedLapCount( 0);
                        currentPlayer.setMoney(currentPlayer.getMoney() - 5);
                        controller.showMessage("released from jail and have lost M5 money!", currentPlayer);
                    } else if (currentPlayer.isJailed()) {
                        //get out of jail using GOOJF card
                        if(currentPlayer.getGetOutOfJailFreeCount() > 0) {
                            if(!digitalPlayer && controller.twoChoiceDialog("Do you wish to use your Get Out of Jail Free card?", "Yes", "No"))
                            {
                                currentPlayer.setJailed(false);
                                currentPlayer.setJailedLapCount( 0);
                                currentPlayer.setGetOutOfJailFreeCount(currentPlayer.getGetOutOfJailFreeCount() - 1);
                                controller.showMessage("released from jail!", currentPlayer);
                            } else if (digitalPlayer) {
                                currentPlayer.setJailed(false);
                                currentPlayer.setGetOutOfJailFreeCount(currentPlayer.getGetOutOfJailFreeCount() - 1);
                                controller.showMessage("released from jail!", currentPlayer);
                            } else {
                                currentPlayer.setJailedLapCount( currentPlayer.getJailedLapCount() + 1);
                                continue;
                            }
                        } else {
                            currentPlayer.setJailedLapCount( currentPlayer.getJailedLapCount() + 1);
                            continue;
                        }
                    }

                    //move on board
                    int oldIndex = currentPlayer.getCurrentSpace().getIndex();
                    int boardIndex = currentPlayer.getCurrentSpace().getIndex() + dice[0] + dice[1];
                    if (boardIndex >= 40) { //passed Go space or at Go space
                        currentPlayer.setMoney(currentPlayer.getMoney() + 200
                                + currentPlayer.getToken().getSalaryChange());
                        boardIndex = boardIndex % 40;
                        controller.drawPlayerBoxes(players);
                        controller.showMessage("Salary is paid!",null);
                    }
                    Space space = board.getSpace(boardIndex);
                    currentPlayer.setCurrentSpace(space);
                    space.setLatestPlayer(currentPlayer);

                    controller.drawToken(i, oldIndex, boardIndex);

                    if (space instanceof CardSpace) {
                        System.out.println("Draw a card!");
                        drawCard(currentPlayer, (CardSpace) space);
                        if(currentPlayer.isJailed()) {
                            doublesCount = 0;
                            currentPlayer.setJailedLapCount( currentPlayer.getJailedLapCount() + 1);
                            continue;
                        }
                    } else if (space instanceof GoToJailSpace) {
                        doublesCount = 0;
                        sendToJail(currentPlayer);
                        controller.drawToken(i, boardIndex, 10);
                        controller.showMessage("sent to jail!", currentPlayer);
                        continue;
                    } else if (space instanceof JailSpace) {
                        controller.showMessage("visiting jail.", currentPlayer);
                    } else if (space instanceof TaxSpace) {
                        int payment = (int) (((TaxSpace) space).getTax()
                                * currentPlayer.getToken().getTaxMultiplier());
                        currentPlayer.setMoney(currentPlayer.getMoney() - payment);
                        controller.drawPlayerBoxes(players);
                        if(digitalPlayer)
                            controller.showMessage(currentPlayer.getPlayerName() + " paid M" + payment + " for tax.", null);
                        else
                            controller.showMessage("You paid M" + payment + " for tax.", null);

                    } else if (space instanceof WheelOfFortuneSpace) {
                        try{
                            controller.spinWheelOfFortune(((WheelOfFortuneSpace) space).spinWheel(), digitalPlayer);
                            controller.drawPlayerBoxes(players);
                        } catch(Exception e){
                            System.err.println(e.getMessage());
                        }
                    } else if (space instanceof PropertySpace) {
                        cameToProperty((PropertySpace) space);
                    }

                    //digital player turn actions
                    if (digitalPlayer && !(currentPlayer.isBankrupt())) {
                        //will it do mortgage?
                        if(((DigitalPlayer) currentPlayer).decideOnMortgageAction())
                            System.out.println("player did mortgage");
                        //will it do redeem?
                        if(((DigitalPlayer) currentPlayer).decideOnRedeemAction())
                            System.out.println("player did redeem");
                        //will it trade?
                        Player tradePlayer = ((DigitalPlayer) currentPlayer).decideOnTradeAction(players);
                        if (tradePlayer != null) {
                            int[] tradeProposal = ((DigitalPlayer) currentPlayer).getTradeProposal();
                            if (tradeProposal != null ) {
                                System.out.println("START TRADE");
                                //sent trade to controller?
                                Trade digitalPlayerTrade = new Trade(currentPlayer, tradePlayer);
                                ArrayList<Property> offeredProperty = new ArrayList<>();
                                offeredProperty.add(currentPlayer.getProperties().get(tradeProposal[0]));
                                digitalPlayerTrade.offer(offeredProperty, tradeProposal[1], tradeProposal[2]);

                                ArrayList<Property> requestedProperty = new ArrayList<>();
                                requestedProperty.add(tradePlayer.getProperties().get(tradeProposal[3]));
                                digitalPlayerTrade.want(requestedProperty, tradeProposal[4], tradeProposal[5]);

                                controller.tradeProposalDialog(digitalPlayerTrade, tradePlayer instanceof DigitalPlayer);
                            }
                        }

                    }

                    if (dice[0] == dice[1]) {
                        i--;
                    }
                }
                if(currentPlayer.getMoney() < 0)
                {
                    int count = -3;
                    for(Player p : players)
                        if(!p.getPlayerName().equals(currentPlayer.getPlayerName()) &&  p.isBankrupt())
                            count++;
                    currentPlayer.lost(count);
                    controller.showMessage("bankrupt!!", currentPlayer);
                    controller.drawPlayerBoxes(players); //update UI
                }
                controller.finishTurn(digitalPlayer);
            }

            //THIEF TURN
            if(!restarted && board.getThief() != null) {
                thief = board.getThief();
                if(thief.getCurrentSpace() == null) {
                    //controller.drawThief();
                    thief.setCurrentSpace(board.getSpace(10));
                }
                int thiefDice = thief.rollDice();
                if(thiefDice == -1){
                    controller.drawToken(4, thief.getCurrentSpace().getIndex(), -1);
                    controller.showMessage("Thief could not catch its target", null);
                    controller.drawPlayerBoxes(players);
                    board.setThief(null);
                    thief = null;
                }
                else{
                    int move = thiefDice + thief.getCurrentSpace().getIndex() ;
                    int targetSpace = thief.getTarget().getCurrentSpace().getIndex();
                    if((targetSpace > thief.getCurrentSpace().getIndex() && targetSpace <= move)
                            || (move > 40 && move % 40 > targetSpace ) ){
                        move = thief.getTarget().getCurrentSpace().getIndex();
                    }

                    move = move % 40;
                    controller.drawToken(4, thief.getCurrentSpace().getIndex(), move);
                    if(thief.move(board.getSpace(move))) {
                        thief.steal();
                        controller.drawToken(4, thief.getCurrentSpace().getIndex(), -1);
                        controller.showMessage("Thief stole " + Thief.STEAL_AMOUNT + " from " + thief.getTarget().getPlayerName(), null);
                        controller.drawPlayerBoxes(players);
                        board.setThief(null);
                        thief = null;
                    }
                }
            }
            //auto-save game every lap
            try {
                saveGame();
            } catch (Exception e) {
                fileManager.log(e);
                System.out.println(e.getMessage());
            }
        }
        controller.showScoreboard();
        System.out.println("Game Over!");
    }

    private void cameToProperty(PropertySpace space) {
        if(currentPlayer == space.getAssociatedProperty().getOwner()) { //own property
            controller.showMessage("Current player is on their own property.", null);
            //can build on if they choose so
            if (currentPlayer instanceof DigitalPlayer && space.getAssociatedProperty() instanceof LandProperty
                    && ((DigitalPlayer) currentPlayer).decideOnBuildAction()) {
                System.out.println("COMPUTER DOES BUILD");
                controller.showMessage(currentPlayer.getPlayerName() + " build on top of their property. ", null);
                ((DigitalPlayer) currentPlayer).doBuild();
            }
        } else if (space.getAssociatedProperty().getOwner() == null ) { //owned by bank
            //buy or auction
            if(currentPlayer instanceof DigitalPlayer)
                if( ((DigitalPlayer) currentPlayer).decideOnBuy(space.getAssociatedProperty()) ) {
                    //controller let others know of the buying action
                    System.out.println("Computer bought the property.");
                    controller.showMessage(currentPlayer.getPlayerName() + " bought the property: "
                            + space.getAssociatedProperty().getPropertyName(), null);
                }
                else {
                    // START AUCTION FOR PROPERTY
                    controller.showMessage(currentPlayer.getPlayerName() + " started an auction for the property: "
                            + space.getAssociatedProperty().getPropertyName(), null);
                    observable = new Auction(space.getAssociatedProperty());
                    observable.attach(this);
                    controller.startAuction();
                    }
            else if (controller.buyProperty(space)) {
                currentPlayer.addProperty(space.getAssociatedProperty());
                currentPlayer.setMoney(currentPlayer.getMoney() - (int) (space.getAssociatedProperty().getValue()
                        * currentPlayer.getToken().getPropertyCostMultiplier() ));
                space.getAssociatedProperty().setOwner(currentPlayer);
                controller.drawPlayerBoxes(players);
            } else {
                //bank.startAuction(space.getAssociatedProperty());
                // START AUCTION FOR PROPERTY
                observable = new Auction(space.getAssociatedProperty());
                observable.attach(this);
                controller.startAuction();
            }
            System.out.println(space.getName() + " belongs to " + space.getAssociatedProperty().getOwner());
        } else { //owned by another player
            //pay rent
            //int rentAmount = currentPlayer.payRent(((PropertySpace) space).getOwner(), dice);
            int rentAmount = space.calculateRent(currentPlayer);
            currentPlayer.payPlayer(space.getAssociatedProperty().getOwner(), rentAmount);
            controller.drawPlayerBoxes(players);
            if(currentPlayer instanceof DigitalPlayer)
                controller.showMessage(currentPlayer.getPlayerName() + " paid " + rentAmount + "M rent to "
                        +  space.getAssociatedProperty().getOwner().getPlayerName() + ".", null);
            else
                controller.showMessage("You paid " + rentAmount + "M rent to "
                    +  space.getAssociatedProperty().getOwner().getPlayerName() + ".", null);
        }
    }

    private void sendToJail(Player player) {
        player.setJailed(true);
        player.setCurrentSpace(board.getSpace(10));
    }

    // Could ask player in game loop controller.postponeCard() and draw card
    private void drawCard(Player player, CardSpace space) {
        Card card = board.drawCard(space.getType());

        if(player instanceof DigitalPlayer) {
            CardEvent ce = card.getCardEvent();
            if(!(ce instanceof ThiefEvent))
                controller.openCard(player.getPlayerName() + " opened a card!\n" + card.getCardText());
            int oldIndex = currentPlayer.getCurrentSpace().getIndex();
            ce.handleEvent(currentPlayer, players, board);
            if(ce instanceof ThiefEvent){
                controller.openCard(player.getPlayerName() + " opened a card!\n" + card.getCardText() + board.getThief().getTarget().getPlayerName());
                controller.drawToken(4, -1, 10);
            }
            int i = 0;
            for (; i < LAP; i++) {
                if (players[i] == currentPlayer) {
                    break;
                }
            }
            controller.drawToken(i, oldIndex, currentPlayer.getCurrentSpace().getIndex());
            controller.drawPlayerBoxes(players);
        }
        else if(  controller.postponeCard()) {
            ArrayList<Card> cards = player.getPostponedCards();
            cards.add(card);
            player.setPostponedCards( cards );
        } else {
            openCard(card);
            controller.drawPlayerBoxes(players);
        }
    }

    //need to be implemented in GUI
    private void openPostponedCard(Card card){
        if(currentPlayer.getPostponedCards().contains(card)) {
            openCard(card);
            ArrayList<Card> postponedCards = currentPlayer.getPostponedCards();
            postponedCards.remove(card);
            currentPlayer.setPostponedCards(postponedCards);
        }
    }

    private void openCard(Card card){
        System.out.println(card.getCardText());
        CardEvent ce = card.getCardEvent();
        if(!(ce instanceof ThiefEvent))
            controller.showMessage(card.getCardText(), null);
        int oldIndex = currentPlayer.getCurrentSpace().getIndex();
        ce.handleEvent(currentPlayer, players, board);
        if(ce instanceof ThiefEvent){
            controller.drawToken(4, -1, 10);
            controller.showMessage(card.getCardText() + board.getThief().getTarget().getPlayerName(), null);
        }
        int i = 0;
        for (; i < 4; i++) {
            if (players[i] == currentPlayer) {
                break;
            }
        }
        controller.drawToken(i, oldIndex, currentPlayer.getCurrentSpace().getIndex());
    }

    private void initializingLap() {

        for (int i = 0; i < LAP; i++) {
            players[i].setCurrentSpace(board.getSpace(0));
        }
        int[] dice;
        int[] diceSums = new int[LAP];
        boolean digital;
        for(int i = 0; i < LAP; i++) {
            digital = ( players[i] instanceof DigitalPlayer);
            System.out.println(players[i] + " is computer: " + digital);
            dice = controller.rollDice(players[i].getPlayerName(), digital);
            diceSums[i] = dice[0] + dice[1];
        }
        //calculate player turn order
        Player[] order = new Player[LAP];
        int player = 0;
        int max = 0;
        for(int i = 0; i < LAP; i++) {
            max = 0;
            for(int j = 0; j < LAP; j++) {
                if(max < diceSums[j]) {
                    max = diceSums[j];
                    player = j;
                }
            }
            order[i] = players[player];
            diceSums[player] = 0;
        }
        players = order;

        //token choose
        boolean restart = restarted;
        for(int i = 0; i < LAP; i++) {
            players[i].setToken(new Token(controller.chooseToken(players[i].getPlayerName(),
                        players[i] instanceof DigitalPlayer, restart)));

            controller.setTokenImage(i, players[i].getToken().getTokenName());
            if(!restarted)
                controller.drawToken(i, -1, 0);
            else
                controller.drawToken(i, 0, 0);
            //change player turn
            restart = false;
        }
        controller.setTokenImage(4, "thief");
        controller.drawPlayerBoxes(players);
    }

    public boolean isGameEnd() {
        int count = 0;
        for (Player p: players ) {
            if(p.isBankrupt() )
                count++;
        }
        return (count > 2) || (lapCount == lapLimit);
    }

    private void createDigitalPlayers(int playerCount) {
        String[] names = {"John AI","Beatrice AI", "Mike AI", "Ada AI"};
        PlayStrategy[] strategies = {new StingyDecorator( new EasyStrategy()),
                new StingyDecorator( new MediumStrategy()), new StingyDecorator( new HardStrategy()),
                new GenerousDecorator(new EasyStrategy()), new GenerousDecorator( new MediumStrategy()),
                new GenerousDecorator( new HardStrategy()), new RiskyDecorator( new EasyStrategy()),
                new RiskyDecorator(new MediumStrategy()), new RiskyDecorator( new HardStrategy()) };

        for(int i = playerCount; i < LAP; i++)
            players[i] = new DigitalPlayer(names[i], strategies[ (int) (Math.random() * 9)]);
    }

    public void startGame() {
        controller.setMap(board);
        gameLoop(0);
    }

    public void continueGame() {
        controller.setMap(board);
        controller.drawPlayerBoxes(players);
        for(int i = 0; i < LAP; i++) {
            controller.setTokenImage(i, players[i].getToken().getTokenName());
            if(!players[i].isBankrupt())
                controller.drawToken(i, players[i].getCurrentSpace().getIndex(), players[i].getCurrentSpace().getIndex());
        }
        int playerTurn = 0;
        for(int i = 0; i < players.length; i++){
            if(players[i].getPlayerName().equals(currentPlayer.getPlayerName()))
                playerTurn = i;
        }
        controller.setTokenImage(4, "thief");
        gameLoop(playerTurn);
    }

    //input from UI
    public void restartGame() {
        lapCount = 0;
        int oldIndex = 0;
        for ( int i = 0; i < LAP; i++ ) {
            players[i].reset();

            //move on board
            oldIndex = players[i].getCurrentSpace().getIndex();

            controller.drawToken(i, oldIndex, -1);
        }
        controller.drawPlayerBoxes(players);
        controller.labelUpdate(lapCount);
        restarted = true;
    }

    //input from UI
    public boolean saveGame() throws Exception{
        return fileManager.saveGame(this);
    }

    @Override
    public void update() {
        if(observable.getState() == 0) {
            Player highestBidder = ((Auction) observable).getHighestBidder();
            //add property to highest bidder
            ArrayList<Property> properties = highestBidder.getProperties();
            properties.add(((Auction) observable).getAuctionedProperty());
            highestBidder.setProperties(properties);
            ((Auction) observable).getAuctionedProperty().setOwner(highestBidder);

            //get payment from highest bidder
            highestBidder.setMoney(highestBidder.getMoney() - ((Auction) observable).getHighestBid());
        }
        else
            System.err.println("Auction started");
    }
}
