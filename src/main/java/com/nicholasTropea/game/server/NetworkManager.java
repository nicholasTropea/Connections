package com.nicholasTropea.game.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Listens for incoming connections and manages them using a NIO Selector.
 * 
 * Implements {@link Runnable} to execute the selection loop in a separate thread.
 * 
 * @author Nicholas Riccardo Tropea
 */
public class NetworkManager implements Runnable {
    /** Listening server port. */
    private final int port;

    /** Shared runtime containing repositories and coordinators. */
    private final ServerRuntime runtime;


    /**
     * Creates a new NetworkManager with the passed port.
     * 
     * @param port TCP port on which to listen for connections
     * @param runtime Shared server runtime dependencies
     */
    public NetworkManager(int port, ServerRuntime runtime) {
        this.port = port;
        this.runtime = Objects.requireNonNull(runtime, "runtime is required");
    }


    /** Executes the main server selection loop. */
    @Override
    public void run() {
        try (
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            Selector selector = Selector.open()
        ) {
            serverChannel.bind(new InetSocketAddress(this.port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server NIO active on port: " + this.port);

            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) { continue; }

                    if (key.isAcceptable()) {
                        handleAccept(serverChannel, selector);
                    }
                    else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        }
        catch (IOException e) {
            System.err.println("NIO Server error: " + e.getMessage());
        }
    }


    /**
     * Accepts a new client connection and registers it with the selector.
     */
    private void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        
        System.out.println("New NIO connection from: " + clientChannel.getRemoteAddress());
        
        ClientHandler handler = new ClientHandler(clientChannel, this.runtime);
        clientChannel.register(selector, SelectionKey.OP_READ, handler);
    }


    /**
     * Handles a read event from a client channel.
     */
    private void handleRead(SelectionKey key) {
        ClientHandler handler = (ClientHandler) key.attachment();
        try {
            handler.handleRead();
        }
        catch (IOException e) {
            System.err.println("Closing client connection: " + e.getMessage());
            handler.close();
            key.cancel();
        }
    }
}
