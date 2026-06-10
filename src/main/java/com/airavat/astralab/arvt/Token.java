package com.airavat.astralab.arvt;

public record Token(TokenType type, String text, int line, int column) {
}
