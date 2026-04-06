package common; // Defining the package name where this class resides

import java.io.Serializable; // Importing Serializable interface to allow object serialization

/**
 * Represents a financial record of a transaction in the Bistro system.
 * This class stores price details, discounts, and confirmation identifiers.
 */
public class Bill implements Serializable {

    /** Unique version identifier for serialization compatibility. */
    private static final long serialVersionUID = 1L;
    
    /** Unique ID of the bill. */
    private long billId;
    
    /** Confirmation code associated with the transaction. */
    private long confirmationCode;
    
    /** The initial cost before any discounts are applied. */
    private double baseAmount;
    
    /** The percentage of discount to be subtracted from the base amount. */
    private double discountPercent;
    
    /** The final price the customer needs to pay after calculations. */
    private double finalAmount;

    /**
     * Constructs a new Bill instance with complete transaction details.
     * * @param billId           The unique identifier for the bill.
     * @param confirmationCode The unique code confirming the transaction.
     * @param baseAmount       The original price before discounts.
     * @param discountPercent  The discount rate applied (in percentage).
     * @param finalAmount      The total amount to be paid after discount.
     */
    public Bill(long billId, long confirmationCode, double baseAmount, double discountPercent, double finalAmount) {
        this.billId = billId;
        this.confirmationCode = confirmationCode;
        this.baseAmount = baseAmount;
        this.discountPercent = discountPercent;
        this.finalAmount = finalAmount;
    }

    // --- Getters Section ---

    /**
     * Gets the unique identifier of the bill.
     * * @return The bill ID as a long.
     */
    public long getBillId() { 
        return billId; 
    }

    /**
     * Gets the confirmation code linked to this bill's transaction.
     * * @return The confirmation code as a long.
     */
    public long getConfirmationCode() { 
        return confirmationCode; 
    }

    /**
     * Gets the initial amount of the bill before any discounts.
     * * @return The base amount as a double.
     */
    public double getBaseAmount() { 
        return baseAmount; 
    }

    /**
     * Gets the discount percentage applied to this bill.
     * * @return The discount percent as a double.
     */
    public double getDiscountPercent() { 
        return discountPercent; 
    }

    /**
     * Gets the final calculated amount that the customer is charged.
     * * @return The final amount as a double.
     */
    public double getFinalAmount() { 
        return finalAmount; 
    }
}