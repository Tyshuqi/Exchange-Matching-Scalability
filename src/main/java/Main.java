import command.CreateAccountCommand;
import command.CreateCommand;
import engine.CreateExecutor;
import engine.EngineServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import utils.XMLParser;
import utils.XMLResponseGenerator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class Main {
    public static void main( String[] args ) throws ParserConfigurationException, TransformerException {
//        EngineServer server = new EngineServer();
//        server.start();

        Object command = XMLParser.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<create>\n" +
                "<account id=\"123456\" balance=\"-1000\"/>\n" +
                "<symbol sym=\"SPY\">\n" +
                "<account id=\"23456\">-1000</account>\n" +
                "</symbol>\n" +
                "</create>");

        EngineServer server = new EngineServer();
        server.start();

//            for (Object e: createCommand.getCommands()) {
//                if (e instanceof CreateAccountCommand) {
//                    CreateAccountCommand createAccountCommand = (CreateAccountCommand) e;
//                    System.out.println(createAccountCommand.getId() + " " + createAccountCommand.getBalance());
//                }
//            }
        }

//        Document document = XMLResponseGenerator.generateResponseDocument();
//        Element opened = XMLResponseGenerator.generateOpenedResponse(document, 10L, "abc", 1, 3.0);
//        Element error = XMLResponseGenerator.generateErrorResponseWithId(document, "123456", "Invalid Account ID");
//        document.getDocumentElement().appendChild(error.cloneNode(true));
//        document.getDocumentElement().appendChild(error.cloneNode(false));
//        document.getDocumentElement().appendChild(error.cloneNode(false));


//        System.out.println(XMLResponseGenerator.convertToString(document));

    }

