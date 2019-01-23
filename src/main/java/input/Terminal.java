package input;

import java.util.List;

public class Terminal extends RuleAtom {
    public Terminal(final String var, final String name, final String javaCode) {
        super(var, name, javaCode);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}
