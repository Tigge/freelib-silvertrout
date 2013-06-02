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
import java.util.Map;

import silvertrout.Plugin;

public class Freelib extends Plugin {

    private Connection db;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date lastUpdate;
    private String channelName;

    private final int UPDATE_INTERVAL = 60;


    @Override
    public void onLoad(Map<String, String> settings) {
        // Channel
        channelName = settings.get("channel");
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
        if (!getNetwork().isInChannel(channelName)) {
            getNetwork().getConnection().join(channelName);
        }
    }

    @Override
    public void onTick(int ticks) {
        if (ticks % UPDATE_INTERVAL == 0) {
            update();
        }
    }

    private Date getLatestUpdate() {
        Date result = new Date(0);
        try {
            Statement statement = db.createStatement();
            ResultSet rs = statement
                    .executeQuery("SELECT added FROM books ORDER BY added DESC LIMIT 1");
            if (rs.next()) {
                result = dateFormat.parse(rs.getString("added"));
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            // Fallthrough
            e.printStackTrace();
        } catch (ParseException e) {
            // Fallthrough
            e.printStackTrace();
        }
        return result;
    }

    private void print(String s) {
        getNetwork().getChannel(channelName).sendPrivmsg(s);
    }

    private void updateBook(Book book) {
        print(" ⎧ " + book.getTitle() + " - " + book.getAuthor());
        print(" ⎩ Information: " + book.getInfoUrl() + " - Låna: " + book.getLendUrl());
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
                updateBook(Book.fromResultSet(rs));
                count++;
            }
            rs.close();
            statement.close();
            System.out.println("New last: " + lastUpdate + ", count: " + count);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
