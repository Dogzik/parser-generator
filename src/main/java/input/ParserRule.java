package input;

import java.util.List;

public class ParserRule {
    public static class RuleArgument {
        private final String name;
        private final String type;

        RuleArgument(final String name, final String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    private final String name;
    private final List<RuleArgument> arguments;
    private final RuleArgument returnValue;
    private final List<RuleAlternative> alternatives;

    public ParserRule(final String name, final List<RuleArgument> arguments,
                      final RuleArgument returnValue, final List<RuleAlternative> alternatives) {
        this.name = name;
        this.arguments = arguments;
        this.returnValue = returnValue;
        this.alternatives = alternatives;
    }

    public String getName() {
        return name;
    }

    public List<RuleArgument> getArguments() {
        return arguments;
    }

    public RuleArgument getReturnValue() {
        return returnValue;
    }

    public List<RuleAlternative> getAlternatives() {
        return alternatives;
    }
}
