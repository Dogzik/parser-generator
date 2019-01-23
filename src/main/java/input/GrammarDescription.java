package input;


import javax.swing.text.html.parser.Parser;
import java.util.*;
import java.util.stream.Collectors;

public class GrammarDescription {
    private String header;
    private List<TokenRule> tokenRules;
    private List<TokenRule> skips;
    private ParserRule start;
    private List<ParserRule> parserRules;

    private Map<String, Set<String>> first;
    private Map<String, Set<String>> follow;

    private final static String EPS = "EPS";
    public final static String END = "_END";

    private Exception error;

    public GrammarDescription(final String header, final List<TokenRule> tokenRules, final List<String> skipsNames,
                              final String start, final List<ParserRule> parserRules) {
        try {
            this.header = header;
            this.tokenRules = tokenRules;
            final Map<String, TokenRule> nameToTokenRule = new HashMap<>();
            for (final TokenRule rule : tokenRules) {
                if (nameToTokenRule.putIfAbsent(rule.getName(), rule) != null) {
                    throw new Exception("Duplicate lexer rules");
                }
            }
            if (skipsNames.stream().distinct().count() != skipsNames.size()) {
                throw new Exception("Duplicate tokens in ignore list");
            }
            this.skips = new ArrayList<>();
            for (final String name : skipsNames) {
                final TokenRule skip = nameToTokenRule.getOrDefault(name, null);
                if (skip == null) {
                    throw new Exception("Unknown ignore rule");
                }
                skips.add(skip);
            }
            final Set<String> nonTerminals = getNonTerminals(parserRules);
            if (!nonTerminals.contains(start)) {
                throw new Exception("Non rule for start non-terminal");
            }
            for (final ParserRule rule : parserRules) {
                if (rule.getName().equals(start)) {
                    this.start = rule;
                    break;
                }
            }
            if (!validateParserRules(nameToTokenRule.keySet(), nonTerminals, parserRules)) {
                throw new Exception("Rules contain unknown terminals or non-terminals");
            }
            this.parserRules = parserRules;

            this.first = new HashMap<>();
            countFirst();

            this.follow = new HashMap<>();
            countFollow();

            if (!checkLL1()) {
                throw new Exception("Not LL1 grammar");
            }
            this.error = null;
        } catch (Exception e) {
            this.error = e;
        }
    }

    private Set<String> getNonTerminals(final List<ParserRule> parserRules) {
        return parserRules.stream()
                .map(ParserRule::getAlternatives)
                .flatMap(Collection::stream)
                .filter(alternative -> alternative instanceof AtomAlternative)
                .map(alternative -> (AtomAlternative) alternative)
                .map(AtomAlternative::getAtoms)
                .flatMap(Collection::stream)
                .filter(ruleAtom -> !ruleAtom.isTerminal())
                .map(RuleAtom::getName)
                .collect(Collectors.toSet());
    }

    private boolean validateParserRules(final Set<String> terminals, final Set<String> nonTerminals,
                                        final List<ParserRule> parserRules) {
        return parserRules.stream()
                .map(ParserRule::getAlternatives)
                .flatMap(Collection::stream)
                .filter(alternative -> alternative instanceof AtomAlternative)
                .map(alternative -> (AtomAlternative) alternative)
                .map(AtomAlternative::getAtoms)
                .flatMap(Collection::stream)
                .allMatch(ruleAtom -> {
                    if (ruleAtom.isTerminal()) {
                        return terminals.contains(ruleAtom.getName());
                    } else {
                        return nonTerminals.contains(ruleAtom.getName());
                    }
                });
    }

    private Set<String> getFirstForAtoms(final List<RuleAtom> atoms) {
        if (atoms.isEmpty()) {
            return Collections.singleton(EPS);
        }
        final RuleAtom begin = atoms.get(0);
        if (begin.isTerminal()) {
            return Collections.singleton(begin.getName());
        } else {
            first.putIfAbsent(begin.getName(), new HashSet<>());
            final Set<String> res = first.get(begin.getName());
            if (res.contains(EPS)) {
                res.remove(EPS);
                res.addAll(getFirstForAtoms(atoms.subList(1, atoms.size())));
            }
            return res;
        }
    }

    private void countFirst() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (final ParserRule rule : parserRules) {
                for (final RuleAlternative alternative : rule.getAlternatives()) {
                    first.putIfAbsent(rule.getName(), new HashSet<>());
                    final Set<String> prevSet = first.get(rule.getName());
                    final int prevSize = prevSet.size();
                    prevSet.addAll(getFirst(alternative));
                    if (prevSet.size() != prevSize) {
                        changed = true;
                    }
                }
            }
        }
    }

    private void countFollow() {
        follow.put(start.getName(), new HashSet<>());
        follow.get(start.getName()).add(END);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (final ParserRule rule : parserRules) {
                for (final RuleAlternative alternative : rule.getAlternatives()) {
                    if (alternative instanceof EpsilonAlternative) {
                        continue;
                    }
                    final List<RuleAtom> atoms = ((AtomAlternative) alternative).getAtoms();
                    for (int i = 0; i < atoms.size(); ++i) {
                        if (atoms.get(i).isTerminal()) {
                            continue;
                        }
                        final Set<String> tmp = getFirstForAtoms(atoms.subList(i + 1, atoms.size()));
                        boolean hadEps = tmp.contains(EPS);
                        follow.putIfAbsent(atoms.get(i).getName(), new HashSet<>());
                        final Set<String> prevSet = follow.get(atoms.get(i).getName());
                        int prevSize = prevSet.size();
                        prevSet.addAll(tmp);
                        prevSet.remove(EPS);
                        if (hadEps) {
                            follow.putIfAbsent(rule.getName(), new HashSet<>());
                            final Set<String> tmp2 = follow.get(rule.getName());
                            prevSet.addAll(tmp2);
                        }
                        if (prevSet.size() != prevSize) {
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    private boolean checkRule(final ParserRule rule) {
        for (int i = 0; i < rule.getAlternatives().size(); ++i) {
            for (int j = 0; j < rule.getAlternatives().size(); ++j) {
                if (i == j) {
                    continue;
                }
                final Set<String> firstA = getFirst(rule.getAlternatives().get(i));
                final Set<String> firstB = getFirst(rule.getAlternatives().get(j));
                boolean intersect = firstB.stream().anyMatch(firstA::contains);
                if (intersect) {
                    return false;
                }
                if (firstA.contains(EPS)) {
                    intersect = follow.get(rule.getName()).stream().anyMatch(firstA::contains);
                    if (intersect) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean checkLL1() {
        return parserRules.stream().allMatch(this::checkRule);
    }

    private Set<String> getFirst(final RuleAlternative alternative) {
        if (alternative instanceof EpsilonAlternative) {
            return Collections.singleton(EPS);
        }
        return getFirstForAtoms(((AtomAlternative) alternative).getAtoms());
    }

    public Set<String> getMarkers(final ParserRule rule, int altInd) {
        final Set<String> res = new HashSet<>(getFirst(rule.getAlternatives().get(altInd)));
        if (res.contains(EPS)) {
            res.addAll(follow.get(rule.getName()));
        }
        res.remove(EPS);
        return res;
    }

    public Exception getError() {
        return error;
    }

    public String getHeader() {
        return header;
    }

    public List<TokenRule> getTokenRules() {
        return tokenRules;
    }

    public List<TokenRule> getSkips() {
        return skips;
    }

    public List<ParserRule> getParserRules() {
        return parserRules;
    }
    public ParserRule getStart() {
        return start;
    }
}
