package com.company;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Run {
    private static final UserInterface USER_INTERFACE = new UserInterface();
    private final static Management MANAGEMENT = new Management();
    private static Statement sqlStatement = null;
    private static final String url = "jdbc:mysql://localhost:3306/hotel_booking_system?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static final String user = "root";
    private static boolean exitProgram = false;
    private static boolean exitLoop = false;
    private static Connection connection;

    public void Program() throws SQLException {
        connection = DriverManager.getConnection(url, user, "SuperSecretPassword");
        try {
            createSqlStatement();
            while (!exitProgram) {
            int selection = USER_INTERFACE.adminOrCustomerChoice();
            adminOrCustomer(selection);
        }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            connection.close();
        }
    }

    public void adminOrCustomer(int selection) throws SQLException {
        switch (selection) {
            case 1 -> {
                while (!exitLoop) {
                    selection = USER_INTERFACE.adminChoice();
                    adminView(selection);
                }
            }
            case 2 -> {
                while (!exitLoop) {
                    USER_INTERFACE.customerChoice();
                    customerView(selection);
                }
            }
            case 0 -> exitProgram = true;
        }
    }

    public void adminView(int selection) throws SQLException {
        switch (selection) {
            case 1 -> MANAGEMENT.newCustomer();
            case 2 -> MANAGEMENT.searchCustomer();
            case 3 -> MANAGEMENT.manageCustomer();
            case 4 -> MANAGEMENT.foodOrder();
            case 5 -> MANAGEMENT.checkOutWithBill();
            case 0 -> exitLoop = true;
        }
    }

    public void customerView(int selection) throws SQLException {
        switch (selection) {
            case 1 -> MANAGEMENT.roomInfo();
            case 2 -> MANAGEMENT.roomAvailability();
            case 3 -> MANAGEMENT.bookRoom();
            case 4 -> MANAGEMENT.orderFood();
            case 5 -> exitLoop = true;
        }
    }

    public static Statement getSqlStatement() {
        return sqlStatement;
    }

    public static void createSqlStatement() throws SQLException {
        System.out.println("Anslutningen lyckades!");
        sqlStatement = connection.createStatement();
    }
}
