package dbLogic.restaurantDB;

import java.time.LocalDateTime;
import java.util.List;

import MainControllers.ServerController;
import common.WaitingListEntry;
import serverLogic.scheduling.WaitingListScheduler;

/**
 * WaitingListController handles the logic for managing the restaurant's waiting list queue.
 * It coordinates the process of notifying waiting guests when tables become available,
 * ensuring fair allocation based on entry time (FIFO) and future reservation conflicts.
 */
public class WaitingListController {

	/**
     * Triggered whenever a table in the restaurant is freed.
     * This method searches the waiting list for the first suitable party (FIFO) 
     * that can fit at the table without conflicting with upcoming reservations.
     *
     * @param tableId The unique identifier of the table that has just become available.
     * @throws Exception Although caught internally, this method handles complex logic involving 
     * several database controllers and scheduled tasks.
     */
	public static void handleTableFreed(int tableId) {
        try {
            int tableCapacity = TableDBController.getTableCapacity(tableId);

            List<WaitingListEntry> waitingList =
            		VisitDBController.getWaitingEntriesOrderedByEntryTime();

            for (WaitingListEntry entry : waitingList) {

                int guests = entry.getNumberOfGuests();

                if (guests > tableCapacity) {
                    continue; 
                }

                boolean canSeat =
                    SeatingAvailabilityController
                        .canSeatWithFutureReservations(
                            tableId,
                            LocalDateTime.now()
                        );

                if (canSeat) {
                    JoinWaitingListDBController.updateStatus(
                        entry.getConfirmationCode(),
                        "NOTIFIED"
                    );

                    ServerController.log(
                    	    "[WAITING LIST] Notification sent to customer. " +
                    	    "confirmationCode=" + entry.getConfirmationCode() +
                    	    ", guests=" + entry.getNumberOfGuests()
                    	);
                    
                    WaitingListScheduler.startNoShowTimer(
                            entry.getConfirmationCode(),
                            tableId
                        );
                    
                    return; 
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
