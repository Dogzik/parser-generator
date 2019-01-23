grammar Input;

@header {
import java.util.LinkedList;
import java.util.Collections;
}

grammarDescription returns [GrammarDescription descr]
    : hI=headerInfo lI=lexerInfo s=skip sI=startInfo pI=parserInfo
    {
        $descr = new GrammarDescription($hI.header, $lI.tokenRules, $s.skipNames, $sI.start, $pI.parserRules);
    };

headerInfo returns [String header]: HEADER code=CODE {$header = $code.text;};

lexerInfo returns [List<TokenRule> tokenRules]
    : TOKENS '[' tokens=tokenList ']'
    {
        $tokenRules = $tokens.tokens;
    };

tokenList returns [List<TokenRule> tokens]
    : head=singleToken tail=tokenList
    {
        $tokens = $tail.tokens;
        $tokens.add(0, $head.token);
    }
    | {$tokens = new LinkedList<TokenRule>();}
    ;

singleToken returns [TokenRule token]: id=IDENTIFIER ':=' regex=REGEX ';'
{
    $token = new TokenRule($id.text, $regex.text);
};

skipList returns [List<String> skipNames]
    : head=IDENTIFIER tail=skipList
    {
        $skipNames = $tail.skipNames;
        $skipNames.add(0, $head.text);
    }
    | {$skipNames = new LinkedList<String>();}
    ;

skip returns [List<String> skipNames]: IGNORE '[' skips=skipList ']' {$skipNames = $skips.skipNames;};

terminal returns [RuleAtom atom]: var=IDENTIFIER '=' id=IDENTIFIER (code=CODE)?
{
    $atom = new Terminal($var.text, $id.text, $code.text);
};

nonTerminal returns [RuleAtom atom]: var=IDENTIFIER '=' id=IDENTIFIER '(' params=paramList ')' (code=CODE)?
{
    $atom = new NonTerminal($var.text, $id.text, $params.params, $code.text);
};

ruleAtom returns [RuleAtom atom]
    : resT=terminal {$atom = $resT.atom;}
    | resN=nonTerminal {$atom = $resN.atom;}
    ;

ruleAtomList returns [List<RuleAtom> atoms]
    : head=ruleAtom tail=ruleAtomListTail
    {
        $atoms = $tail.atoms;
        $atoms.add(0, $head.atom);
    }
    ;

ruleAtomListTail returns [List<RuleAtom> atoms]
    : head=ruleAtom tail=ruleAtomListTail
    {
        $atoms = $tail.atoms;
        $atoms.add(0, $head.atom);
    }
    | {$atoms = new LinkedList<RuleAtom>();}
    ;

paramList returns [List<String> params]
    : head=IDENTIFIER tail=paramListTail
    {
        $params = $tail.params;
        $params.add(0, $head.text);
    }
    | {$params = new LinkedList<String>();}
    ;

paramListTail returns [List<String> params]
    : ',' tail=paramList {$params = $tail.params;}
    | {$params = new LinkedList<String>();}
    ;

argument returns [ParserRule.RuleArgument arg]:  name=IDENTIFIER ':' type=IDENTIFIER
{
    $arg = new ParserRule.RuleArgument($name.text, $type.text);
};

argumentList returns [List<ParserRule.RuleArgument> args]
    : head=argument tail=argumentListTail
    {
        $args = $tail.args;
        $args.add(0, $head.arg);
    }
    | { $args = new LinkedList<ParserRule.RuleArgument>(); }
    ;

argumentListTail returns [List<ParserRule.RuleArgument> args]
    : ',' tail=argumentList {$args = $tail.args;}
    | { $args = new LinkedList<ParserRule.RuleArgument>(); }
    ;

alternative returns [RuleAlternative alt]
    : (init=CODE)? EPS (code=CODE)?
    {
        $alt = new EpsilonAlternative($init.text, $code.text);
    }
    | init=CODE? atoms=ruleAtomList
    {
        $alt = new AtomAlternative($init.text, $atoms.atoms);
    }
    ;

alternativeList returns [List<RuleAlternative> alts]
    : head=alternative tail=alternativeListTail
    {
        $alts = $tail.alts;
        $alts.add(0, $head.alt);
    }
    ;

alternativeListTail returns [List<RuleAlternative> alts]
    : '|' tail=alternativeList {$alts = $tail.alts;}
    | {$alts = new LinkedList<RuleAlternative>();}
    ;

singleRule returns [ParserRule rule]
    : name=IDENTIFIER '(' args=argumentList ')' '->' ret=argument ':=' alts=alternativeList ';'
    {
        $rule = new ParserRule($name.text, $args.args, $ret.arg, $alts.alts);
    };

ruleList returns [List<ParserRule> rules]
    : head=singleRule tail=ruleList
    {
        $rules = $tail.rules;
        $rules.add(0, $head.rule);
    }
    | {$rules = new LinkedList<ParserRule>();}
    ;

parserInfo returns [List<ParserRule> parserRules]
    : RULES '[' ruleList ']' {$parserRules = $ruleList.rules;};

startInfo returns [String start]: START '=' name=IDENTIFIER ';' {$start = $name.text;};

WHITESPACE:     [ \t\r\n]+ -> skip;
EPS:            'EPS';
HEADER:         'header';
IGNORE:         'ignore';
TOKENS:         'tokens';
RULES:          'rules';
START:          'start';
IDENTIFIER:     [a-zA-z][a-zA-z0-9_]*;
CODE :          '{' (~[{}]+ CODE?)* '}';
REGEX:          '"'(~["])+'"';
