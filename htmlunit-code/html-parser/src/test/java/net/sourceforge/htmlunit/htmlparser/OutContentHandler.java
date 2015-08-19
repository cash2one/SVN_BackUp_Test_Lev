package net.sourceforge.htmlunit.htmlparser;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

class OutContentHandler implements ContentHandler, XMLDocumentHandler {

    @Override
    public void setDocumentLocator(Locator locator) {
        System.out.println("setDocumentLocator");
    }

    @Override
    public void startDocument() throws SAXException {
        System.out.println("startDocument 1");
    }

    @Override
    public void startDocument(XMLLocator locator, String encoding,
            NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
        System.out.println("startDocument 2");
    }

    @Override
    public void endDocument() throws SAXException {
        System.out.println("endDocument");
    }

    @Override
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        System.out.println("startPrefixMapping");
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        System.out.println("endPrefixMapping");
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < atts.getLength(); i++) {
            builder.append(atts.getURI(i) + ',' + atts.getLocalName(i) + ',' + atts.getQName(i) + ',' + atts.getValue(i) + '-');
        }
        System.out.println("startElement 1: " + uri + ',' + localName + ',' + qName + ", atts: " + builder);
    }

    @Override
    public void startElement(QName qName, XMLAttributes atts, Augmentations augs)
            throws XNIException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < atts.getLength(); i++) {
            builder.append(atts.getURI(i) + ',' + atts.getLocalName(i) + ',' + atts.getQName(i) + ',' + atts.getValue(i) + '-');
        }
        System.out.println("startElement 2: " + qName.uri + ',' + qName.prefix + ',' + qName.localpart + ", atts: " + builder);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        System.out.println("endElement 1: " + uri + ',' + localName + ',' + qName);
    }

    @Override
    public void endElement(QName qName, Augmentations augs) throws XNIException {
        System.out.println("endElement 2: " + qName.uri + ',' + qName.prefix + ',' + qName.localpart);
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        System.out.println("characters 1: " + new String(ch, start, length));
    }

    @Override
    public void characters(XMLString text, Augmentations augs)
            throws XNIException {
        System.out.println("characters 2: " + text);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        System.out.println("ignorableWhitespace");
    }

    @Override
    public void processingInstruction(String target, String data)
            throws SAXException {
        System.out.println("processingInstruction");
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        System.out.println("skippedEntity");
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        System.out.println("comment");
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId,
            Augmentations augs) throws XNIException {
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
            throws XNIException {
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs)
            throws XNIException {
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return null;
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs)
            throws XNIException {
    }

    @Override
    public void processingInstruction(String target, XMLString data,
            Augmentations augs) throws XNIException {
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier,
            String encoding, Augmentations augs) throws XNIException {
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs)
            throws XNIException {
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone,
            Augmentations augs) throws XNIException {
    }

}
