package input;

import java.util.Optional;

abstract public class RuleAtom {
    private final String var;
    private final String name;
    private final Optional<String> code;

    public RuleAtom(final String var, final String name, final String javaCode) {
        this.var = var;
        this.name = name;
        this.code = Optional.ofNullable(javaCode).map(code -> {
            int codeLen = code.length();
            return code.substring(1, codeLen).substring(0, codeLen - 2);
        });
    }

    public abstract boolean isTerminal();

    public Optional<String> getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getVar() {
        return var;
    }
}
