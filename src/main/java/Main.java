import command.CreateCommand;
import command.TransactionsCommand;
import engine.CreateExecutor;
import engine.EngineServer;
import engine.TransactionsExecutor;
import utils.XMLParser;

public class Main {
    public static void main( String[] args ) {
        EngineServer server = new EngineServer();
        server.start();

//        Object createCommand = XMLParser.parse("""
//                <?xml version="1.0" encoding="UTF-8"?>
//                <create>
//                <account id="123456" balance="2000"/>
//                <symbol sym="SPY">
//                <account id="123456">1000</account>
//                </symbol>
//                </create>""");
//
//        CreateExecutor createExecutor = new CreateExecutor((CreateCommand) createCommand);
//        String response = createExecutor.execute();
//        System.out.println("response is:" + response);
//
//        Object command1 = XMLParser.parse("""
//                <?xml version="1.0" encoding="UTF-8"?>
//                <transactions id="123456">
//                <order sym="SPY" amount="20" limit="100"/>
//                <query id="1" />
//                </transactions>""");
//
//        TransactionsExecutor transactionsExecutor1 = new TransactionsExecutor((TransactionsCommand) command1);
//        response = transactionsExecutor1.execute();
//        System.out.println("response is:" + response);
//
//        Object command2 = XMLParser.parse("""
//                <?xml version="1.0" encoding="UTF-8"?>
//                <transactions id="123456">
//                <order sym="SPY" amount="-10" limit="50"/>
//                <cancel id="1" />
//                <query id="1" />
//                </transactions>""");
//
//        TransactionsExecutor transactionsExecutor2 = new TransactionsExecutor((TransactionsCommand) command2);
//        response = transactionsExecutor2.execute();
//        System.out.println("response is:" + response);
    }
}

