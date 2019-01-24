import calc.CalcLexer;
import calc.CalcParser;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Calc {
    public static void main(String[] args) {
        Path input = Paths.get(args[0]);
        try {
            CalcLexer lexer = new CalcLexer(input);
            CalcParser parser = new CalcParser(lexer);
            System.out.println(parser.mainRule());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
