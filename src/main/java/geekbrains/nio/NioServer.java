package geekbrains.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class NioServer {
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buf;
    private Path currentDir;

    public NioServer (int port) throws IOException {
        currentDir = Paths.get("./");
        buf = ByteBuffer.allocate(5);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (serverChannel.isOpen()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            try {
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        handleAccept();
                    }

                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    iterator.remove();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    private void handleRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder msg = new StringBuilder();
        while (true){
            int read = channel.read(buf);
            if (read == -1){
                channel.close();
                return;
            }

            if (read ==0){
                break;
            }
            buf.flip();
            while (buf.hasRemaining()){
                msg.append((char)buf.get());
            }

            buf.clear();
        }

        processMessage(channel ,msg.toString().trim());

    }

    private void processMessage (SocketChannel channel, String msg) throws IOException {
        String[] tokens = msg.split(" +");
        TerminalCommandType type = null;
        try {
            type = TerminalCommandType.byCommand(tokens[0]);
            switch (type){
                case LS:
                    sendString(channel, getFilesList());
                    break;
                case CAT:
                    processCatCommand(channel, tokens);
                    break;
                case CD:
                    processCdCommand(channel, tokens);
                case MKDIR:
                    processMkdir(channel, tokens);
                case TOUCH:
                    processTouch(channel, tokens);

            }

        } catch (RuntimeException e){
            String response = "Command " + tokens[0] + " is not exists\n\r";
            sendString(channel, response);
        }
    }

    private void processTouch(SocketChannel channel, String[] tokens) throws IOException {
        String newFile = "";
        if (tokens == null || tokens.length !=2) {
            sendString(channel, "command MKDIR should have 2 args");
        }else {
            newFile = tokens[1];
            Files.createFile(Paths.get(newFile));
        } if (Files.exists(currentDir)){
            sendString(channel, "File " + newFile + " created\n\r");
        } else {
            sendString(channel, "File don't created\n\r");
        }

    }

    private void processMkdir(SocketChannel channel, String[] tokens) throws IOException {
        String dirName= "";
        if (tokens == null || tokens.length !=2) {
            sendString(channel, "command MKDIR should have 2 args");
        }else {
            dirName = tokens[1];
            Files.createDirectories(Paths.get(dirName));
        } if (Files.isDirectory(currentDir)){
            sendString(channel, "directory " + dirName + " created\n\r");
        } else {
            sendString(channel, "directory don't created\n\r");
        }
    }



    private void processCdCommand(SocketChannel channel, String[] tokens) throws IOException {
        if (tokens == null || tokens.length !=2){
            sendString(channel, "command cat should have 2 args\n\r");
        }else {
            String dir = tokens[1];
            if (Files.isDirectory(currentDir.resolve(dir))){
                currentDir = currentDir.resolve(dir);
                channel.write(ByteBuffer.wrap("nnoopt->".getBytes(StandardCharsets.UTF_8)));
            } else {
                sendString(channel, "you cannot use cd command to DIR\n\r");
            }
        }
    }

    private void processCatCommand(SocketChannel channel, String [] tokens) throws IOException {
        if (tokens == null || tokens.length !=2){
            sendString(channel, "command cat should have 2 args");
        } else {
            String fileName = tokens[1];
            Path file =  currentDir.resolve(fileName);
            if (!Files.isDirectory(file)) {
                String content = new String(Files.readAllBytes(file)) + "\n\r";
                sendString(channel, content);
            }else {
                sendString(channel, "you cannot use cat command to DIR\n\r");
            }
        }
    }

    private String getFilesList() throws IOException {
        return Files.list(currentDir)
                .map(p->p.getFileName()
                .toString() + " " + getFileSuffix(p)).collect(Collectors.joining("\n")) + "\n\r";
    }

    private String getFileSuffix(Path path) {
        if (Files.isDirectory(path)){
            return "[DIR]";
        } else {
            return "[FILE] " + path.toFile().length() + " bytes";
        }
    }

    private void sendString(SocketChannel channel, String msg) throws IOException {
        channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("nnoopt->".getBytes(StandardCharsets.UTF_8)));
    }

    private void handleAccept() throws IOException {
        System.out.println("Client accepted");
            SocketChannel socketChannel = serverChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ, "Hello");
            socketChannel.write(ByteBuffer.wrap(("Welcome\n\r" + "nnoopt-> ").getBytes(StandardCharsets.UTF_8)));
        }



    public static void main(String[] args) throws IOException {
        new NioServer(8189);


    }
}
