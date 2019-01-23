package generators;

import input.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ParserGenerator {
    private final static String THROWS = "ParsingException, LexingException";

    private final Path output;
    private final String name;
    private final GrammarDescription grammar;
    private final String tokensName;
    private final Map<String, String> nonTermToType;

    public ParserGenerator(final Path output, final String name, final GrammarDescription grammar) {
        this.output = output;
        this.name = name;
        this.grammar = grammar;
        this.tokensName = name + "Tokens";
        this.nonTermToType = new HashMap<>();
        for (final ParserRule rule : grammar.getParserRules()) {
            nonTermToType.put(rule.getName(), rule.getReturnValue().getType());
        }
    }

    public void generate() throws Exception {

        try (final TabbedWriter writer = new TabbedWriter(output.resolve(name + "Parser.java"))) {
            writer.writeHeader(grammar);
            writer.write(0, "\n");
            writer.write(0, "import runtime.TokenData;\n");
            writer.write(0, "import runtime.LexingException;\n");
            writer.write(0, "import runtime.ParsingException;\n\n");
            writer.write(0, "public class " + name + "Parser {\n");
            writer.write(1, "private final " + name + "Lexer lexer;\n");
            writer.write(1, "private Token curToken;\n\n");
            writer.write(1, "public " + name + "Parser(final " + name + "Lexer lexer) {\n");
            writer.write(2, "this.lexer = lexer;\n");
            writer.write(2, "this.curToken = null;\n");
            writer.write(1, "}\n\n");
            writer.write(1, "public ");
            final ParserRule mainRule = grammar.getStart();
            final String mainRuleArgs = getArgs(mainRule);
            writer.writeFunctionHead(mainRule.getReturnValue().getType(), "mainRule", mainRuleArgs, THROWS);
            writer.write(2, "curToken = lexer.getNext();\n");
            writer.write(2, "final " + grammar.getStart().getReturnValue().getType() + " ");
            writer.write(0, grammar.getStart().getReturnValue().getName() + " = parse");
            writer.write(0, grammar.getStart().getName() + "(");
            final String mainRuleParams = grammar.getStart().getArguments().stream()
                    .map(ParserRule.RuleArgument::getName)
                    .collect(Collectors.joining(", "));
            writer.write(0, mainRuleParams + ");\n");
            writer.write(2, "if (curToken.type != " + tokensName + "._END) {\n");
            writer.write(3, "throw new ParsingException(\"Expected end of input but found \" + curToken.data.getName());\n");
            writer.write(2, "}\n");
            writer.write(2, "return res;\n");
            writer.write(1, "}\n\n");

            for (final ParserRule rule : grammar.getParserRules()) {
                generateRule(rule, writer);
            }

            writer.write(0, "}\n");
        }
    }

    private String getArgs(final ParserRule rule) {
        return rule.getArguments().stream()
                .map(arg -> arg.getType() + " " + arg.getName())
                .collect(Collectors.joining(", "));
    }

    private void generateRule(final ParserRule rule, final TabbedWriter writer) throws IOException {
        writer.write(1, "private ");
        writer.writeFunctionHead(rule.getReturnValue().getType(), "parse" + rule.getName(), getArgs(rule), THROWS);
        writer.write(2, rule.getReturnValue().getType() + " " + rule.getReturnValue().getName() + " = null;\n");
        writer.write(2, "switch (curToken.type) {");
        final List<Set<String>> allMarkers = new ArrayList<>();
        for (int i = 0; i < rule.getAlternatives().size(); ++i) {
            generateAlternative(rule, i, writer);
            allMarkers.add(grammar.getMarkers(rule, i));
        }
        writer.write(0,"\n");
        writer.write(3, "default: {\n");
        final String wanted = allMarkers.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.joining(", "));
        writer.write(4, "throw new ParsingException(\"Expected " + wanted + " but found \" + curToken.type.name());\n");
        writer.write(3, "}\n");
        writer.write(2, "}\n");
        writer.write(2, "return " + rule.getReturnValue().getName() + ";\n");
        writer.write(1, "}\n\n");

    }

    private void generateAlternative(final ParserRule rule, final int ind, final TabbedWriter writer) throws IOException {
        final RuleAlternative alternative = rule.getAlternatives().get(ind);
        final Set<String> markers = grammar.getMarkers(rule, ind);
        for (final String mark : markers) {
            writer.write(0, "\n");
            writer.write(3, "case " + mark + ":");
        }
        writer.write(0, " {\n");
        writer.write(4, alternative.getInitCode().orElse("") + "\n");
        if (alternative instanceof EpsilonAlternative) {
            final EpsilonAlternative eps = (EpsilonAlternative) alternative;
            writer.write(4, eps.getCode().orElse("") + "\n");
        } else {
            for (final RuleAtom atom : ((AtomAlternative) alternative).getAtoms()) {
                writeAtom(atom, writer);
            }
        }
        writer.write(4, "break;\n");
        writer.write(3, "}");

    }

    private void writeAtom(final RuleAtom atom, final TabbedWriter writer) throws IOException {
        if (atom instanceof Terminal) {
            writer.write(4, "if (curToken.type != " + tokensName + "." + atom.getName() + ") {\n");
            writer.write(5, "throw new LexingException(\"Expected  + " + atom.getName() + " but found \" + curToken.type.name());\n");
            writer.write(4, "}\n");
            writer.write(4, "TokenData " + atom.getVar() + " = curToken.data;\n");
            writer.write(4, atom.getCode().orElse("") + "\n");
            writer.write(4, "curToken = lexer.getNext();\n");
        } else {
            final NonTerminal nonTerm = (NonTerminal) atom;
            writer.write(4, nonTermToType.get(nonTerm.getName()) + " " + nonTerm.getVar() + " = ");
            final String params = String.join(", ", nonTerm.getParams());
            writer.write(0, "parse" + nonTerm.getName() + "(" + params + ");\n");
            writer.write(4, nonTerm.getCode().orElse("") + "\n");
        }
    }
}
