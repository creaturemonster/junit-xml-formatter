package io.cucumber.junitxmlformatter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.Character.offsetByCodePoints;

class EscapingXmlStreamWriter {
    // refactor: add a constructor that takes a XMLStreamWriter
    // reason: the class should be able to write to a XMLStreamWriter
    public EscapingXmlStreamWriter(XMLStreamWriter reportWriter) {
        this.reportWriter = Objects.requireNonNull(reportWriter);
    }

    private final XMLStreamWriter reportWriter;

    void writeStartDocument(String encoding, String version) throws XMLStreamException {
        reportWriter.writeStartDocument(encoding, version);
    }

    void writeNewLine() throws XMLStreamException {
        reportWriter.writeCharacters("\n");
    }

    void writeStartElement(String localName) throws XMLStreamException {
        reportWriter.writeStartElement(localName);
    }

    void writeEndElement() throws XMLStreamException {
        reportWriter.writeEndElement();
    }

    void writeEndDocument() throws XMLStreamException {
        reportWriter.writeEndDocument();
    }

    void flush() throws XMLStreamException {
        reportWriter.flush();
    }

    void writeEmptyElement(String localName) throws XMLStreamException {
        reportWriter.writeEmptyElement(localName);
    }

    void writeAttribute(String localName, String value) throws XMLStreamException {
        reportWriter.writeAttribute(localName, escapeIllegalChars(value));
    }

    private static final Pattern CDATA_TERMINATOR_SPLIT = Pattern.compile("(?<=]])(?=>)");

    void writeCData(String data) throws XMLStreamException {
        // https://stackoverflow.com/questions/223652/is-there-a-way-to-escape-a-cdata-end-token-in-xml
        for (String part : CDATA_TERMINATOR_SPLIT.split(data, -1)) {
            // see https://www.w3.org/TR/xml/#dt-cdsection
            reportWriter.writeCData(escapeIllegalChars(part));
        }
    }

    private static String escapeIllegalChars(String value) {
        boolean allAllowed = true;
        for (int i = 0; i < value.length(); i = offsetByCodePoints(value, i, 1)) {
            int codePoint = value.codePointAt(i);
            if (!isLegal(codePoint)) {
                allAllowed = false;
                break;
            }
        }
        if (allAllowed) {
            return value;
        }

        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i = offsetByCodePoints(value, i, 1)) {
            int codePoint = value.codePointAt(i);
            if (isLegal(codePoint)) {
                escaped.appendCodePoint(codePoint);
            } else {
                // see https://www.w3.org/TR/xml/#NT-CharRef
                escaped.append("&#").append(codePoint).append(';');
            }
        }
        return escaped.toString();
    }

    @SuppressWarnings("UnnecessaryParentheses")
    private static boolean isLegal(int codePoint) {
        // see https://www.w3.org/TR/xml/#charsets
        return codePoint == 0x9
                || codePoint == 0xA
                || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }

}
