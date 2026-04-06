package serverLogic.terminal;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import common.ServiceResponse;
import common.ServiceResponse.ServiceStatus;
import dbLogic.restaurantDB.JoinWaitingListDBController;
import dbLogic.restaurantDB.SeatingAvailabilityController;
import dbLogic.restaurantDB.TableDBController;
import dbLogic.restaurantDB.VisitDBController;
import ocsf.server.ConnectionToClient;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler responsible for processing join waiting list requests from terminal clients.
 * Determines whether a subscriber can be seated immediately or added to the waiting list.
 */
public class JoinWaitingListHandler {

    /**
     * Handles the "JOIN_WAITING_LIST" server command.
     * Determines whether the subscriber can be seated immediately or added to the waiting list.
     *
     * @param messageList A list containing the request data sent from the client.
     * @param client The OCSF client connection used to send responses.
     */
    public void handle(ArrayList<Object> messageList, ConnectionToClient client) {

        // Debug log indicating handler activation
        System.out.println("JOIN_WAITING_LIST HANDLER CALLED");

        try {
            //  STEP 1: Extract number of guests from protocol 
            int numberOfGuests = (int) messageList.get(1);

            // STEP 2: Retrieve authenticated user ID from connection context
            Integer userId = (Integer) client.getInfo("userId");
            if (userId == null) {
                sendError(client, "User not authenticated");
                return;
            }
            
            // STEP 3: Validate restaurant operational status 
            if (!JoinWaitingListDBController.isRestaurantOpenNow()) {
                sendError(client, "RESTAURANT_CLOSED");
                return;
            }

            // STEP 4: Check if user is already active in the waiting list 
            try {
                if (JoinWaitingListDBController.isUserAlreadyActive(userId)) {
                    // Prevent duplicate active waiting list entries
                    sendError(client, "ALREADY_IN_LIST"); 
                    return; 
                }
            } catch (SQLException e) {
                e.printStackTrace();
                sendError(client, "DATABASE_ERROR");
                return;
            }
            
         // STEP 5: Fairness rule: if someone is already waiting, do not allow immediate entry
            try {
                if (JoinWaitingListDBController.hasWaitingGuests()) {

                    // Directly add the user to the waiting list (skip immediate seating logic)
                    long confirmationCode = System.currentTimeMillis();

                    JoinWaitingListDBController.insertWaitingListEntry(confirmationCode,userId,numberOfGuests,"WAITING");

                    client.sendToClient(new ServiceResponse(ServiceStatus.UPDATE_SUCCESS,confirmationCode));
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                sendError(client, "DATABASE_ERROR");
                return;
            }


            // STEP 6: Generate confirmation code
            long confirmationCode = System.currentTimeMillis();

            // STEP 7: Retrieve candidate tables based on party size 
            List<Integer> candidateTables = TableDBController.getCandidateTables(numberOfGuests);

            //DEBUG
            System.out.println("DEBUG – candidate tables: " + candidateTables);

            Integer chosenTableId = null;

         // STEP 8: Check seating availability against future reservations 
            /*
             * This step determines whether an immediate seating is possible without
             * compromising future reservations.
             *
             * The logic is table-based (not seat-based):
             * - Immediate entry assigns a specific table, consuming its full capacity.
             * - Even unused seats at that table become unavailable for future reservations.
             *
             * Algorithm:
             * 1. Calculate the restaurant's total seating capacity.
             * 2. Subtract the capacity of all currently unavailable tables
             *    (tables already assigned to active visits).
             * 3. For each candidate table (sorted by capacity in ascending order):
             *    a. Assume the table is assigned to the incoming guests.
             *    b. Subtract the table's full capacity from the remaining capacity.
             *    c. Verify that the remaining capacity is sufficient to accommodate
             *       all guests from future reservations within the defined time window.
             *
             * The first table that satisfies this condition is selected,
             * ensuring a best-fit strategy that minimizes capacity waste
             * and preserves larger tables for future use.
             */

            int totalCapacity = TableDBController.getRestaurantMaxCapacity();
            int unavailableCapacity = TableDBController.getUnavailableCapacity();
            int futureGuests = SeatingAvailabilityController.getFutureReservedGuests(LocalDateTime.now(),LocalDateTime.now().plusHours(2));

            for (Integer tableId : candidateTables) {

                int tableCapacity = TableDBController.getTableCapacity(tableId);

                int freeCapacityAfterSeating = totalCapacity - unavailableCapacity - tableCapacity;

                if (futureGuests <= freeCapacityAfterSeating) {
                    chosenTableId = tableId;
                    break;
                }
            }

            // STEP 9: Immediate seating scenario 
            if (chosenTableId != null) {

                // Mark the selected table as unavailable
                TableDBController.setTableUnavailable(chosenTableId);

                // Create an ACTIVE visit entry and initialize a connected bill
                int billId = VisitDBController.insertVisitAndCreateBill(confirmationCode,chosenTableId,userId);

                // Build response payload for immediate entry
                Map<String, Object> data = new HashMap<>();
                data.put("mode", "IMMEDIATE");
                data.put("confirmationCode", confirmationCode);
                data.put("tableId", chosenTableId);

                // Send success response with immediate seating details
                client.sendToClient(new ServiceResponse(ServiceStatus.UPDATE_SUCCESS,data));
                return;
            }

            // STEP 10: No table available – add to waiting list 
            JoinWaitingListDBController.insertWaitingListEntry(confirmationCode,userId,numberOfGuests,"WAITING");

            // Send success response with confirmation code only
            client.sendToClient(new ServiceResponse(ServiceStatus.UPDATE_SUCCESS,confirmationCode));

        } catch (Exception e) {
            e.printStackTrace();
            sendError(client, "INTERNAL_ERROR");
        }
    }

    /**
     * Sends a standardized error response back to the client.
     */
    private void sendError(ConnectionToClient client, String msg) {
        try {
            client.sendToClient(new ServiceResponse(ServiceStatus.INTERNAL_ERROR, msg));
        } catch (IOException ignored) {}
    }
}