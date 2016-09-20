package socketdeli;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Socket  {

	private java.net.Socket socket;
	private String hostname;
	private int port;
	private InputStream istream;
	private OutputStream ostream;
	private SocketDelegate delegate;
	private Thread readThread;
	
	public Socket(String hostname, int port) throws UnknownHostException, IOException  {
		this.hostname = hostname;
		this.port = port;
	}

	public void connect(SocketDelegate delegate) {

		if (delegate == null)
			throw new IllegalArgumentException("delegate");
		
		this.delegate = delegate;
		
		Runnable runnable = new Runnable(){
            public void run() {
                try {
                	
                	byte[] buffer = new byte[1024];
                	
                	PacketAccumulator accumulator = new PacketAccumulator();
                	
                	if (socket == null)
                		socket = new java.net.Socket(hostname, port);
                	else{
                		InetSocketAddress sa = new InetSocketAddress(hostname, port);
                		socket.connect(sa);
                	}
                	
                	istream = socket.getInputStream();
                	ostream = socket.getOutputStream();
                	
                	Socket.this.delegate.onConntected();
                    Thread current = Thread.currentThread();
                    while (current == readThread && istream != null) 
                    {
                    	int bytesRead = istream.read(buffer);
                    	
                    	byte[] readBuffer = new byte[bytesRead];
                    	
                    	System.arraycopy(buffer, 0, readBuffer, 0, readBuffer.length);
                    	
                    	Socket.this.delegate.onReceiveData(readBuffer);
                    	
                    	byte[] message = accumulator.accumulate(readBuffer);
                    	
                    	if (message != null)
                    		Socket.this.delegate.onReceiveMessage(message);
                    	
                    }
                } catch (IOException ex) {
                	Socket.this.delegate.onError(ex);
                    close();
                }
            }
		};
		
		readThread = new Thread(runnable);
        readThread.start();
		
	}
	
	public void disconnect(){
		close();
	}
	
	public void writeBinaryMessage(byte[] buffer){
		
        try
        {
            
            PacketHeader header = PacketHeader.create(buffer);
            
            byte[] headerBuffer = header.getBuffer();

            byte[] finalBuffer = new byte[PacketHeader.HeaderLength + buffer.length];

            System.arraycopy(headerBuffer, 0, finalBuffer, 0, headerBuffer.length);

            System.arraycopy(buffer, 0, finalBuffer, PacketHeader.HeaderLength, buffer.length);

            ostream.write(finalBuffer);

        }
        catch (Exception ex)
        {
            this.delegate.onError(ex);
        }
		
	}
	
	private void close() {
        try {
        	if (istream != null)
        		istream.close();
        	if (ostream != null)
        		ostream.close();
        	if (socket != null)
        		socket.close();
            istream = null;
            ostream = null;
            this.delegate.onDisconnected();
        } catch(IOException ex) {
            delegate.onError(ex);
        }
        delegate.onDisconnected();
    }

}


