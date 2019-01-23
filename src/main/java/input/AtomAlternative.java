package input;

import java.util.List;
import java.util.Optional;

public class AtomAlternative extends RuleAlternative {
    private final List<RuleAtom> atoms;

    public AtomAlternative(final String javaCode, final List<RuleAtom> atoms) {
        super(javaCode);
        this.atoms = atoms;
    }

    public List<RuleAtom> getAtoms() {
        return atoms;
    }
}
