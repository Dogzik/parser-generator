package input;

import java.util.regex.Pattern;

public class TokenRule {
    private final String name;
    private final String regex;

    public TokenRule(final String name, final String regex) {
        this.name = name;
        this.regex = regex;
    }

    public String getName() {
        return name;
    }

    public String getRegex() {
        return regex;
    }
}
