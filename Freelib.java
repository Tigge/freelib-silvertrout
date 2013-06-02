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
import java.util.List;
import java.util.Map;

import silvertrout.Channel;
import silvertrout.Plugin;
import silvertrout.User;
import silvertrout.commons.CommandLine;

public class Freelib extends Plugin {

    private Connection db;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date lastUpdate;
    private String channelName;

    private static final int SEARCH_LIMIT = 5;
    private static final int UPDATE_INTERVAL = 60;

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

    @Override
    public void onPrivmsg(User user, Channel channel, String message) {
        if (channel.getName().equals(channelName)) {
            CommandLine command = new CommandLine(message);
            if (command.getCommand().equals("search")) {
                List<Book> books = search(command.getParam("for"));
            }

        }
    }

    private List<Book> search(String param) {

        try {
            String searchParam = "%" + param + "%";
            PreparedStatement statement = db
                    .prepareStatement("SELECT * FROM books WHERE isbn LIKE ? OR isbn13 LIKE ? OR title LIKE ? OR author LIKE ?");
            for (int i = 1; i <= 4; i++) {
                statement.setString(i, searchParam);
            }

            ResultSet rs = statement.executeQuery();
            int count = 1;
            for (; rs.next(); count++) {
                if (count <= SEARCH_LIMIT) {
                    Book book = Book.fromResultSet(rs);
                    print("" + count + ". " + book.getTitle() + " - " + book.getAuthor());
                }
            }

            if (count > SEARCH_LIMIT) {
                print("Showing only " + SEARCH_LIMIT + " of " + count + " results");
            } else if (count == 1) {
                print("No results");
            }

            rs.close();
            statement.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
