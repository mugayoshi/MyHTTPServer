import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

class ConnectionThread extends Thread{
	Socket clientSocket;
	static final String INDEX_FILE_ROOT = "/Users/yoshikawamuga/Documents/html/";
	String type;
	boolean keepAlive;
	int threadNum;
	String[] public_dir;
	public ConnectionThread(Socket clientSock, int num) throws SocketException{
		this.clientSocket = clientSock;
		keepAlive = false;
		this.threadNum = num;
		this.public_dir = new String[10];
		getPublicDirectory();
		try{
			this.clientSocket.setSoTimeout(30000);
			this.clientSocket.setKeepAlive(true);

		}catch(SocketException e){
			System.out.println(e);
		}
	}
	public void run(){
		String destIP = clientSocket.getInetAddress().toString();
		int destPort = clientSocket.getPort();
		System.out.println("IP: " + destIP);
		System.out.println("PORT: " + destPort);

		try{
			PrintStream clientSockOutStream = new PrintStream(clientSocket.getOutputStream());
			BufferedReader clientSockInputBufReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			boolean isFirstLine = false;
			boolean timeout = false;
			String inputFilePath = "";
			String responseFilePath = "";
			String inputLineFromClient = "";
			while(isFirstLine == false || this.keepAlive){

				do{
					try{
						inputLineFromClient = clientSockInputBufReader.readLine();

						if(isFirstLine == false && inputLineFromClient != null){
							inputFilePath = getRequestedFilePath(inputLineFromClient);
							isFirstLine = true;
						}else if(inputLineFromClient!= null &&inputLineFromClient.toUpperCase().contains("KEEP")){
							this.keepAlive = true;
							System.out.println("it's gonna keeping alive");
						}
						if(inputLineFromClient == null){
							break;
						}else{
							System.out.println(inputLineFromClient);
						}
					}catch(SocketTimeoutException e){
						System.out.println("TIME OUT!!");
						clientSockOutStream.close();
						timeout = true;
						break;
					}
				}while(clientSockInputBufReader.ready());
				isFirstLine = false;

				if(timeout){
					System.out.println("break cuz it's time out");
					break;
				}
				
				if(inputLineFromClient == null){
					System.out.println("input from client is null");
					break;
				}

				if(inputFilePath.equals("/")){
					this.type = "html";
					responseFilePath = INDEX_FILE_ROOT + "index.html";
				}else if(inputFilePath.contains("/") == false){
					//this condition is for a file that is located in html dir 
					//i.e. /Documents/html/test.html
					responseFilePath = INDEX_FILE_ROOT + inputFilePath;
				}else if(checkDirectory(inputFilePath) == false){
					responseFilePath = INDEX_FILE_ROOT + "wrong_dir.html";
				}else{
					responseFilePath = INDEX_FILE_ROOT + inputFilePath;
				}

				File responseFile = new File(responseFilePath);
				System.out.println("Thread #:" + this.threadNum + ": " + responseFile.getName() + " is requested");
				sendfile(clientSockOutStream, responseFile);
				clientSockOutStream.flush();
				if(!this.keepAlive){
					clientSockOutStream.close();
					continue;
				}				

			}//end of while()
		}catch (IOException e) {
			//	System.out.print("IOException: ");
			System.out.println(e);//+ e);
		}

	}
	//read file and write to browser
	void sendfile(PrintStream stream, File file){
		try{			
			String responseStr;			
			if(file.exists()){				
				DataInputStream inputStream = new DataInputStream(new FileInputStream(file));
				int len = (int)file.length();
				byte buf[] = new byte[len];				
				inputStream.readFully(buf);
				
				this.type = getFileType(file.getName());
				
				long lastMod = file.lastModified();
				Date date = new Date();
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				
				responseStr = "HTTP/1.1 200 OK \r\n" +					
						"date: " + dateFormat.format(date) + "\r\n" +
						"server: Muga's HTTP Server\r\n" +
						"last-modified: " + dateFormat.format(lastMod) + "\r\n" +						
						"content-length: " + len + "\r\n" +
						"keep-alive: timeout=15, max=90\r\n" +
						"connection: keep-alive\r\n" +
						"content-type: " + this.type + "\r\n\n";
				
				stream.write(responseStr.getBytes());				
				stream.write(buf, 0, len);
				stream.flush();
				stream.write("".getBytes());//writes blank at the end
				
				inputStream.close();				
			}else{
				String responseFile = INDEX_FILE_ROOT + "notfound.html";
				File err_file = new File(responseFile);
				DataInputStream inputStream = new DataInputStream(new FileInputStream(err_file));

				int len = (int)err_file.length();
				byte buf[] = new byte[len];
				inputStream.readFully(buf);
				
				responseStr = "HTTP/1.1 404 Not Found\r\n " + 
						"content-length: 22\r\n" + 
						"content-type: " + "text/html" + 
						"\n\n\n";
				
				stream.write(responseStr.getBytes());
				stream.write(buf, 0, len);
				stream.flush();
				stream.write("".getBytes());
				
				inputStream.close();
			}

		}catch(Exception e){
			System.out.println("Error sending file in send file method");
		}
	}
	String getRequestedFilePath(String input){
		String[] splitedStr = input.split(" ");
		if(splitedStr[1].equals("/") ){
			//in this case, input = "GET / HTTP/1.1"
			return splitedStr[1];
		}else{
			//in this case, input = "GET /style.css HTTP/1.1"
			String filePath = splitedStr[1].substring(1);
			return filePath;
		}
	}
	String getFileType(String filename){
		String[] splitedStr = filename.split("\\.");
		System.out.println("file type: " + splitedStr[1]);

		String  ext = splitedStr[1];
		if(ext.equals("html")){
			return "text/html";
		}else if(ext.equals("jpg")){
			return "image/jpeg";
		}else if(ext.equals("png")){
			return "image/png";
		}else if(ext.equals("gif")){
			return "image/gif";
		}else if(ext.equals("txt")){
			return "text/plain";
		}else if(ext.equals("css")){
			return "text/css";
		}else{
			return "text/html";
		}		
	}

	void getPublicDirectory(){

		try{
			FileReader publicDirFileReader = new FileReader(INDEX_FILE_ROOT + "public_direc.txt");
			BufferedReader fileBufReader = new BufferedReader(publicDirFileReader);
			String[] splitedStr;
			String lineTextFile;
			int publicDirCounter = 0;

			while((lineTextFile = fileBufReader.readLine()) != null){
				splitedStr = lineTextFile.split(" ");//split line with space
				for(int i = 0; i < splitedStr.length; i++){
					this.public_dir[publicDirCounter] =  splitedStr[i];
					publicDirCounter++;
				}
			}
			fileBufReader.close();		
		}catch(Exception e){
			System.out.println(e);
		}

	}

	boolean checkDirectory(String filePath){
		boolean containsPublicDir;
		String[] splitedPath = filePath.split("/");
		for(int i = 0; i < splitedPath.length - 1; i++){
			containsPublicDir = false;
			for(int j = 0; j < public_dir.length; j++){
				if(splitedPath[i].equals(public_dir[j])){
					containsPublicDir = true;
				}
			}
			if(!containsPublicDir){
				System.out.println("wrong directory is accessed now");
				return false;
			}else if(i == splitedPath.length - 2){
				return true;
			}
		}
		return false;
	}


}//end of connection thread
