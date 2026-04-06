package common;

import java.io.Serializable;

/**
 * Represents a physical dining table within the Bistro restaurant system.
 * This class stores table identification, seating capacity, and current availability status.
 */
public class Table implements Serializable {
    
    /** Serial version UID for serialization compatibility. */
    private static final long serialVersionUID = 1L;

    /** Unique identifier for the table. */
    private int tableId;
    
    /** Maximum number of diners that can be seated at the table. */
    private int capacity;
    
    /** Current status indicating if the table is free for reservation or seating. */
    private boolean isAvailable;
    
    /**
     * Constructs a new Table instance with specified details.
     * * @param tableId     The unique identifier for the table.
     * @param capacity    The maximum seating capacity.
     * @param isAvailable The initial availability status.
     */
    public Table(int tableId, int capacity, boolean isAvailable) {
        this.tableId = tableId;
        this.capacity = capacity;
        this.isAvailable = isAvailable;    
    }

    /**
     * Retrieves the unique identifier of the table.
     * * @return The table ID as an integer.
     */
    public int getTableId() {
        return tableId;
    }

    /**
     * Updates the unique identifier of the table.
     * * @param tableId The new table ID to set.
     */
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    /**
     * Retrieves the seating capacity of the table.
     * * @return The capacity as an integer.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Updates the seating capacity of the table.
     * * @param capacity The new capacity to set.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Checks the current availability status of the table.
     * * @return True if available, false otherwise.
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Updates the availability status of the table.
     * * @param isAvailable The new availability status to set.
     */
    public void setAvailabe(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}