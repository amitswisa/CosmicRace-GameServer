package utils.singletons;

import addons.Character;
import utils.GlobalSettings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBHandler {

    public static void updateStatsInDB(Character character) {
        updateUsersCharactersDataTable(character);
        updateGameUsersTable(character);
    }

    private static void updateUsersCharactersDataTable(Character character) {
        //TODO
    }


    private static void updateGameUsersTable(Character character) {
        StringBuilder sqlQuery = new StringBuilder();
        sqlQuery.append("UPDATE GameUsers").append('\n');
        sqlQuery.append("SET coinsAmount = ")/*.append(character.getCoins())*/.append('\n');
        sqlQuery.append("WHERE id = ").append(character.getCharacterID());

        String answer = executeQuery(sqlQuery.toString());
    }

    private static String executeQuery(String sqlQuery) {

        String answer = null;
        try {
            // Get a connection to the database
            Connection conn = DriverManager.getConnection(GlobalSettings.DB_URL,
                    GlobalSettings.DB_USERNAME, GlobalSettings.DB_PASSWORD);

            // Prepare the update statement with the characterId parameter
            PreparedStatement statement = conn.prepareStatement(sqlQuery);

            // Execute the update statement
            int rowsAffected = statement.executeUpdate();

            // Check if any rows were affected by the update
            if (rowsAffected > 0) {
                answer = "Player stat updated successfully"; //TODO: better Response to Dvir.
            } else {
                answer = "Something Went wrong..."; //TODO: better Response to Dvir.
            }

            // Close the database connection and statement
            statement.close();
            conn.close();

            return answer;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
