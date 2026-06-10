package com.airavat.astralab.arvt;

import java.util.List;

public record CommandNode(String name, List<String> args) {
    public CommandNode {
        args = List.copyOf(args);
    }

    public String argText() {
        return String.join(" ", args);
    }
}
