package socketdeli;

import java.security.InvalidParameterException;
import java.util.zip.CRC32;

import javax.management.InvalidApplicationException;


public class PacketHeader {

	
	public static int MaxPacketSize = 1024 * 1024 * 128; // 128 MB
	
	public static final byte[] HeaderBuffer = new byte[] { 0, 1, 1, 2, 3, 5, 8, 13, 21, 34 }; // fibonacci sequence
    public static final int HeaderBufferLength = 10;
    public static final int SizeAndCrcHeaderLength = 12;
    public static final int HeaderLength = HeaderBufferLength + SizeAndCrcHeaderLength;

    private static final CRC32 crc = new CRC32();
    
    private long _checksum;
    private int _packetSize;
    
    // returns the expected checksum from the header
    public long getChecksum() { return _checksum; }
    
    // returns the expected packet size from the header
    public int getPacketSize() { return _packetSize; }
    
 // returns the total packet size, including the header
    public int getTotalMessageSize() { return HeaderLength + _packetSize; }
    
    private PacketHeader(int size, long checksum)
    {
    	
    	if (size > MaxPacketSize)
    		throw new InvalidParameterException("The size exceeds MaxPacketSize");
    	
    	_packetSize = size;
    	_checksum = checksum;
    }

    // returns this header instance as a byte array
    public byte[] getBuffer()
    {
        byte[] buffer = new byte[HeaderLength];

        System.arraycopy(HeaderBuffer, 0, buffer, 0, HeaderBuffer.length);

        int intSize = 4;

        for (int i = 0; i < intSize; i++)
        {
            byte b = (byte) ((_packetSize >> 8 * i) & 0x000000FF);
            buffer[HeaderBufferLength - 1 + 6 - i] = b;
        }

        for (int i = 0; i < intSize; i++)
        {
            byte b = (byte)((_checksum >> 8 * i) & 0x000000FF);
            buffer[HeaderBufferLength - 1 + 12 - i] = b;
        }

        return buffer;

    }

    // tries to get a valid header from a byte array
    public static PacketHeader fromBuffer(byte[] buffer) throws InvalidApplicationException
    {

        if (buffer.length < HeaderLength)
            return null;

        for (int i = 0; i < HeaderBufferLength; i++)
        {
            if (buffer[i] != HeaderBuffer[i])
                return null;
        }

        int size = 0;
        for (int i = 5; i > 1; i--)
        {
        	long b = unsignedToBytes(buffer[HeaderBufferLength + i]);
        	long value = (long)(b << 8 * (5 - i));
            size |= value;
        }

        long checksum = 0;
        for (int i = 5; i > 1; i--)
        {
            long b = unsignedToBytes(buffer[HeaderBufferLength + 6 + i]);
            long value = (long)(b << 8 * (5 - i));
            checksum |= value;
        }
        
        if (size > MaxPacketSize)
    		throw new InvalidApplicationException("The size from the header buffer exceeds MaxPacketSize");

        return new PacketHeader(size, checksum);

    }

    // Creates a valid header for a given byte array
    public static PacketHeader create(byte[] buffer)
    {
    	
    	crc.reset();
    	crc.update(buffer);

    	long checksum = crc.getValue();
    	 
        return new PacketHeader(buffer.length, checksum);
        
    }
    
    private static long unsignedToBytes(byte b) {
        return b & 0xFF;
    }
	
}
