package socketdeli;

public interface SocketDelegate 
{
	public void onError(Exception exception);
	public void onConntected();
	public void onDisconnected();
	public void onReceiveData(byte[] buffer);
	public void onReceiveMessage(byte[] buffer);
}
