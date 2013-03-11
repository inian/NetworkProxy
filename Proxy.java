import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;

public class Proxy {
	public static void main(String[] args) throws IOException {
		// get this as the arg value
		int listeningPort = Integer.parseInt(args[0]);
		ServerSocket ss = new ServerSocket(listeningPort);

		while (true) {
			// create a socket
			Socket client = ss.accept();
			resolveProxy(client);
			client.close();
		}
	}
	/*
	 * This function resolves the request from the client, makes the request and writes 
	 * the request back
	 */
	public static void resolveProxy(Socket clientSocket) throws IOException {
		// get client request
		InputStream incomingRequest = clientSocket.getInputStream();
		OutputStream outputResult = clientSocket.getOutputStream();
		String request = new String("");
		byte[] cbuf = new byte[500];
		int bytes_read = 0;
		while ((bytes_read = incomingRequest.read(cbuf, 0, 500)) > 0) {
			String buffer = new String(cbuf.clone());
			request = request + new String(buffer);

			if (request.contains("\r\n\r\n")) {
				// all the headers are recieved now
				if (request.startsWith("POST")) {
					String contentLength = "Content-Length: ";
					// Some requests have a small l for length in the header
					request = request.replace("Content-length",
							"Content-Length");
					int start_index = request.indexOf("Content-Length: ")
							+ contentLength.length();
					// add in the post message body
					int message_length = Integer.parseInt(request.substring(
							start_index,
							start_index
									+ request.substring(start_index).indexOf(
											"\n")).trim());
					int message_already_read = request.substring(
							request.indexOf("\r\n\r\n")).length();
					while (message_already_read < message_length) {
						bytes_read = incomingRequest.read(cbuf, 0, 500);
						message_already_read += bytes_read;
						String temp = new String(cbuf.clone());
						request = request + new String(temp);
						cbuf = new byte[500];
					}
					break;
				} else {
					break;
				}
			}
			cbuf = new byte[500];
		}
		if (!request.equals("")) {
			String line = request.substring(0, request.indexOf("\n")).trim();
			if(!(line.endsWith("HTTP/1.0")) || line.endsWith("HTTP/1.1")) {
				String error = "<html>Bad request</html>";
				byte[] cbuf1 = new byte[100];
				cbuf1 = error.getBytes();
				outputResult.write(cbuf1);
				return; 
			}
			int start = request.indexOf(" ") + 1;
			int end = request.indexOf("HTTP");
			String request_url = "";
			try {
				request_url = request.substring(start, end); //get the request url from the request string
			} catch (StringIndexOutOfBoundsException e) {
				return;
			}
			URL url = null;
			try {
			url = new URL(request_url);
			} catch(MalformedURLException e) {
				//send a error message back to the client
				String error = "<html>Bad request</html>";
				byte[] cbuf1 = new byte[100];
				cbuf1 = error.getBytes();
				outputResult.write(cbuf1);
				return;
			}
			try {
				requestWebPage(url.getHost(), request, outputResult);
			} catch (IOException e) {

			}
		}
		outputResult.close();
		incomingRequest.close();
	}

	/*
	 * This function sets up the socket and communicates with the server to
	 * request for a particular web page.
	 */
	public static void requestWebPage(String hostname, String request,
			OutputStream resultStream) throws IOException {
		// setup the communication socket for port 80.
		Socket socket = new Socket(hostname, 80);
		InputStream inFromServer = socket.getInputStream();

		OutputStream out = socket.getOutputStream();
		PrintWriter outw = new PrintWriter(out, false);
		outw.print(request);
		outw.flush();

		byte[] cbuf = new byte[200];
		int bytes_got = 0;
		while ((bytes_got = inFromServer.read(cbuf)) > 0) {
			try {
				resultStream.write(cbuf, 0, bytes_got);
			} catch (SocketException e) {
				break;
			}
			cbuf = new byte[200];
		}
		resultStream.flush();
		resultStream.close();
		inFromServer.close();
		socket.close();
	}
	
}
