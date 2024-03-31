package utils;

import entity.Order;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class XMLResponseGenerator {

    public static String generateOpenedResponse(Order order) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Element results = document.createElement("results");
        document.appendChild(results);

        Element opened = document.createElement("opened");
        opened.setAttribute("id", String.valueOf(order.getId()));
        opened.setAttribute("sym", order.getSymbol());
        opened.setAttribute("amount", String.valueOf(order.getAmount()));
        opened.setAttribute("limit", String.format("%.2f", order.getLimitPrice()));
        results.appendChild(opened);

        return convertToString(document);
    }
    private static String convertToString(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}
