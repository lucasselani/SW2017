package br.sw.cacadoresdelivrosbr.model;

/**
 * Created by lucasselani on 29/04/17.
 */

public class Book {
    private String bookId;
    private String bookName;
    private String bookDesc;

    private Book(){ }

    public Book(String bookId, String bookName, String bookDesc) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.bookDesc = bookDesc;
    }

    public String getBookId() {
        return bookId;
    }

    public String getBookName() {
        return bookName;
    }

    public String getBookDesc() {
        return bookDesc;
    }
}
