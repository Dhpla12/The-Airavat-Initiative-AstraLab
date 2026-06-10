package com.airavat.astralab.arvt;

import java.util.List;

public record ProgramNode(String rocketName, List<BlockNode> blocks, List<CommandNode> commands) {
    public ProgramNode {
        blocks = List.copyOf(blocks);
        commands = List.copyOf(commands);
    }
}
