import java.sql.*;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CurrencyConverterNew {
    static final String DB_URL = "jdbc:mysql://localhost:3306/currency_db";
    static final String USER = "root";
    static final String PASS = "Simk@123";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println("Connected to the database!");

            // User input for conversion
            System.out.print("Enter base currency (e.g., USD): ");
            String fromCurrency = scanner.nextLine().toUpperCase();

            System.out.print("Enter target currency (e.g., INR): ");
            String toCurrency = scanner.nextLine().toUpperCase();

            System.out.print("Enter amount to convert: ");
            double amount = scanner.nextDouble();

            // Check if the target currency rate exists in the database
            double targetRate = fetchExchangeRate(conn, toCurrency);

            // If not, ask user to enter the rate and store it in the database
            if (targetRate == 0) {
                System.out.print("Enter the exchange rate for " + toCurrency);
                targetRate = scanner.nextDouble();
                insertExchangeRate(conn, toCurrency, targetRate);
            }

            // Perform conversion
            double convertedAmount = amount * targetRate;
            System.out.printf("Converted Amount: %.2f %s%n", convertedAmount, toCurrency);

            // Log conversion in the database
            logConversion(conn, fromCurrency, toCurrency, amount, convertedAmount);
            System.out.println("Conversion logged successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double fetchExchangeRate(Connection conn, String currency) throws SQLException {
        String query = "SELECT exchange_rate FROM exchange_rates WHERE currency_code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currency);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("exchange_rate");
            }
        }
        return 0; // Return 0 if not found
    }

    private static void insertExchangeRate(Connection conn, String currency, double rate) throws SQLException {
        String query = "INSERT INTO exchange_rates (currency_code, exchange_rate) VALUES (?, ?) " +
                       "ON DUPLICATE KEY UPDATE exchange_rate = VALUES(exchange_rate)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currency);
            stmt.setDouble(2, rate);
            stmt.executeUpdate();
        }
    }

    private static void logConversion(Connection conn, String fromCurrency, String toCurrency, double amount, double convertedAmount) throws SQLException {
        String query = "INSERT INTO conversion_logs (from_currency, to_currency, amount, converted_amount, conversion_time) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fromCurrency);
            stmt.setString(2, toCurrency);
            stmt.setDouble(3, amount);
            stmt.setDouble(4, convertedAmount);

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            stmt.setString(5, now.format(formatter));

            stmt.executeUpdate();
        }
    }
}
