package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;


public class Main {
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final int port = 23456;
    private static final String directory = "/home/danielvoipan/IdeaProjects/File Server/File Server/task/src/client/data/";

    public static void main(String[] args) {
        System.out.println("Client started!");
        try (
                Socket socket = new Socket(IP_ADDRESS, port);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        ) {
            Scanner scanner = new Scanner(System.in);
            boolean getOut = false;
            while (true) {
                if (getOut)
                    break;
                System.out.print("Enter action (1 - get a file, 2 - save a file, 3 - delete a file): ");
                String n = scanner.nextLine();
                switch (n.toLowerCase()) {
                    case "1" :
                        getFile(scanner, input, output);
                        break;
                    case "2" :
                        saveFile(scanner, input, output);
                        break;
                    case "3" :
                        deleteFile(scanner, input, output);
                        break;
                    case "exit" :
                        System.out.println("The request was sent.");
                        output.writeUTF("EXIT NOW");
                        getOut = true;
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // delete file from server
    private static void deleteFile(Scanner scanner, DataInputStream input, DataOutputStream output) throws IOException {
        System.out.print("Do you want to delete the file by name or by id (1 - name, 2 - id): ");
        String getFileByWhat = scanner.nextLine();
        int type = 0;

        switch (getFileByWhat) {
            case "1" :
                System.out.print("Enter name of the file: ");
                type = 1;
                break;
            case "2" :
                System.out.print("Enter id: ");
                type = 2;
        }
        if (type == 1) {
            String fileName = scanner.nextLine();
            output.writeUTF("DELETE " + "BY_NAME " + fileName);
        } else {
            String id = scanner.nextLine();
            output.writeUTF("DELETE " + "BY_ID " + id);
        }
        System.out.println("The request was sent.");
        // read output from server
        // 404 - file not found, 200 - file found.
        String del = input.readUTF();
        if (del.equals("404")) {
            System.out.println("The response says that this file is not found!");
        } else {
            System.out.println("The response says that this file was deleted successfully!");
        }
    }

    // save file from  client to server
    private static void saveFile(Scanner scanner, DataInputStream input, DataOutputStream output) throws IOException {
        System.out.print("Enter name of the file: ");
        String fileNameToSave = scanner.nextLine();
        System.out.print("Enter name of the file to be saved on server: ");
        String newFileNameToSave = scanner.nextLine();
        if (newFileNameToSave.length() == 0) {
            newFileNameToSave = makeFileName();
        }
        System.out.println("The request was sent.");
        ClientFile out = saveFileInByteArray(fileNameToSave);
        if (out.getCode() == 403) {
            System.out.println("The response says that this file is not found!");
            return;
        }
        output.writeUTF("PUT" + " " + newFileNameToSave);
        output.writeInt(out.getContent().length);
        output.write(out.getContent());

        String result = input.readUTF();
        String[] split = result.split(" ");
        String code = split[0];
        // the file exists
        if (code.equals("403")) {
            System.out.println("The response says that this file already exists!");
        } else {
            String returnId = split[1];
            System.out.printf("Response says that file is saved! ID = %s\n", returnId);
        }
    }

    // make a random file name
    private static String makeFileName() {
        long unixTime = System.currentTimeMillis() / 1000L;
        return "file" + unixTime;
    }

    // get file from server and save it
    private static void getFile(Scanner scanner, DataInputStream input, DataOutputStream output) throws IOException {
        System.out.print("Do you want to get the file by name or by id (1 - name, 2 - id): ");
        String getFileByWhat = scanner.nextLine();
        int type = 0;

        switch (getFileByWhat) {
            case "1" :
                System.out.print("Enter name of the file: ");
                type = 1;
                break;
            case "2" :
                System.out.print("Enter id: ");
                type = 2;
                break;
        }
        if (type == 1) {
            String fileName = scanner.nextLine();
            output.writeUTF("GET " + "BY_NAME " + fileName);
        } else {
            String id = scanner.nextLine();
            output.writeUTF("GET " + "BY_ID " + id);
        }
        System.out.println("The request was sent.");
        // read output from server
        // 404 - file not found, 200 - file found.
        int code = input.readInt();
        if (code == 404) {
            System.out.println("The response says that this file is not found!");
            return;
        }
        int length = input.readInt();
        byte[] out = new byte[length];
        input.readFully(out, 0, out.length);
        System.out.print("The file was downloaded! Specify a name for it: ");
        String newFileName = scanner.nextLine();
        File f = new File(directory + newFileName);
        try (OutputStream os = new FileOutputStream(f)) {
            os.write(out);
        }
        System.out.println("File saved on the hard drive!");
    }

    private static ClientFile saveFileInByteArray(String name) throws IOException {
        File f = new File(directory + name);
        if (!f.exists()) {
            return new ClientFile(403, new byte[]{});
        }
        byte[] buffer = new byte [(int) f.length()];
        FileInputStream is = new FileInputStream(f);
        is.read(buffer);
        is.close();
        return new ClientFile(200, buffer);
    }
}

class ClientFile {
    int code;
    byte[] content;

    ClientFile(int code, byte[] content) {
        this.code = code;
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

    public int getCode() {
        return code;
    }
}
