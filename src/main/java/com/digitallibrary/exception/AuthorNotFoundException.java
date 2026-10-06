package com.digitallibrary.exception;

public class AuthorNotFoundException extends RuntimeException {

    public AuthorNotFoundException(String authorId) {
        super("Author not found with id: " + authorId);
    }
}
