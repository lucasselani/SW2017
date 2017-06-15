package br.sw.cacadoresdelivrosbr.model;

import android.support.annotation.Keep;

/**
 * Created by lucasselani on 29/04/17.
 */

@Keep
public class Book {
    private String bookId;
    private String bookName;
    private String bookDesc;

    public Book(){ }

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

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public void setBookDesc(String bookDesc) {
        this.bookDesc = bookDesc;
    }
}
