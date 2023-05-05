package utils.servlets;
import utils.Utils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/updatePlayerStats")
public class UpdatePlayerStatsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Get the characterId and requested stat from the request parameters
        int characterId = Integer.parseInt(request.getParameter("characterId"));
        String stat = request.getParameter("stat");

        // Update the player's stat by adding 1 to its current value

        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("UPDATE GameCharacters ");
        updateQuery.append("SET defaultStats = JSON_SET(defaultStats, '$.").append(stat).append("', JSON_EXTRACT(defaultStats, '$.").append(stat).append("') + 1) ");
        updateQuery.append("WHERE id = ?");

        try {
            // Get a connection to the database
            Connection conn = DriverManager.getConnection(Utils.DB_URL,
                    Utils.DB_USERNAME, Utils.DB_PASSWORD);

            // Prepare the update statement with the characterId parameter
            PreparedStatement statement = conn.prepareStatement(updateQuery.toString());
            statement.setInt(1, characterId);

            // Execute the update statement
            int rowsAffected = statement.executeUpdate();

            // Check if any rows were affected by the update
            if (rowsAffected > 0) {
                response.getWriter().write("Player stat updated successfully"); //TODO: better Response to Dvir.
            } else {
                response.getWriter().write("No player found with characterId " + characterId); //TODO: better Response to Dvir.
            }

            // Close the database connection and statement
            statement.close();
            conn.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
