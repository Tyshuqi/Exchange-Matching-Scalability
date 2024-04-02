package engine;

import command.CreateCommand;
import command.TransactionsCommand;
import utils.XMLParser;

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
//            System.out.printf("message is "+ message);
            Object command = XMLParser.parse(message);
            String response = "";

            if (command instanceof CreateCommand) {
                CreateExecutor createExecutor = new CreateExecutor((CreateCommand) command);
                response = createExecutor.execute();
                System.out.println("response is:" + response);
            }
            else if (command instanceof TransactionsCommand) {
                TransactionsExecutor transactionsExecutor = new TransactionsExecutor((TransactionsCommand) command);
                response = transactionsExecutor.execute();
            }
            outputStream.writeUTF(response);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
