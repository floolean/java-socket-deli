package socketdeli;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import javax.management.InvalidApplicationException;

public class PacketAccumulator {
	
	private static final CRC32 crc = new CRC32();
	
	List<Byte> _receiveBuffer = new ArrayList<Byte>();

    public byte[] accumulate(byte[] buffer) throws InvalidApplicationException
    {

    	for(int i = 0; i < buffer.length; i++)
    		_receiveBuffer.add(buffer[i]);

        if (_receiveBuffer.size() >= PacketHeader.HeaderLength) // we got enough data to check for a valid header
        {
            PacketHeader header = PacketHeader.fromBuffer(buffer);

            if (header != null) // we got a valid header
            {
                if (_receiveBuffer.size() >= header.getTotalMessageSize()) // check if we got enough data for this packet
                {
                	
                	int messageStart = PacketHeader.HeaderLength;
                	int messageEnd = messageStart + header.getPacketSize();
                	
                    byte[] messageBuffer = new byte[header.getPacketSize()];
                    
                    for( int i = messageStart; i < messageEnd; ++i) // copy data to a byte array that fits the given packet size from header, without the header
                    	messageBuffer[i - messageStart] = _receiveBuffer.get(i);

                    crc.reset();
                    crc.update(messageBuffer); // calculate crc checksum of byte array
                    
                    long checksum = crc.getValue();

                    if (checksum == header.getChecksum()) // check if we got the same checksum result
                    {

                        int total = _receiveBuffer.size();
                        int messageLength = messageStart + messageEnd;
                        List<Byte> tmpList = new ArrayList<Byte>();
                        for(int i = messageLength; i < total; i++)
                        	tmpList.add(_receiveBuffer.get(i));
                        _receiveBuffer = tmpList;
                        return messageBuffer; // return a valid byte buffer 
                    }

                }
            }
            else // the header was invalid, so we skip the rest of the data
            {
                _receiveBuffer.clear();
                return null;
            }

        }

        return null;
    }
	
}
