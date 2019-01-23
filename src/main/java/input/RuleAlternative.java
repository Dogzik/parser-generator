package input;

import jdk.nashorn.internal.runtime.options.Option;

import java.util.Optional;

public abstract class RuleAlternative {
    final Optional<String> initCode;

    public RuleAlternative(final String initCode) {
        this.initCode = Optional.ofNullable(initCode).map(code -> {
            int codeLen = code.length();
            return code.substring(1, codeLen).substring(0, codeLen - 2);
        });
    }

    public Optional<String> getInitCode() {
        return initCode;
    }
}
