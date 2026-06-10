package com.airavat.astralab.arvt;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int index;
    private int line = 1;
    private int column = 1;

    public Lexer(String source) {
        this.source = source == null ? "" : source;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                add(TokenType.NEWLINE, "\n");
                advanceLine();
            } else if (c == '#') {
                consumeComment();
            } else if (c == '{') {
                add(TokenType.LEFT_BRACE, "{");
                advance();
            } else if (c == '}') {
                add(TokenType.RIGHT_BRACE, "}");
                advance();
            } else if (isNumberStart(c)) {
                tokenizeNumber();
            } else if (isIdentifierStart(c)) {
                tokenizeIdentifier();
            } else {
                advance();
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return List.copyOf(tokens);
    }

    private void tokenizeIdentifier() {
        int start = index;
        int startColumn = column;
        while (!isAtEnd() && isIdentifierPart(peek())) {
            advance();
        }
        tokens.add(new Token(TokenType.IDENTIFIER, source.substring(start, index), line, startColumn));
    }

    private void tokenizeNumber() {
        int start = index;
        int startColumn = column;
        if (peek() == '+' || peek() == '-') {
            advance();
        }
        while (!isAtEnd() && Character.isDigit(peek())) {
            advance();
        }
        if (!isAtEnd() && peek() == '.') {
            advance();
            while (!isAtEnd() && Character.isDigit(peek())) {
                advance();
            }
        }
        if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
            int exponentStart = index;
            advance();
            if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
                advance();
            }
            if (!isAtEnd() && Character.isDigit(peek())) {
                while (!isAtEnd() && Character.isDigit(peek())) {
                    advance();
                }
            } else {
                index = exponentStart;
                column -= 1;
            }
        }
        tokens.add(new Token(TokenType.NUMBER, source.substring(start, index), line, startColumn));
    }

    private void consumeComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private boolean isAtEnd() {
        return index >= source.length();
    }

    private char peek() {
        return source.charAt(index);
    }

    private void advance() {
        index++;
        column++;
    }

    private void advanceLine() {
        index++;
        line++;
        column = 1;
    }

    private void add(TokenType type, String text) {
        tokens.add(new Token(type, text, line, column));
    }

    private static boolean isNumberStart(char c) {
        return Character.isDigit(c) || c == '+' || c == '-';
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '/' || c == '^';
    }
}
