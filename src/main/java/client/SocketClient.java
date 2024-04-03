package client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketClient {

    private static String SERVER_HOST = "localhost";
    private static int SERVER_PORT = 12345;
    //private static final String XML_FILE_PATH = "test.xml";


    public static void sendXmlToServer(String xmlData) {
        String threadInfo = "Thread ID: " + Thread.currentThread().getId() + ", Name: " + Thread.currentThread().getName();
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {

            // Log sending data with thread info
            System.out.println(threadInfo + " - Sending XML data to server.");

            // Send XML data to server
            outputStream.writeUTF(xmlData);
            outputStream.flush();

            // Read response from server and log with thread info
            String response = inputStream.readUTF(); // This will block until a response is received
            System.out.println(threadInfo + " - Server response: " + response);

        } catch (IOException e) {
            System.out.println(threadInfo + " - Error: ");
            e.printStackTrace();
        }
    }


    public static String readXmlFromResources(String fileName) {
        try (InputStream inputStream = SocketClient.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource file" + fileName + "not found");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

//    public static void main(String[] args) {
//
//        SERVER_HOST = "localhost";
//        SERVER_PORT = 12345;
//        String xmlData = readXmlFromResources();
//
//        if (xmlData == null) {
//            System.err.println("Failed to read XML data from file.");
//            return;
//        }
//        System.out.println(xmlData);
//        int numberOfThreads = 10; // Number of threads to simulate clients
//        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
//
//        for (int i = 0; i < numberOfThreads; i++) {
//            executor.submit(() -> sendXmlToServer(xmlData));
//        }
//
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            System.out.println("Shutting down...");
//            executor.shutdownNow();
//        }));
//    }
//}

    public static void main(String[] args) {
        SERVER_HOST = "localhost";
        SERVER_PORT = 12345;

        int numberOfThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 1; i <= numberOfThreads; i++) {
            String fileName = "test" + i + ".xml";
            String xmlData = readXmlFromResources(fileName);

            if (xmlData == null) {
                System.err.println("Failed to read XML data from file: " + fileName);
                continue;
            }

            // log
            System.out.println("Thread will send data from: " + fileName);
            executor.submit(() -> sendXmlToServer(xmlData));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            executor.shutdownNow();
        }));
    }
}

