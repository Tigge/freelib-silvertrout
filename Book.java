package silvertrout.plugins.freelib;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class Book {

    private String isbn;
    private String isbn13;
    private String title;
    private String author;
    private String category;
    private String publisher;
    private Date publishDate;
    private String language;
    private String format;
    private String description;
    private String cover;

    private final String INFO_URL = "http://elib.se/library/ebook_detail.asp?id_type=ISBN&id={isbn}&lib=40";
    private final String LEND_URL = "https://www.elib.se/library/login.asp?post=L%C3%A5na+boken&lib=40&id={isbn}&id_type=ISBN";

    public String getISBN() {
        return isbn;
    }

    public String getISBN13() {
        return isbn13;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getInfoUrl() {
        return INFO_URL.replace("{isbn}", getISBN());
    }

    public String getLendUrl() {
        return LEND_URL.replace("{isbn}", getISBN());
    }

    static Book fromResultSet(ResultSet rs) {
        try {
            Book book = new Book();
            book.isbn = rs.getString("isbn");
            book.isbn13 = rs.getString("isbn13");
            book.title = rs.getString("title");
            book.author = rs.getString("author");
            book.category = rs.getString("category");
            book.publisher = rs.getString("publisher");
            book.publishDate = rs.getDate("publish_date");
            book.language = rs.getString("language");
            book.format = rs.getString("format");
            book.description = rs.getString("description");
            book.cover = rs.getString("cover");
            return book;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

    }
}
