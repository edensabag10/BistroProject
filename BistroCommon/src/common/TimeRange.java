package common;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Utility class representing a specific time interval (e.g., Opening Hours).
 * This class is crucial for validating if a requested reservation falls within 
 * the operational hours of the restaurant.
 * * <p>Implemented as {@link Serializable} to allow cross-network transfer between 
 * Client and Server via OCSF.</p>
 * * @author Software Engineering Student
 * @version 1.0
 */
public class TimeRange implements Serializable {
    
    /** Serial version UID for maintaining serialization consistency across different JVMs. */
    private static final long serialVersionUID = 1L;
    
    /** The start of the time interval in "HH:mm" format. */
    private String openTime;  
    
    /** The end of the time interval in "HH:mm" format. */
    private String closeTime; 

    /**
     * Constructs a TimeRange with designated start and end points.
     * * @param openTime Opening time string (e.g., "08:00").
     * @param closeTime Closing time string (e.g., "22:00").
     */
    public TimeRange(String openTime, String closeTime) {
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    /**
     * Core Algorithm: Determines if a given time string falls within this defined range.
     * This method is designed to handle three distinct logical scenarios:
     * 1. Closed: When open and close times are identical (e.g., 00:00 to 00:00).
     * 2. Standard ranges: (e.g., 08:00 to 22:00).
     * 3. Midnight-crossing ranges: (e.g., 22:00 to 02:00).
     *
     * @param timeStr The time to check in "HH:mm" format.
     * @return true if the time is within the range (inclusive), false otherwise.
     */
    public boolean isWithinRange(String timeStr) {
        try {
            LocalTime target = LocalTime.parse(timeStr);
            LocalTime start = LocalTime.parse(openTime);
            LocalTime end = LocalTime.parse(closeTime);
            
            if (start.equals(end)) {
                return false; 
            }

            /**
             * תיקון: 
             * התחלה: כוללת (!isBefore -> >=)
             * סוף: לא כוללת (isBefore -> <)
             */
            if (start.isBefore(end)) {
                // הלקוח יכול להזמין מ-08:00 ועד 22:59, אבל לא ב-23:00 בדיוק
                return (!target.isBefore(start) && target.isBefore(end));
            } 
            
            // טיפול במעבר חצות (למשל 22:00 עד 02:00)
            // מותר אם זה בין הפתיחה לחצות, או בין חצות לסגירה (לא כולל הסגירה)
            return (!target.isBefore(start) || target.isBefore(end));
            
        } catch (DateTimeParseException e) {
            System.err.println("TimeRange Error: Invalid time format - " + timeStr);
            return false;
        }
    }

    /** @return The starting time of this range. */
    public String getOpenTime() { 
        return openTime; 
    }

    /** @return The ending time of this range. */
    public String getCloseTime() { 
        return closeTime; 
    }

    @Override
    public String toString() {
        if (openTime.equals(closeTime)) {
            return "Closed";
        }
        return openTime + " - " + closeTime;
    }
}