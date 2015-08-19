package net.sourceforge.htmlunit.htmlparser;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class ValidatorCanonicalTest {

    /** Tests that can be safely ignored (e.g. add missing "body", attribute order, etc. */
    private static List<String> EXPECTED_FAILURES_ = Arrays.asList("test009");

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
    public ValidatorCanonicalTest(final File file) {
        file_ = file;
    }
    
    @Test
    public void test() throws Exception {
        String html = IOUtils.toString(new FileReader(file_));
        StringReader stringReader = new StringReader(html);
        HtmlParser parser = new HtmlParser(XmlViolationPolicy.ALLOW);
        CanonicalContentHandler contentHandler = new CanonicalContentHandler();
        parser.setContentHandler(contentHandler);
        parser.parse(new InputSource(stringReader));
        String actual = contentHandler.builder.toString().trim();
        String testName = file_.getName();
        testName = testName.substring(0, testName.lastIndexOf('.'));
        String canonialName = testName + ".canonical";
        String expected = IOUtils.toString(new FileReader(new File(file_.getParent(), canonialName)));
        expected = expected.replace("\r\n", "\n").trim();
        boolean success = expected.replace(")HEAD", ")HEAD\n(BODY\n)BODY").equals(actual);
        if (!success) {
            success = expected.equals(actual);
        }
        if (EXPECTED_FAILURES_.contains(testName)) {
            if (success) {
                Assert.fail("Test " + file_.getName() + " was expected to fail, but already works");
            }
        }
        else if (!success) {
            Assert.assertEquals(file_.getName(), expected, actual);
        }
    }

    private static class CanonicalContentHandler implements ContentHandler {

        private StringBuilder builder = new StringBuilder();
        private StringBuilder characters = new StringBuilder();
        
        @Override
        public void setDocumentLocator(Locator locator) {
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            flushCharacters();
            builder.append('(').append(qName.toUpperCase()).append('\n');
            for (int i = 0; i < atts.getLength(); i++) {
                builder.append('A').append(atts.getQName(i)).append(' ').append(atts.getValue(i)).append('\n');
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            flushCharacters();
            builder.append(')').append(qName.toUpperCase()).append('\n');
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            characters.append(new String(ch, start, length));
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length)
                throws SAXException {
        }

        @Override
        public void processingInstruction(String target, String data)
                throws SAXException {
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
        }
        
        private void flushCharacters() {
            if (characters.length() != 0) {
                builder.append('"')
                    .append(characters.toString().replaceAll("\n", "\\\\n"))
                    .append('\n');
                characters.setLength(0);
            }
        }
    }
}