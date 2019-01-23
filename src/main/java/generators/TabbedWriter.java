package generators;

import input.GrammarDescription;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TabbedWriter implements AutoCloseable {
    private final static String TAB = "    ";
    private final Writer writer;

    public TabbedWriter(final Path path) throws IOException {
        this.writer = Files.newBufferedWriter(path);
    }

    public void writeTabs(int cnt) throws IOException {
        for (int i = 0; i < cnt; ++i) {
            writer.write(TAB);
        }
    }

    public void write(int tabs, final String s) throws IOException {
        writeTabs(tabs);
        writer.write(s);
    }

    public void writeHeader(final GrammarDescription grammar) throws IOException {
        String header = grammar.getHeader();
        header = header.substring(1);
        header = header.substring(0, header.length() - 1);
        writer.write(header + "\n");
    }

    public void writeFunctionHead(final String retType, final String name, final String args, final String except) throws IOException {
        write(0, retType + " " + name + "(" + args + ")");
        if (!except.isEmpty()) {
            write(0, " throws " + except);
        }
        write(0, " {\n");
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
