package generators;

import input.GrammarDescription;
import input.TokenRule;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class LexerGenerator {
    private final Path output;
    private final String name;
    private final GrammarDescription grammar;

    public LexerGenerator(final Path output, final String name, final GrammarDescription grammar) {
        this.output = output;
        this.name = name;
        this.grammar = grammar;
    }

    public void generate() throws Exception {
        generateTokens();
        generateLexer();
    }

    private void generateTokens() throws Exception {
        try (final TabbedWriter writer = new TabbedWriter(output.resolve(name + "Tokens.java"))) {
            writer.writeHeader(grammar);
            writer.write(0, "public enum " + name + "Tokens {\n");
            final Set<String> skips = grammar.getSkips().stream()
                    .map(TokenRule::getName)
                    .collect(Collectors.toSet());
            final String tokens = grammar.getTokenRules().stream()
                    .map(TokenRule::getName)
                    .filter(token -> !skips.contains(token))
                    .collect(Collectors.joining(", "));
            writer.write(1, tokens + ", " + GrammarDescription.END + ";\n}\n");
        }
        try (final TabbedWriter writer = new TabbedWriter(output.resolve("Token.java"))) {
            writer.writeHeader(grammar);
            writer.write(0, "import runtime.TokenData;\n\n");
            writer.write(0, "public class Token {\n");
            writer.write(1, "public final " + name + "Tokens type;\n");
            writer.write(1, "public final TokenData data;\n\n");
            writer.write(1, "public Token(final " + name + "Tokens type, final TokenData data) {\n");
            writer.write(2, "this.type = type;\n");
            writer.write(2, "this.data = data;\n");
            writer.write(1, "}\n}\n");
        }
    }

    private void generateLexer() throws Exception {
        final String tokensName = name + "Tokens";
        try (final TabbedWriter writer = new TabbedWriter(output.resolve(name + "Lexer.java"))) {
            writer.writeHeader(grammar);
            writer.write(0, "import input.TokenRule;\n");
            writer.write(0, "import java.util.Scanner;\n");
            writer.write(0, "import java.util.List;\n");
            writer.write(0, "import java.util.ArrayList;\n");
            writer.write(0, "import java.io.IOException;\n");
            writer.write(0, "import runtime.TokenData;\n");
            writer.write(0, "import runtime.LexingException;\n");
            writer.write(0, "import java.nio.file.Path;\n\n");
            writer.write(0, "public class " + name + "Lexer implements AutoCloseable {\n");
            writer.write(1, "private final static Token _END_ = new Token(" + tokensName + "._END, new TokenData(\"\", \"\"));\n");
            writer.write(1, "private final Scanner scanner;\n");
            writer.write(1, "private final List<TokenRule> tokenRules;\n");
            writer.write(1, "private final List<" + tokensName + "> tokenTypes;\n\n");
            writer.write(1, "public " + name + "Lexer(final Path input) throws IOException {\n");

            final List<TokenRule> skips = grammar.getSkips();
            final Set<String> skipNames = skips.stream().map(TokenRule::getName).collect(Collectors.toSet());
            final List<TokenRule> tokens = grammar.getTokenRules().stream()
                    .filter(rule -> !skipNames.contains(rule.getName()))
                    .collect(Collectors.toList());
            final String delimiter = skips.stream()
                    .map(TokenRule::getRegex)
                    .map(regex -> {
                        int len = regex.length();
                        return regex.substring(1, len).substring(0, len - 2);
                    })
                    .map(regex -> "(" + regex + ")")
                    .collect(Collectors.joining("|"));

            writer.write(2, "this.scanner = new Scanner(input).useDelimiter(\"" + delimiter + "\");\n");
            writer.write(2, "this.tokenRules = new ArrayList<>();\n");
            writer.write(2, "this.tokenTypes = new ArrayList<>();\n");
            for (final TokenRule rule : tokens) {
                writer.write(2, "this.tokenRules.add(new TokenRule(");
                writer.write(0, "\"" + rule.getName() + "\", " + rule.getRegex() + "));\n");
                writer.write(2, "this.tokenTypes.add(" + tokensName + "." + rule.getName() + ");\n");
            }
            writer.write(1, "}\n\n");

            writer.write(1, "public void close() throws Exception {\n");
            writer.write(2, "scanner.close();\n");
            writer.write(1, "}\n\n");

            writer.write(1, "public Token getNext() throws LexingException {\n");
            writer.write(2, "if (tokenRules.stream().map(TokenRule::getRegex).noneMatch(scanner::hasNext)) {\n");
            writer.write(3, "if (!scanner.hasNext()) {\n");
            writer.write(4, "return _END_;\n");
            writer.write(3, "} else {\n");
            writer.write(4, "throw new LexingException(\"Unmatched data in input: \\\"\" + scanner.next() + \"\\\"\");\n");
            writer.write(3, "}\n");
            writer.write(2, "} else {\n");
            writer.write(3, "for (int i = 0; i < tokenRules.size(); ++i) {\n");
            writer.write(4, "final TokenRule rule = tokenRules.get(i);\n");
            writer.write(4, "if (scanner.hasNext(rule.getRegex())) {\n");
            writer.write(5, "return new Token(tokenTypes.get(i), new TokenData(rule.getName(), scanner.next(rule.getRegex())));\n");
            writer.write(4, "}\n");
            writer.write(3, "}\n");
            writer.write(2, "}\n");
            writer.write(2, "return null;\n");
            writer.write(1, "}\n");
            writer.write(0, "}\n");
        }
    }
}
