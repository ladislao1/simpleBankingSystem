package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static Random random = new Random();
    static Scanner scanner = new Scanner(System.in);

    public static String generateCardNumber() {
        StringBuilder builder = new StringBuilder("400000");
        for (int i = 0; i < 9; i++) {
            builder.append(random.nextInt(10));             // generate random card
        }                                                           // last digit generate with Luhn algoritm

        /*-----    Luhn Algoritm -------*/
        int sum = 0;
        for (int i = 0; i < builder.length(); i++) {
            int ch = builder.charAt(i) - '0';

            if (i % 2 == 0) {  // если четное, умножаем на 2
                ch = ch < 5 ? ch * 2 : ch * 2 - 9;
            }
            sum += ch;
        }
        builder.append((10 - (sum % 10)) % 10);
        return builder.toString();
    }

    public static boolean checkCardIfLuhn(String cardNumber) {
        int checkSum = cardNumber.charAt(cardNumber.length() - 1);
        String temp = cardNumber.substring(0, cardNumber.length() - 1);
        int sum = 0;
        for (int i = 0; i < temp.length(); i++) {
            int ch = temp.charAt(i) - '0';

            if (i % 2 == 0) {  // если четное, умножаем на 2
                ch = ch < 5 ? ch * 2 : ch * 2 - 9;
            }
            sum += ch;
        }
        return checkSum == (10 - (sum % 10)) % 10;
    }

    public static String generatePin() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            builder.append(random.nextInt(10)); // генерируем рандомный пин-код
        }
        return builder.toString();
    }

    public static void mainMenu() {

        System.out.println("1. Create an account\n2. Log into account\n0. Exit");
        switch (scanner.nextInt()) {
            case 1:
                generateNewCard();
                break;
            case 2:
                checkCard();
                break;
            case 0:
                exit();
                break;
            default:
                break;
        }
    }

    private Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:card.s3db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public static void generateNewCard() {

        String tempCard = generateCardNumber();
        String tempPin = generatePin();
        String sql = "INSERT INTO card (number, pin) VALUES (?, ?)";

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:card.s3db");
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {

                PreparedStatement pstmt = con.prepareStatement(sql);
                pstmt.setString(1, tempCard);
                pstmt.setString(2, tempPin);

                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("\nYour card have been created\n" +
                "Your card number:\n" + tempCard + "\nYour card PIN:\n" + tempPin + "\n");
        //builder.append(tempCard + " " + tempPin + " ");

        mainMenu();
    }

    public static void checkCard() {

        System.out.println("\nEnter your card number:");
        String checkCard = scanner.next();
        System.out.println("\nEnter your PIN:");
        String checkPin = scanner.next();
        //int lookingForaCard = builder.lastIndexOf(checkCard);

        String sql = "SELECT * FROM card WHERE number = ?";
        String pin = "";

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:card.s3db");

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {

                PreparedStatement pstmt = con.prepareStatement(sql);
                pstmt.setString(1, checkCard);

                try (ResultSet cardCheck = statement.executeQuery(sql)) {
                    while (cardCheck.next()) {

                       cardCheck.getInt("id");
                       cardCheck.getString("number");
                       pin = cardCheck.getString("pin");
                       cardCheck.getLong("balance");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("user entered pin:" + checkPin);
        System.out.println("real pin:" + pin);


        if (checkPin.equals(pin)) {
            System.out.println("You have successfully logged in!\n");
            inAccount(checkCard);
        } else {
            System.out.println("\nWrong card number or PIN!\n");
            mainMenu();
        }
    }

    public static void inAccount(String cardNumber) {
        String sql;
        long balance = 0;

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:card.s3db");

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {

                sql = "SELECT * FROM card WHERE number = '?'";

                PreparedStatement pstmt = con.prepareStatement(sql);
                pstmt.setString(1, cardNumber);

                try (ResultSet cardRead = pstmt.executeQuery(sql)) {
                    while (cardRead.next()) {
                /* --- get values for this account -- */
                        cardRead.getInt("id");
                        cardRead.getString("number");
                        cardRead.getString("pin");
                        balance = cardRead.getLong("balance");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                /* --- read balance and other values from base --- */

                System.out.println("1. Balance\n2. Add income\n3. Do transfer\n4. Close account\n5. Log out\n0. Exit");
                switch (scanner.nextInt()) {

                    case 1:
                        /* ---- show balance ---*/

                        System.out.println("Balance: " + balance);

                        inAccount(cardNumber);
                        break;

                    case 2:
                        /* ---- connection to sql and add income transfer ---*/
                        System.out.println("\nEnter income:");
                        String income = scanner.next();

                        sql = "UPDATE card SET number = ?, balance = balance + ?";

                        PreparedStatement pstmt2 = con.prepareStatement(sql);
                        pstmt2.setString(1, cardNumber);
                        pstmt2.setString(2, income);

                        pstmt2.executeUpdate();

                        System.out.println("\nIncome was added!");
                        break;
                    /*----------------------------------------------------------------*/
                    case 3:
                        /* ---- connection to sql and do transfer ---*/
                        System.out.println("\nTransfer\nEnter card number:");
                        String cardTransferTo = scanner.next();

                        if (!checkCardIfLuhn(cardTransferTo)) {
                            System.out.println("Probably you made mistake in the card number. Please try again!");
                            inAccount(cardNumber);
                        }

                        sql = "SELECT id FROM card WHERE number = '?'";

                        PreparedStatement pstmt3 = con.prepareStatement(sql);
                        pstmt3.setString(1, cardTransferTo);

                        try (ResultSet cardId = pstmt3.executeQuery(sql)) {
                            if (!cardId.next()) {
                                System.out.println("Such a card does not exist.");
                                inAccount(cardNumber);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        if (cardTransferTo.equals(cardNumber)) {
                            System.out.println("You can't transfer money to the same account!");
                            inAccount(cardNumber);
                        }

                        System.out.println("Enter how much money you want to transfer:");

                        long sumTransfer = scanner.nextLong();

                        if (sumTransfer > balance) {
                            System.out.println("Not enough money!");
                        } else {
                            /* add money to account*/
                            sql = "UPDATE card SET number = ?, balance = balance + ?";

                            PreparedStatement pstmt4 = con.prepareStatement(sql);
                            pstmt4.setString(1, cardTransferTo);
                            pstmt4.setString(2, String.valueOf(sumTransfer));

                            pstmt4.executeUpdate();

                            /* take money from first account*/

                            sql = "UPDATE card SET number = ?, balance = balance - ?";

                            PreparedStatement pstmt5 = con.prepareStatement(sql);
                            pstmt5.setString(1, cardNumber);
                            pstmt5.setString(2, String.valueOf(sumTransfer));

                            pstmt5.executeUpdate();

                            System.out.println("Success!");
                        }

                        break;

                    case 4:
                        /* ---- connection to sql and delete account ----*/
                        PreparedStatement pstmt6 = con.prepareStatement("DELETE FROM card WHERE number = ?");
                        pstmt6.setString(1, cardNumber);

                        pstmt6.executeUpdate();

                        System.out.println("\nThe account has been closed!");
                        break;

                    case 5: // log out
                        System.out.println("\nYou have successfully logged out!\n");
                        mainMenu();
                        break;

                    case 0: // exit
                        exit();
                        break;

                    default:
                        System.out.println("Incorrect choice");
                        break;
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void exit() {
        System.out.println("\nBye!");
    }

    public static void main(String[] args) {
        String url = "jdbc:sqlite:" + args[1];

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {

                statement.executeUpdate("CREATE TABLE card (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "number TEXT," +
                        "pin TEXT," +
                        "balance INTEGER DEFAULT 0)");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        mainMenu();
    }
}