import command.CreateAccountCommand;
import command.CreateCommand;
import engine.EngineServer;
import utils.XMLParser;

public class Main {
    public static void main( String[] args ) {
//        EngineServer server = new EngineServer();
//        server.start();

        Object command = XMLParser.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<create>\n" +
                "<account id=\"123456\" balance=\"1000\"/>\n" +
                "<symbol sym=\"SPY\">\n" +
                "<account id=\"123456\">100000</account>\n" +
                "</symbol>\n" +
                "</create>");

        if (command instanceof CreateCommand) {
            CreateCommand createCommand = (CreateCommand) command;
            for (Object e: createCommand.getCommands()) {
                if (e instanceof CreateAccountCommand) {
                    CreateAccountCommand createAccountCommand = (CreateAccountCommand) e;
                    System.out.println(createAccountCommand.getId() + " " + createAccountCommand.getBalance());
                }
            }
        }

    }
}
