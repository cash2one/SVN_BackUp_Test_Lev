package net.sourceforge.htmlunit.htmlparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NekoCanonicalTest {

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> collection = new ArrayList<Object[]>();
        File dir = new File("./src/test/resources");
        for (final File file : dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        })) {
            collection.add(new Object[] { file });
        }
        return collection;
    }

    private File file_;
    public NekoCanonicalTest(final File file) {
        file_ = file;
    }

    @Test
    public void test() throws Exception {
        StringWriter out = new StringWriter();
        XMLDocumentFilter[] filters = { new Writer(out) };
        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        parser.parse(new XMLInputSource(null, file_.toString(), null));
        final BufferedReader reader = new BufferedReader(new StringReader(out.toString()));
        final StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        String actual = sb.toString().trim();
        String canonialName = file_.getName();
        canonialName = canonialName.substring(0, canonialName.lastIndexOf('.')) + ".canonical";
        String expected = IOUtils.toString(new FileReader(new File(file_.getParent(), canonialName)));
        expected = expected.replace("\r\n", "\n").trim();
        Assert.assertEquals(file_.getName(), expected, actual);
    }
}