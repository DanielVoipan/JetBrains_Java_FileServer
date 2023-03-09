package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final int port = 23456;

    private static final String directory = "/home/danielvoipan/IdeaProjects/File Server/File Server/task/src/server/data/";

    private static HashMap<Integer, String> map = new HashMap<>();

    private static final String database = "/home/danielvoipan/IdeaProjects/File Server/File Server/task/src/server/files.data";


    public static void main(String[] args) {
        System.out.println("Server started!");
        try (ServerSocket server = new ServerSocket(port)) {
            AtomicBoolean getOut = new AtomicBoolean(false);
            try {
                File f = new File(database);
                if (f.exists()) {
                    map = deserializeMap();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            while (true) {
                try (
                        Socket socket = server.accept();
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream())
                ) {
                    String msg = input.readUTF();
                    String[] split = msg.split("\\s");
                    byte[] out;
                    String fileName;
                    String searchByWhat = split[1];
                    String action = split[0];
                    if (split.length > 2) {
                        fileName = split[2];
                    } else {
                        fileName = null;
                    }
                    if (action.equals("EXIT")) {
                        socket.close();
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // get file content
                    if (action.equals("PUT")) {
                        int length = input.readInt();
                        out = new byte[length];
                        input.readFully(out, 0, out.length); // read the message
                    } else {
                        out = null;
                    }
                    getOut.set(chooseStuff(output, out, action, searchByWhat, fileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // choose action
    private static boolean chooseStuff(DataOutputStream output, byte[] out, String action, String searchByWhat, String fileName) {
        // action is the first word
        boolean procOut = false;
        try {
            switch (action.toUpperCase()) {
                // get file from server
                case "GET" -> {
                    String getFile;
                    if (fileName.matches("[0-9]+")) {
                        getFile = getFromMap(fileName, 0);
                    } else {
                        getFile = getFromMap(fileName, 1);
                    }
                    if (Objects.equals(getFile, null)) {
                        output.writeInt(404);
                    } else {
                        MyFile fOut = getFileInByteArray(getFile);
                        output.writeInt(fOut.getCode());
                        output.writeInt(fOut.getContent().length);
                        output.write(fOut.getContent());
                    }
                }
                // add file from client to server
                case "PUT" -> {
                    File f = new File(directory + searchByWhat);
                    if (f.exists()) {
                        output.writeUTF("403 ");
                        break;
                    }
                    try (OutputStream os = new FileOutputStream(f)) {
                        os.write(out);
                    }
                    int id = genId();
                    output.writeUTF("200 " + id);
                    addToMap(searchByWhat, id);
                }
                // delete file from server
                case "DELETE" -> {
                    String getFile;
                    if (fileName.matches("[0-9]+")) {
                        getFile = delFromMap(fileName, 0);
                    } else {
                        getFile = delFromMap(fileName, 1);
                    }
                    if (Objects.equals(getFile, null)) {
                        output.writeUTF("404");
                    } else {
                        String del = deleteFile(getFile);
                        output.writeUTF(del);
                    }
                }
                // exit, and shut down server
                case "EXIT" -> procOut = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return procOut;
    }
    // get file content in bytes array
    private static MyFile getFileInByteArray(String name) throws IOException {
        File f = new File(directory + name);
        if (!f.exists()) {
            return new MyFile(404, new byte[]{});
        }
        byte[] buffer = new byte [(int) f.length()];
        FileInputStream is = new FileInputStream(f);
        is.read(buffer);
        is.close();
        return new MyFile(200, buffer);
    }

    // delete file
    private static String deleteFile(String name) {
        File f = new File(directory + name);
        if (!f.exists()) {
            return "404";
        }
        f.delete();
        return "200";
    }

    private static synchronized Map<Integer, String> getMap() {
        return map;
    }

    // get from Map by name or id
    private static String getFromMap(String name, int type) {
        StringBuilder str = new StringBuilder();
        for (var m : map.entrySet()) {
            if (type == 0) {
                if (m.getKey() == Integer.parseInt(name)) {
                    str.append(m.getValue());
                }
            } else {
                if (m.getValue().equals(name)) {
                    str.append(m.getValue());
                }
            }
        }
        if (str.toString().length() == 0) {
            return null;
        } else {
            return str.toString();
        }
    }

    // deserialize Map
    private static HashMap<Integer, String> deserializeMap() {
        try {
            return (HashMap<Integer, String>) SerializationUtils.deserialize(database);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    // add to Map and refresh the database file
    private static synchronized void addToMap(String name, int id) {
        map.put(id, name);
        try {
            SerializationUtils.serialize(getMap(), database);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // remove from Map and refresh the database file
    private static synchronized String delFromMap(String name, int type) {
        boolean found = false;
        String getFile = null;
        for (var m : map.entrySet()) {
            if (type == 1 && m.getValue().equals(name)) {
                map.remove(m.getKey());
                getFile = m.getValue();
                found = true;
                break;
            } else if (type == 0 && m.getKey() == Integer.parseInt(name)) {
                map.remove(m.getKey());
                found = true;
                getFile = m.getValue();
                break;
            }
        }
        if (found) {
            try {
                SerializationUtils.serialize(getMap(), database);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getFile;
    }

    // generate next ID based on saved Map
    private static int genId() {
        int lastId = 0;
        for (var m : map.entrySet()) {
            if (m.getKey() > lastId) {
                lastId = m.getKey();
            }
        }
        return ++lastId;
    }

}
// serialize - deserialize
class SerializationUtils {
    /**
     * Serialize the given object to the file
     */
    public static void serialize(Object obj, String fileName) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.close();
    }

    /**
     * Deserialize to an object from the file
     */
    public static Object deserialize(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(fileName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }
}

class MyFile {
    int code;
    byte[] content;


    MyFile(int code, byte[] content) {
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