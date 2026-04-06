package serverLogic.scheduling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import MainControllers.ServerController;
import dbLogic.restaurantDB.JoinWaitingListDBController;
import dbLogic.restaurantDB.WaitingListController;

/**
 * Scheduler responsible for handling waiting list timeout operations.
 * This class manages time-based actions related to waiting list entries.
 */
public class WaitingListScheduler {

    private static final ScheduledExecutorService scheduler =Executors.newScheduledThreadPool(1);

    /**
     * Starts a 15-minute no-show timer for a notified waiting list entry.
     * If the entry status remains NOTIFIED after the timeout,
     * the entry is marked as NOSHOW and the waiting list is resumed.
     *
     * @param confirmationCode The confirmation code associated with the waiting list entry.
     * @param tableId The ID of the table assigned to the waiting list entry.
     */
    public static void startNoShowTimer(long confirmationCode,int tableId) 
    {

        ServerController.log("[WAITING LIST] 15-minute timer started for code: " + confirmationCode);

        scheduler.schedule(() -> {

            try {
                // Check whether the status is still NOTIFIED

                String currentStatus =JoinWaitingListDBController.getStatusByCode(confirmationCode);

                if ("NOTIFIED".equals(currentStatus)) {

                    // Update entry status to NOSHOW
                	JoinWaitingListDBController.updateStatus(confirmationCode,"NOSHOW");

                    ServerController.log("[WAITING LIST] Customer NOSHOW. Code: " + confirmationCode);

                    // Table is still available – trigger waiting list handling
                    WaitingListController.handleTableFreed(tableId);
                }

            } catch (Exception e) {
                ServerController.log("[WAITING LIST] Error in no-show timer: " + e.getMessage());
            }

        }, 15, TimeUnit.MINUTES);
    }
}
