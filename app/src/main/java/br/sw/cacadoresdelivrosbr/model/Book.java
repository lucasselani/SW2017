package br.sw.cacadoresdelivrosbr.model;

/**
 * Created by lucasselani on 29/04/17.
 */

public class Book {
    public String bookId;
    public String bookName;
    public String bookImage;
    public String bookDesc;

    public Book(){

    }

    public Book(String bookId, String bookName, String bookImage, String bookDesc) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.bookImage = bookImage;
        this.bookDesc = bookDesc;
    }
}
