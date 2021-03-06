package com.company;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Management implements Serializable {
    private static final UserInterface USER_INTERFACE = new UserInterface();
    private static Statement sqlStatement;
    private final static String TOTAL_BILL = "_bill.txt";
    private static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public void newCustomer() throws SQLException, IOException {
        sqlStatement = Run.getSqlStatement();
        String firstName = USER_INTERFACE.enterValue("customer first name");
        String lastName = USER_INTERFACE.enterValue("customer last name");
        String fullName = firstName + " " + lastName;
        String contactNumber = USER_INTERFACE.enterValue("customer contact number");

        PreparedStatement statement = sqlStatement.getConnection().prepareStatement("INSERT INTO customer(Customer_name, contact_number)\n" +
                "VALUES( ? , ? );");
        statement.setString(1, fullName);
        statement.setString(2, contactNumber);

        statement.executeUpdate();

        int customerID = ResultClass.getCustomerId(sqlStatement, fullName);

        System.out.println(fullName + " successfully registered with customer Id: " + customerID);
        System.out.println();

        List<Transaction> transactions = new ArrayList<>();

        DataHandler.writeToFile(transactions, customerID + TOTAL_BILL);
    }

    public void searchCustomer() throws SQLException {
        sqlStatement = Run.getSqlStatement();
        System.out.println("1. Search by Id");
        System.out.println("2. Search by name");
        int ans = USER_INTERFACE.enterInteger("1 or 2", 1, 2);
        switch (ans) {
            case 1 -> {
                int customerId = USER_INTERFACE.enterInteger("customer ID", 1, 100);
                customerId = ResultClass.idMatches("customer", customerId, "id");
                PreparedStatement statement = sqlStatement.getConnection().prepareStatement("SELECT * FROM CUSTOMER WHERE ID = ? ");
                statement.setInt(1, customerId);

                statement.executeQuery();

                ResultClass.setResult(sqlStatement, "customer", "id", customerId);
                System.out.println("User found:");
                System.out.println();
                listAllFromTable();
            }
            case 2 -> {
                String firstName = USER_INTERFACE.enterValue("customer first name");
                String lastName = USER_INTERFACE.enterValue("customer last name");
                String fullName = firstName + " " + lastName;

                String query = "SELECT * FROM customer WHERE customer_name LIKE '" + fullName + "';";
                boolean userFound = ResultClass.ifInputMatches(sqlStatement, query, "id");

                if (userFound) {
                    System.out.println("User found:");
                    System.out.println();

                    int customerId = ResultClass.getCustomerId(sqlStatement,fullName);
                    PreparedStatement statement = sqlStatement.getConnection().prepareStatement("SELECT * FROM CUSTOMER WHERE ID = ? ");
                    statement.setInt(1, customerId);

                    statement.executeQuery();

                    ResultClass.setResult(sqlStatement, "customer", "id", customerId);
                    listAllFromTable();
                } else {
                    System.out.println("No user was found!");
                }
            }
        }
        System.out.println();
    }

    public void deleteCustomer() throws SQLException {
        sqlStatement = Run.getSqlStatement();
        int customerId = USER_INTERFACE.enterInteger("customer ID", 1, 100);
        customerId = ResultClass.idMatches("customer", customerId, "Id");

        PreparedStatement statement = sqlStatement.getConnection().prepareStatement("DELETE FROM customer WHERE id = ?");
        statement.setInt(1, customerId);

        statement.executeUpdate();

        System.out.println("Customer with Id " + customerId + " successfully deleted.");
        System.out.println();
    }

    public void foodOrder(int foodChoice) throws IOException, ClassNotFoundException {

        int customerID = USER_INTERFACE.enterInteger("customer id", 1, 100);

        List<Transaction> foodOrder = DataHandler.readFromFile(customerID + TOTAL_BILL);

        foodOrder.add(Food.listOfFood().get(foodChoice - 1));

        DataHandler.writeToFile(foodOrder, customerID + TOTAL_BILL);
        foodOrder = DataHandler.readFromFile(customerID + TOTAL_BILL);
        System.out.println();
        System.out.println("Order History:");
        foodOrder.stream().filter(s -> s instanceof Food)
                .map(s -> (Food) s)
                .map(s -> s.getNameOfTransaction() + " -" + s.getPrice() + " SEK")
                .forEach(System.out::println);
        System.out.println();
        System.out.println("Total ammount:");
        long totalFoodOrder = foodOrder.stream()
                .filter(s -> s instanceof Food)
                .map(s -> (Food) s)
                .mapToInt(Food::getPrice).summaryStatistics().getSum();
        System.out.println(totalFoodOrder + " SEK");
        System.out.println();
    }

    public void checkOutWithBill(int customerID) throws SQLException, IOException, ClassNotFoundException {
        sqlStatement = Run.getSqlStatement();

        customerID = ResultClass.idMatches("customer", customerID, "id");

        PreparedStatement statement = sqlStatement.getConnection().prepareStatement("UPDATE roombooking SET checkOutDate = CURRENT_TIMESTAMP, roomavailable = true, customer_id = null WHERE customer_id = ? ;");

        statement.setInt(1, customerID);

        statement.executeUpdate();

        getBill(customerID);
        System.out.println();
    }

    public int roomDetails(int roomChoice) throws IOException, ClassNotFoundException {
        sqlStatement = Run.getSqlStatement();

        List<Room> rooms = DataHandler.readFromFile("Rooms.txt");
        System.out.println();
        rooms.stream().filter(s -> s.typeOfRoom == roomChoice)
                .map(s -> s.roomName).forEach(System.out::println);
        System.out.println("-------------------------------");
        rooms.stream().filter(s -> s.typeOfRoom == roomChoice)
                .map(s -> s.roomDescription).forEach(System.out::println);
        System.out.println("-------------------------------");
        System.out.print("Price: ");
        rooms.stream().filter(s -> s.typeOfRoom == roomChoice)
                .map(s -> s.price).forEach(System.out::print);
        System.out.println("KR per night");
        System.out.println();

        return roomChoice;
    }

    public void roomAvailability() throws SQLException {
        sqlStatement = Run.getSqlStatement();
        ResultClass.setAvailableRoomsResult(sqlStatement, "availablerooms");
        listAllFromTable();
        System.out.println();
    }

    public <T> void bookRoom() throws SQLException, IOException, ClassNotFoundException {
        sqlStatement = Run.getSqlStatement();

        boolean loop = true;
        while (loop) {
            int roomChoice = USER_INTERFACE.roomChoice();

            if (roomChoice > 0) {
                String query = ("SELECT * FROM availableRooms WHERE roomnumber = " + roomChoice + ";");
                boolean roomsAvailable = ResultClass.ifInputMatches(sqlStatement, query, "roomNumber");

                if (roomsAvailable) {
                    roomDetails(roomChoice);
                    if (USER_INTERFACE.confirm("Would you like to book this room?")) {

                        int numberOfNights = USER_INTERFACE.enterInteger("number of nights to book", 1, 14);

                        int customerID = USER_INTERFACE.enterInteger("customer id", 1, 100);
                        customerID = ResultClass.idMatches("customer", customerID, "id");

                        query = "SELECT * FROM roombooking WHERE customer_id = " + customerID + " AND roomavailable = 0;";

                        boolean allreadyHaveARoom = ResultClass.ifInputMatches(sqlStatement, query, "customer_id");

                        if (allreadyHaveARoom) {
                            if (USER_INTERFACE.confirm("You already have a booked room, do you want to check out?")) {
                                checkOutWithBill(customerID);
                            }
                        } else {
                            PreparedStatement statement = sqlStatement.getConnection().prepareStatement("UPDATE roombooking " +
                                    "SET customer_id = ?, checkindate = CURRENT_TIMESTAMP, roomavailable = false, " +
                                    "checkOutDate = DATE_ADD(checkindate,INTERVAL " + numberOfNights + " DAY) WHERE roomnumber = " + roomChoice + ";");
                            statement.setInt(1, customerID);

                            statement.executeUpdate();

                            System.out.println("The room is now booked!:");

                            ResultClass.setResult(sqlStatement, "bookedroom", "customer_id", customerID);

                            listAllFromTable();
                            System.out.println();

                            List<T> transactions = new ArrayList<>();

                            Room room = Room.rooms().get(roomChoice - 1);
                            room.setQuantity(numberOfNights);
                            transactions.add((T) room);

                            DataHandler.writeToFile(transactions, customerID + TOTAL_BILL);

                            loop = false;
                        }
                    }
                } else {
                    System.out.println("That room is unfortunate not available, please choose another room");
                    System.out.println();
                }
            } else {
                loop = false;
            }
        }
    }

    public void listAllFromTable() throws SQLException {
        ResultSet result = ResultClass.getResult();

        int columnCount = result.getMetaData().getColumnCount();

        String[] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = result.getMetaData().getColumnName(i + 1);
        }

        for (String columnName : columnNames) {
            System.out.print(UserInterface.PadRight(columnName));
        }

        while (result.next()) {
            System.out.println();
            for (String columnName : columnNames) {
                String value = result.getString(columnName);

                if (value == null)
                    value = "null";

                System.out.print(UserInterface.PadRight(value));
            }
        }
        System.out.println();
    }

    public static void getBill(int customerId) throws IOException, ClassNotFoundException {

        List<Transaction> transactions = DataHandler.readFromFile(customerId + TOTAL_BILL);

        DataHandler.writeToFile(transactions, customerId + TOTAL_BILL);

        transactions = DataHandler.readFromFile(customerId + TOTAL_BILL);
        System.out.println("Bill for customer with id " + customerId + ":");
        System.out.println();

        System.out.println("......ROOM COST......");
        transactions.stream()
                .filter(s -> s instanceof Room)
                .map(s -> (Room) s)
                .map(s -> "- " + s.getNameOfTransaction() + " " + s.getPrice() + " SEK - " + s.getQuantity() + " nights\n\n\t-" + s.getPrice() * s.getQuantity() + " SEK")
                .forEach(System.out::println);

        long totalRoomCost = (transactions.stream()
                .filter(s -> s instanceof Room)
                .map(s -> (Room) s)
                .mapToInt(s -> s.getPrice() * s.getQuantity()).summaryStatistics().getSum());
        System.out.println();

        long roomServiceCost = transactions.stream()
                .filter(s -> s instanceof Food)
                .map(s -> (Food) s)
                .mapToInt(s -> s.getPrice()).summaryStatistics().getSum();

        if (roomServiceCost > 0) {
            System.out.println("......ROOMSERVICE......");

            transactions.stream()
                    .filter(s -> s instanceof Food)
                    .map(s -> (Food) s)
                    .map(s -> "- " + s.getNameOfTransaction() + " " + s.getPrice() + " SEK")
                    .forEach(System.out::println);

            long foodOrder = transactions.stream()
                    .filter(s -> s instanceof Food)
                    .map(s -> (Food) s)
                    .mapToInt(Food::getPrice).summaryStatistics().getSum();

            System.out.println();
            System.out.println("\t-" + ((totalRoomCost + foodOrder)) + " SEK");
            System.out.println();
        }
        System.out.println("Thanks for visiting us, welcome back!");
    }

    public void setup() throws SQLException, IOException {
        if (USER_INTERFACE.confirm(ANSI_RED + "Are you sure that you want to Factory Reset?" + ANSI_RESET)) {

            sqlStatement = Run.getSqlStatement();
            List<Room> newFile = Room.rooms();
            DataHandler.writeToFile(newFile, "Rooms.txt");

            //#region Creating database
            String dropDatabase = "DROP DATABASE hotel_booking_system;";
            String createDatabase = "CREATE DATABASE hotel_booking_system;";
            String useDatabase = "USE hotel_booking_system";
            String createCustomerTable = "CREATE TABLE customer (id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, customer_name VARCHAR(50) NOT NULL, contact_number VARCHAR(50));";
            String createRoomTable = "CREATE TABLE room (roomNumber INT NOT NULL PRIMARY KEY AUTO_INCREMENT, typeOfRoom INT NOT NULL, room_Size VARCHAR(100), price_per_night INT NOT NULL);";
            String createRoomBookingTable = "CREATE TABLE roomBooking (id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, roomNumber INT NOT NULL, customer_id INT DEFAULT NULL, roomAvailable BIT DEFAULT 1, checkInDate DATETIME DEFAULT NULL, checkOutDate DATETIME DEFAULT NULL, FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,FOREIGN KEY (roomnumber) REFERENCES room(roomNumber) ON DELETE CASCADE);";
            String createViewAvailableRooms = "CREATE VIEW availableRooms AS SELECT DISTINCT room.roomNumber, room_size FROM room JOIN roomBooking ON roomBooking.roomNumber = room.roomNumber WHERE roomAvailable = 1;";
            String createViewBookedRooms = "CREATE VIEW bookedroom AS SELECT DISTINCT customer_id, typeOfroom, price_per_night FROM room JOIN roombooking ON room.roomnumber = roombooking.roomnumber;";
            //#endregion

            //#region Creating rooms
            String createStandardSingle = "INSERT INTO room (typeOfRoom, room_size, price_per_night) VALUES (1, 'Standard single', 450);";
            String createStandardDouble = "INSERT INTO room (typeOfRoom, room_size, price_per_night) VALUES (2, 'Standard dobule', 600)";
            String createDeluxeSingle = "INSERT INTO room (typeOfRoom, room_size, price_per_night) VALUES (3, 'Deluxe single', 850);";
            String createDeluxeDouble = "INSERT INTO room (typeOfRoom, room_size, price_per_night) VALUES (4, 'Deluxe single', 1250);";
            //#endregion

            //#region Sending queries to mySql
            ResultClass.setupResult(sqlStatement, dropDatabase);
            ResultClass.setupResult(sqlStatement, createDatabase);
            ResultClass.setupResult(sqlStatement, useDatabase);
            ResultClass.setupResult(sqlStatement, createCustomerTable);
            ResultClass.setupResult(sqlStatement, createRoomTable);
            ResultClass.setupResult(sqlStatement, createRoomBookingTable);
            ResultClass.setupResult(sqlStatement, createViewAvailableRooms);
            ResultClass.setupResult(sqlStatement, createViewBookedRooms);
            ResultClass.setupResult(sqlStatement, createStandardSingle);
            ResultClass.setupResult(sqlStatement, createStandardDouble);
            ResultClass.setupResult(sqlStatement, createDeluxeSingle);
            ResultClass.setupResult(sqlStatement, createDeluxeDouble);
            //#endregion

            //Inserting rooms into booking system
            for (int i = 1; i < 5; i++) {
                String createRoomNumbers = "INSERT INTO roombooking (roomnumber) VALUES (" + i + ");";
                ResultClass.setupResult(sqlStatement, createRoomNumbers);
            }
            System.out.println("Factory Reset - Complete\n" +
                    "Database has been set up from scratch!\n");
        } else {
            System.out.println("Factory Reset - Cancelled\n");
        }
    }
}
