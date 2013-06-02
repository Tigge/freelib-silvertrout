package silvertrout.plugins.freelib;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import silvertrout.Plugin;

public class Freelib extends Plugin {

    private Connection db;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Date lastUpdate;
    private String channel;

    private final int UPDATE_INTERVAL = 60 * 5;

    private final String INFO_URL = "http://elib.se/library/ebook_detail.asp?id_type=ISBN&id={isbn}&lib=40";
    private final String LEND_URL = "https://www.elib.se/library/login.asp?post=L%C3%A5na+boken&lib=40&id={isbn}&id_type=ISBN";

    @Override
    public void onLoad(Map<String, String> settings) {
        // Channel
        channel = settings.get("channel");
        // Database
        String filename = settings.get("database");
        try {
            Class.forName("org.sqlite.JDBC");
            db = DriverManager.getConnection("jdbc:sqlite:" + filename);
            lastUpdate = getLatestUpdate();
            System.out.println("Last update: " + lastUpdate);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected() {
        // Join channel
        if(!getNetwork().isInChannel(channel)) {
            getNetwork().getConnection().join(channel);
        }
    }

    @Override
    public void onTick(int ticks) {
        if (ticks % UPDATE_INTERVAL == 0) {
            update();
        }
    }

    private Date getLatestUpdate() {
        try {
            Statement statement = db.createStatement();
            ResultSet rs = statement
                    .executeQuery("SELECT added FROM books ORDER BY added DESC LIMIT 1");

            if (rs.next()) {
                return dateFormat.parse(rs.getString("added"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Fallthrough
        } catch (ParseException e) {
            // Fallthrough
            e.printStackTrace();
        }
        return new Date(0);
    }

    private void updateBook(String isbn, String isbn13, String title, String author,
            String category, String publisher, java.sql.Date publishDate, String language,
            String format, String description, String cover) {
        System.out.println();

        String lendUrl = LEND_URL.replace("{isbn}", isbn);
        String infoUrl = INFO_URL.replace("{isbn}", isbn);

        getNetwork().getChannel(channel).sendPrivmsg(
                title + " - " + author + ": info " + infoUrl + " låna " + lendUrl);
    }

    private void update() {
        try {
            PreparedStatement statement = db
                    .prepareStatement("SELECT * FROM books WHERE added > ?");
            statement.setString(1, dateFormat.format(lastUpdate));
            ResultSet rs = statement.executeQuery();
            int count = 0;

            while (rs.next()) {

                Date added = dateFormat.parse(rs.getString("added"));
                if (added.after(lastUpdate)) {
                    lastUpdate = added;
                }

                updateBook(rs.getString("isbn"), rs.getString("isbn13"), rs.getString("title"),
                        rs.getString("author"), rs.getString("category"),
                        rs.getString("publisher"), rs.getDate("publish_date"),
                        rs.getString("language"), rs.getString("format"),
                        rs.getString("description"), rs.getString("cover"));
                count++;
            }
            System.out.println("New last: " + lastUpdate + ", count: " + count);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
