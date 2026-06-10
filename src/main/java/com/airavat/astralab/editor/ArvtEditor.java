package com.airavat.astralab.editor;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.beans.value.ObservableValue;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArvtEditor extends BorderPane {
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", ArvtKeywords.ALL) + ")\\b";
    private static final String COMMENT_PATTERN = "#[^\\n]*";
    private static final String NUMBER_PATTERN = "\\b[-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?";
    private static final String UNIT_PATTERN = "(?<=\\d)(?:m2|m\\^2|m/s|kg|g|cm|mm|km|s|ms|deg|rad|K|N|kPa|Pa)";
    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<UNIT>" + UNIT_PATTERN + ")"
                    + "|(?<BRACE>[{}])",
            Pattern.CASE_INSENSITIVE);

    private final CodeArea codeArea = new CodeArea();
    private int braceA = -1;
    private int braceB = -1;

    public ArvtEditor() {
        codeArea.getStyleClass().add("code-area");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setWrapText(false);
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(120))
                .subscribe(ignore -> {
                    updateBraceMatch();
                    updateHighlighting();
                });
        codeArea.caretPositionProperty().addListener((obs, old, value) -> {
            updateBraceMatch();
            updateHighlighting();
        });
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        codeArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
        setCenter(new VirtualizedScrollPane<>(codeArea));
    }

    public void setText(String text) {
        codeArea.replaceText(text == null ? "" : text);
        updateBraceMatch();
        updateHighlighting();
    }

    public String getText() {
        return codeArea.getText();
    }

    public ObservableValue<String> textProperty() {
        return codeArea.textProperty();
    }

    public void requestEditorFocus() {
        codeArea.requestFocus();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.TAB) {
            codeArea.replaceSelection("    ");
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.ENTER) {
            String indent = currentLineIndent();
            if (previousNonWhitespaceChar() == '{') {
                indent += "    ";
            }
            codeArea.replaceSelection("\n" + indent);
            event.consume();
        }
    }

    private void handleKeyTyped(KeyEvent event) {
        if ("{".equals(event.getCharacter())) {
            codeArea.replaceSelection("{}");
            codeArea.moveTo(Math.max(0, codeArea.getCaretPosition() - 1));
            event.consume();
        } else if ("(".equals(event.getCharacter())) {
            codeArea.replaceSelection("()");
            codeArea.moveTo(Math.max(0, codeArea.getCaretPosition() - 1));
            event.consume();
        }
    }

    private String currentLineIndent() {
        int caret = codeArea.getCaretPosition();
        String text = codeArea.getText();
        int lineStart = text.lastIndexOf('\n', Math.max(0, caret - 1)) + 1;
        StringBuilder indent = new StringBuilder();
        for (int i = lineStart; i < text.length() && i < caret; i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\t') {
                indent.append(c);
            } else {
                break;
            }
        }
        return indent.toString();
    }

    private char previousNonWhitespaceChar() {
        String text = codeArea.getText();
        for (int i = Math.min(codeArea.getCaretPosition(), text.length()) - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return '\0';
    }

    private void updateBraceMatch() {
        String text = codeArea.getText();
        int caret = codeArea.getCaretPosition();
        braceA = -1;
        braceB = -1;
        int candidate = -1;
        if (caret > 0 && isBrace(text.charAt(caret - 1))) {
            candidate = caret - 1;
        } else if (caret < text.length() && isBrace(text.charAt(caret))) {
            candidate = caret;
        }
        if (candidate >= 0) {
            int match = findMatchingBrace(text, candidate);
            if (match >= 0) {
                braceA = candidate;
                braceB = match;
            }
        }
    }

    private int findMatchingBrace(String text, int position) {
        char open = text.charAt(position);
        char close = open == '{' ? '}' : '{';
        int direction = open == '{' ? 1 : -1;
        int depth = 0;
        for (int i = position; i >= 0 && i < text.length(); i += direction) {
            char c = text.charAt(i);
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isBrace(char c) {
        return c == '{' || c == '}';
    }

    private void updateHighlighting() {
        codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText(), braceA, braceB));
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text, int braceA, int braceB) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKeywordEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            addPlain(spansBuilder, matcher.start() - lastKeywordEnd);
            Collection<String> style = new ArrayList<>();
            if (matcher.group("COMMENT") != null) {
                style.add("comment");
            } else if (matcher.group("KEYWORD") != null) {
                style.add("keyword");
            } else if (matcher.group("NUMBER") != null) {
                style.add("number");
            } else if (matcher.group("UNIT") != null) {
                style.add("unit");
            } else if (matcher.group("BRACE") != null) {
                style.add("brace");
                if (matcher.start() == braceA || matcher.start() == braceB) {
                    style.add("brace-match");
                }
            }
            spansBuilder.add(style, matcher.end() - matcher.start());
            lastKeywordEnd = matcher.end();
        }
        addPlain(spansBuilder, text.length() - lastKeywordEnd);
        return spansBuilder.create();
    }

    private static void addPlain(StyleSpansBuilder<Collection<String>> spansBuilder, int length) {
        if (length > 0) {
            spansBuilder.add(Collections.emptyList(), length);
        }
    }
}
