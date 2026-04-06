package serverLogic.scheduling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import MainControllers.ServerController;
import dbLogic.restaurantDB.JoinWaitingListDBController;
import dbLogic.restaurantDB.VisitController;
import dbLogic.restaurantDB.WaitingListController;

/**
 * Scheduler responsible for handling delayed visit-related tasks.
 * This class manages time-based actions such as no-show handling.
 */
public class VisitScheduler {
	
	private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    /**
     * Starts a 15-minute no-show timer for a notified waiting list entry.
     * If the visit status remains NOTIFIED after the timeout,
     * the visit is marked as NOSHOW and the table is released.
     *
     * @param confirmationCode The confirmation code associated with the visit.
     * @param tableId The ID of the table reserved for the visit.
     */
    public static void startNoShowTimer(long confirmationCode,int tableId) 
    {
        ServerController.log("[VISIT] 15-minute timer started for code: " + confirmationCode);
        scheduler.schedule(() -> {

            try {
                // Check whether the status is still NOTIFIED
                String currentStatus =
                        VisitController.getStatusByCode(confirmationCode);

                if ("NOTIFIED".equals(currentStatus)) {

                    // Update visit status to NOSHOW
                	VisitController.updateStatus(confirmationCode,"NOSHOW");

                    ServerController.log("[VISIT] Customer NOSHOW. Code: " + confirmationCode);

                    // Table is still available – trigger waiting list handling
                    VisitController.handleTableFreed(tableId);
                }

            } catch (Exception e) {
                ServerController.log("[VISIT] Error in no-show timer: " + e.getMessage());
            }

        }, 15, TimeUnit.MINUTES);
    }

}
