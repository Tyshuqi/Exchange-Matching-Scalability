package utils;

import command.OrderCommand;
import entity.Order;
import entity.Transaction;
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

    public static Document generateResponseDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Element results = document.createElement("results");
        document.appendChild(results);

        return document;
    }

    public static Element generateCreatedResponse(Document document, Long id, String sym){
        Element created = document.createElement("created");
        created.setAttribute("id", String.valueOf(id));

        // Add the symbol attribute if it's not null which means from handlesymbolcommand
        if (sym != null && !sym.isEmpty()) {
            created.setAttribute("sym", sym);
        }
        return created;
    }

    public static Element generateOpenedResponse(Document document, Long id, String sym, int amount, double limit) {
        Element opened = document.createElement("opened");
        opened.setAttribute("id", String.valueOf(id));
        opened.setAttribute("sym", sym);
        opened.setAttribute("amount", String.valueOf(amount));
        opened.setAttribute("limit", String.format("%.2f", limit));

        return opened;
    }

<<<<<<< HEAD
    public static Element generateErrorCreateResponse(Document document, String id, String sym, String message) {
        Element error = document.createElement("error");
        // Set the account id or sym as attributes, if provided
        if (id != null && !id.isEmpty()) {
            error.setAttribute("id", id);
        }
        if (sym != null && !sym.isEmpty()) {
            error.setAttribute("sym", sym);
        }
        // Set the error message
        error.setTextContent(message);

        return error;
    }
=======

    public static Element generateStatusByOrder(Document document, Order order, boolean forCancel) {
        Element status = document.createElement(forCancel ? "canceled" : "status");
        status.setAttribute("id", String.valueOf(order.getId()));

        if (order.getStatus() == Order.Status.OPEN) {
            Element open = document.createElement("open");
            open.setAttribute("shares", String.valueOf(order.getAmount()));
            status.appendChild(open);
        }
        else if (order.getStatus() == Order.Status.CANCELED) {
            Element cancel = document.createElement("canceled");
            cancel.setAttribute("shares", String.valueOf(order.getAmount()));
            cancel.setAttribute("time", String.valueOf(order.getCanceledTime()));
            status.appendChild(cancel);
        }

        for (Transaction transaction: order.getTransactions()) {
            Element executed = document.createElement("executed");
            executed.setAttribute("shares", String.valueOf(transaction.getShares()));
            executed.setAttribute("price", String.format("%.2f", transaction.getPrice()));
            executed.setAttribute("time", String.valueOf(transaction.getTime()));
            status.appendChild(executed);
        }

        return status;
    }

>>>>>>> 27c769375e3f9aa80b425e0a7a432e4e1d619df4
    public static Element generateErrorResponse(Document document, String message) {
        Element error = document.createElement("error");
        error.setTextContent(message);
        return error;
    }

    public static Element generateErrorResponseWithId(Document document, String id, String message) {
        Element error = generateErrorResponse(document, message);
        error.setAttribute("id", id);
        return error;
    }

    public static Element generateOrderErrorResponse(Document document, OrderCommand orderCommand, String message) {
        Element error = generateErrorResponse(document, message);
        error.setAttribute("sym", orderCommand.getSym());
        error.setAttribute("amount", String.valueOf(orderCommand.getAmount()));
        error.setAttribute("limit", String.format("%.2f", orderCommand.getLimit()));

        return error;
    }

    public static String convertToString(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}
