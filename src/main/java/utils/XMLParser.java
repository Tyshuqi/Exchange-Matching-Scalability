package utils;

import command.CreateCommand;
import command.TransactionsCommand;

import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

public class XMLParser {
    public static Object parse(String message) {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(message));

            xsr.nextTag();
            String rootElementName = xsr.getLocalName();

            Object command = null;
            JAXBContext context = switch (rootElementName) {
                case "create" -> JAXBContext.newInstance(CreateCommand.class);
                case "transactions" -> JAXBContext.newInstance(TransactionsCommand.class);
                default -> throw new IllegalArgumentException("Unexpected root element: " + rootElementName);
            };

            if (context != null) {
                Unmarshaller unmarshaller = context.createUnmarshaller();
                command = unmarshaller.unmarshal(xsr);
            }

            return command;
        }
        catch (XMLStreamException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
