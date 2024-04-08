package engine;

import command.CreateCommand;
import command.TransactionsCommand;
import utils.XMLParser;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TaskHandler implements Runnable {
    Socket clientSocket;
    public TaskHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream())) {
            String message = inputStream.readUTF();
            Object command;
            String response = "";

            try {
                command = XMLParser.parse(message);

                if (command instanceof CreateCommand) {
                    CreateExecutor createExecutor = new CreateExecutor((CreateCommand) command);
                    response = createExecutor.execute();
                } else if (command instanceof TransactionsCommand) {
                    TransactionsExecutor transactionsExecutor = new TransactionsExecutor((TransactionsCommand) command);
                    response = transactionsExecutor.execute();
                }
            } catch (XMLStreamException | JAXBException e) {
                response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><results><error>Invalid XML format or structure</error></results>";
                e.printStackTrace();
            }

            outputStream.writeUTF(response);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
