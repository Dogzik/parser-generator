package generators;

import input.GrammarDescription;
import input.TokenRule;

import java.io.Writer;
import java.nio.file.Path;
import java.util.HashSet;
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
            writer.write(0, "import java.nio.file.Files;\n");
            writer.write(0, "import java.util.Map;\n");
            writer.write(0, "import java.util.HashMap;\n");
            writer.write(0, "import java.util.stream.Collectors;\n");
            writer.write(0, "import java.util.regex.Pattern;\n");
            writer.write(0, "import java.util.regex.Matcher;\n");
            writer.write(0, "import java.util.Arrays;\n");
            writer.write(0, "import java.io.IOException;\n");
            writer.write(0, "import runtime.TokenData;\n");
            writer.write(0, "import runtime.LexingException;\n");
            writer.write(0, "import java.nio.file.Path;\n\n");
            writer.write(0, "public class " + name + "Lexer {\n");
            writer.write(1, "private final static Token _END_ = new Token(" + tokensName + "._END, new TokenData(\"_END\", \"\"));\n");
            writer.write(1, "private final Pattern ignore;\n");
            writer.write(1, "private final Matcher matcher;\n");
            writer.write(1, "private String text;\n");
            writer.write(1, "private final Map<" + tokensName + ", Pattern> tokens;\n\n");
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

            writer.write(2, "this.ignore = Pattern.compile(\"" + delimiter + "\");\n");
            writer.write(2, "this.matcher = Pattern.compile(\"\").matcher(\"\");\n");
            writer.write(2, "this.text = Files.newBufferedReader(input).lines().collect(Collectors.joining());\n");
            writer.write(2, "this.tokens = new HashMap<>();\n");
            for (final TokenRule rule : tokens) {
                writer.write(2, "this.tokens.put(" + tokensName + "." + rule.getName() + ", ");
                writer.write(0, "Pattern.compile(" + rule.getRegex() + "));\n");
            }
            writer.write(1, "}\n\n");

            writer.write(1, "public Token getNext() throws LexingException {\n");
            writer.write(2, "matcher.usePattern(ignore);\n");
            writer.write(2, "matcher.reset(text);\n");
            writer.write(2, "while (matcher.lookingAt()) {\n");
            writer.write(3, "text = text.substring(matcher.end());\n");
            writer.write(3, "matcher.reset(text);\n");
            writer.write(2, "}\n");
            writer.write(2, "final String curText = text;\n");
            writer.write(2, "boolean matched = Arrays.stream(" + tokensName + ".values()).filter(type -> (type != " + tokensName + "._END)).map(tokens::get).anyMatch(regex -> {\n");
            writer.write(3, "matcher.usePattern(regex);\n");
            writer.write(3, "matcher.reset(curText);\n");
            writer.write(3, "return matcher.lookingAt();\n");
            writer.write(2, "});\n");
            writer.write(2, "if (!matched) {\n");
            writer.write(3, "if (text.isEmpty()) {\n");
            writer.write(4, "return _END_;\n");
            writer.write(3, "} else {\n");
            writer.write(4, "throw new LexingException(\"Unmatched data in input: \\\"\" + text + \"\\\"\");\n");
            writer.write(3, "}\n");
            writer.write(2, "} else {\n");
            writer.write(3, "for (final " + tokensName + " type : " + tokensName + ".values()) {\n");
            writer.write(4, "final Pattern regex = tokens.get(type);\n");
            writer.write(4, "matcher.usePattern(regex);\n");
            writer.write(4, "matcher.reset(text);\n");
            writer.write(4, "if (matcher.lookingAt()) {\n");
            writer.write(5, "final String curMatch = text.substring(0, matcher.end());\n");
            writer.write(5, "text = text.substring(curMatch.length());\n");
            writer.write(5, "return new Token(type, new TokenData(type.name(), curMatch));\n");
            writer.write(4, "}\n");
            writer.write(3, "}\n");
            writer.write(2, "}\n");
            writer.write(2, "return null;\n");
            writer.write(1, "}\n");
            writer.write(0, "}\n");
        }
    }
}
