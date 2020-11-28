package board;

public class TaxSpace extends Space {
    private enum TaxType {
        LUXURY, INCOME
    }

    private TaxType type;

    public TaxSpace(String taxType) {
        if (taxType.equals("LUXURY")) {
            type = TaxType.LUXURY;
            setName("Luxury Tax");
        }
        else {
            type = TaxType.INCOME;
            setName("Income Tax");
        }
    }

    public int getTax() {
        if(type == TaxType.INCOME)
            return 10;
        else
            return 20;
    }

}
