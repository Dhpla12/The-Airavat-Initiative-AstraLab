package com.airavat.astralab.arvt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Parser {
    private final List<Token> tokens;
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public ProgramNode parse() {
        skipNewlines();
        Token rocket = consumeIdentifier("Expected 'rocket' declaration.");
        if (!rocket.text().equalsIgnoreCase("rocket")) {
            throw error(rocket, "ARVT files must begin with 'rocket <Name>'.");
        }
        Token name = consumeIdentifier("Expected rocket name after 'rocket'.");
        skipLine();

        List<BlockNode> blocks = new ArrayList<>();
        List<CommandNode> commands = new ArrayList<>();

        while (!check(TokenType.EOF)) {
            skipNewlines();
            if (check(TokenType.EOF)) {
                break;
            }
            if (check(TokenType.IDENTIFIER) && checkNext(TokenType.LEFT_BRACE)) {
                blocks.add(parseBlock());
            } else if (check(TokenType.IDENTIFIER)) {
                commands.add(parseCommand());
            } else {
                throw error(peek(), "Unexpected token '%s'.".formatted(peek().text()));
            }
        }
        return new ProgramNode(name.text(), blocks, commands);
    }

    private BlockNode parseBlock() {
        Token name = consumeIdentifier("Expected block name.");
        consume(TokenType.LEFT_BRACE, "Expected '{' after block name.");
        skipNewlines();
        Map<String, String> properties = new LinkedHashMap<>();
        while (!check(TokenType.RIGHT_BRACE) && !check(TokenType.EOF)) {
            Token key = consumeIdentifier("Expected property name.");
            String value = collectValue();
            properties.put(key.text().toLowerCase(Locale.ROOT), value);
            skipNewlines();
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' to close %s block.".formatted(name.text()));
        skipLine();
        return new BlockNode(name.text().toLowerCase(Locale.ROOT), properties);
    }

    private CommandNode parseCommand() {
        Token name = consumeIdentifier("Expected command.");
        List<String> args = new ArrayList<>();
        while (!check(TokenType.NEWLINE) && !check(TokenType.EOF)) {
            args.add(advance().text());
        }
        skipLine();
        return new CommandNode(name.text().toLowerCase(Locale.ROOT), args);
    }

    private String collectValue() {
        StringBuilder value = new StringBuilder();
        while (!check(TokenType.NEWLINE) && !check(TokenType.RIGHT_BRACE) && !check(TokenType.EOF)) {
            Token token = advance();
            if (value.length() > 0 && token.type() == TokenType.IDENTIFIER && !looksLikeUnit(token.text())) {
                value.append(' ');
            }
            value.append(token.text());
        }
        return value.toString().trim();
    }

    private void skipLine() {
        while (match(TokenType.NEWLINE)) {
            // keep consuming blank lines
        }
    }

    private void skipNewlines() {
        while (match(TokenType.NEWLINE)) {
            // consume
        }
    }

    private Token consumeIdentifier(String message) {
        return consume(TokenType.IDENTIFIER, message);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (type == TokenType.EOF) {
            return peek().type() == TokenType.EOF;
        }
        return !isAtEnd() && peek().type() == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(current + 1).type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ArvtException error(Token token, String message) {
        return new ArvtException("Line %d, column %d: %s".formatted(token.line(), token.column(), message));
    }

    private static boolean looksLikeUnit(String value) {
        return value.matches("[A-Za-z]+(?:/s|\\^?2|-1)?");
    }
}
