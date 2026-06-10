package com.airavat.astralab.arvt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArvtInterpreterTest {
    private static final String SOURCE = """
            rocket ParserRocket

            nosecone {
                shape ogive
                length 25cm
                mass 80g
            }

            body {
                length 1.2m
                diameter 0.08m
                mass 0.35kg
            }

            motor {
                thrust_curve E12
            }

            simulate
            graph altitude
            """;

    @Test
    void parsesRocketBlocksAndCommands() {
        ProgramNode program = new ArvtInterpreter().parse(SOURCE);

        assertEquals("ParserRocket", program.rocketName());
        assertEquals(3, program.blocks().size());
        assertEquals(2, program.commands().size());
    }

    @Test
    void executeProducesRocketAndSimulation() {
        ExecutionResult result = new ArvtInterpreter().execute(SOURCE);

        assertEquals("ParserRocket", result.rocket().name());
        assertTrue(result.simulationResult().isPresent());
        assertTrue(result.requestedGraphs().contains("altitude"));
    }
}
