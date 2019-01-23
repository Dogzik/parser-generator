import generators.LexerGenerator;
import generators.ParserGenerator;
import input.GrammarDescription;
import input.InputLexer;
import input.InputParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Generator {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Expected 2 args: <grammar> <output_directory>");
            return;
        }

        Path input = null;
        Path output = null;
        InputLexer lexer = null;
        try {
            input = Paths.get(args[0]);
            output = Paths.get(args[1]);
            lexer = new InputLexer(CharStreams.fromPath(input));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        InputParser parser = new InputParser(new CommonTokenStream(lexer));
        final GrammarDescription grammar = parser.grammarDescription().descr;
        if (grammar.getError() != null) {
            System.err.println(grammar.getError().getMessage());
            return;
        }
        try {
            Files.createDirectories(output);
            final String name = input.getName(input.getNameCount() - 1).toString();
            final LexerGenerator lexerGenerator = new LexerGenerator(output, name, grammar);
            lexerGenerator.generate();
            final ParserGenerator parserGenerator = new ParserGenerator(output, name, grammar);
            parserGenerator.generate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
