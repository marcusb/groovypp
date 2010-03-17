package org.mbte.groovypp.remote.netty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class BroadcastDiscovery {
    private static InetAddress GROUP;
    private static final int PORT = 4238;
    private static final long MAGIC = 0x23982392L;

    public UUID getUid() {
        return uid;
    }

    private final UUID uid;
    private final InetSocketAddress address;
    private Thread sendThread;
    private Thread receiveThread;
    private volatile boolean stopped;
    private MulticastSocket socket;

    static {
        try {
            GROUP = InetAddress.getByName("230.0.0.239");
        } catch (UnknownHostException ignored) {
            GROUP = null;
        }
    }

    public BroadcastDiscovery(final UUID uid, final InetSocketAddress address) {
        this.uid = uid;
        this.address = address;
    }

    public void start() {
        try {
            socket = new MulticastSocket(PORT);
            final InetAddress group = GROUP;
            socket.joinGroup(group);

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final DataOutputStream stream = new DataOutputStream(out);

            stream.writeLong(MAGIC);
            stream.writeLong(uid.getMostSignificantBits());
            stream.writeLong(uid.getLeastSignificantBits());
            stream.writeInt(address.getPort());
            final byte[] addrBytes = address.getAddress().getAddress();
            stream.writeInt(addrBytes.length);
            stream.write(addrBytes);
            stream.close();

            final byte[] bytes = out.toByteArray();

            sendThread = new Thread() {
                @Override
                public void run() {
                    while (!stopped) {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                        }

                        try {
                            final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, GROUP, PORT);
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            sendThread.start();

            receiveThread = new Thread() {
                @Override
                public void run() {
                    final byte[] buf = new byte[3 * 8 + 3 * 4];
                    final byte[] addrBuf4 = new byte[4];
                    final byte[] addrBuf6 = new byte[6];
                    while (!stopped) {
                        final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        try {
                            socket.receive(packet);
                            final DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf));
                            if (in.readLong() == MAGIC) {
                                final UUID uuid = new UUID(in.readLong(), in.readLong());
                                final int port = in.readInt();
                                final int addrLen = in.readInt();
                                if (addrLen == 4) {
                                    in.read(addrBuf4);
                                    onDiscovery(uuid, new InetSocketAddress(InetAddress.getByAddress(addrBuf4), port));
                                } else {
                                    in.read(addrBuf6);
                                    onDiscovery(uuid, new InetSocketAddress(InetAddress.getByAddress(addrBuf6), port));
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            receiveThread.start();
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void stop() {
        try {
            stopped = true;

            if (sendThread != null) {
                sendThread.join();
            }

            if (receiveThread != null) {
                receiveThread.join();
            }

            if (socket != null) {
                socket.close();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void onDiscovery(final UUID uuid, final SocketAddress address) {
    }
}