package input;

import javax.swing.text.html.Option;
import java.util.Optional;

public class EpsilonAlternative extends RuleAlternative{
    final private Optional<String> code;

    public EpsilonAlternative(final String initCode, final String javaCode) {
        super(initCode);
        this.code = Optional.ofNullable(javaCode).map(code -> {
            int codeLen = code.length();
            return code.substring(1, codeLen).substring(0, codeLen - 2);
        });
    }

    public Optional<String> getCode() {
        return code;
    }
}
