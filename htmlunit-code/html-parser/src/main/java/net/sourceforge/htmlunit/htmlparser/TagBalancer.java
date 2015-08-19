package net.sourceforge.htmlunit.htmlparser;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class TagBalancer implements ContentHandler {

    static final HTMLElement[] EMPTY_ELEMENTS = new HTMLElement[0];

    private ContentHandler contentHandler_;

    private boolean isHeadCreated;
    private boolean isBodyCreated;

    private List<Element> stack_ = new ArrayList<Element>();

    public void setContentHandler(ContentHandler handler) {
        contentHandler_ = handler;
    }

    public ContentHandler getContentHandler() {
        return contentHandler_;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        if (contentHandler_ != null) {
            contentHandler_.setDocumentLocator(locator);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        if (contentHandler_ != null) {
            contentHandler_.startDocument();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        for (int i = stack_.size() - 1; i >= 0; i--) {
            endElement("", "", stack_.get(i).getQName());
        }
        if (contentHandler_ != null) {
            contentHandler_.endDocument();
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        if (contentHandler_ != null) {
            contentHandler_.startPrefixMapping(prefix, uri);
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (contentHandler_ != null) {
            contentHandler_.endPrefixMapping(prefix);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        HTMLElement e = HTMLElement.getElement(qName);
        if (e == HTMLElement.HEAD) {
            isHeadCreated = true;
        }
        else if (e == HTMLElement.BODY) {
            isBodyCreated = true;
        }
        if (e == HTMLElement.BODY && !isHeadCreated) {
            startElement(uri, "", HTMLElement.HEAD.name(), new AttributesImpl());
        }
        if (e.getParent() != null) {
            boolean found = false;
            for (Element parent : stack_) {
                if (parent.getQName().equals(e.getParent().name())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                startElement(uri, "", e.getParent().name(), new AttributesImpl());
            }
        }
        for (HTMLElement c : e.getCloses()) {
            for (int i = stack_.size() -1; i >= 0; i--) {
                Element current  = stack_.get(i);
                if (current.getQName().equals(c.name())) {
                    endElement(uri, "", c.name());
                }
            }
        }
        if (contentHandler_ != null) {
            contentHandler_.startElement(uri, localName, qName, atts);
        }

        stack_.add(new Element(uri, localName, qName, atts));
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        endElement(uri, localName, qName, true);
    }

    private void endElement(String uri, String localName, String qName, boolean deleteOtherInline)
            throws SAXException {
        HTMLElement e = HTMLElement.getElement(qName);

        int parentStackIndex = -1;
        for (int i = stack_.size() - 1; i >= 0; i--) {
            if (stack_.get(i).getQName().equals(e.name())) {
                parentStackIndex = i;
                break;
            }
        }
        
        
        List<Element> inlineList = null;
        if (e.isInline() && deleteOtherInline) {
            inlineList = new ArrayList<Element>();
            for (int i = stack_.size() - 1; i > parentStackIndex; i--) {
                Element el = stack_.get(i);
                if (HTMLElement.getElement(el.getQName()).isInline()) {
                    inlineList.add(el);
                    endElement(uri, "", el.getQName(), false);
                }
            }
        }

        if (contentHandler_ != null) {
            contentHandler_.endElement(uri, localName, qName);
        }
        if (inlineList != null) {
            for (int i = inlineList.size() - 1; i >= 0; i--) {
                startElement(uri, "", inlineList.get(i).getQName(), inlineList.get(i).getAttributes());
            }
        }
        if (parentStackIndex != -1) {
            stack_.remove(parentStackIndex);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (!isHeadCreated && !isBodyCreated) {
            startElement("", "", HTMLElement.BODY.name(), new AttributesImpl());
        }
        if (contentHandler_ != null) {
            contentHandler_.characters(ch, start, length);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        if (contentHandler_ != null) {
            contentHandler_.ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(String target, String data)
            throws SAXException {
        if (contentHandler_ != null) {
            contentHandler_.processingInstruction(target, data);
        }
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        if (contentHandler_ != null) {
            contentHandler_.skippedEntity(name);
        }
    }
}
