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
	Socket client;
	static String INDEX_FILE_ROOT = "/Users/yoshikawamuga/Documents/html/";
	String type;
	boolean keepAlive;
	int threadNum;
	String[] public_dir = new String[10];
	public ConnectionThread(Socket c1, int num) throws SocketException{
		this.client = c1;
		keepAlive = false;
		this.threadNum = num;
		getPublicDirectory();
		try{
			this.client.setSoTimeout(30000);
			this.client.setKeepAlive(true);

		}catch(SocketException e){
			System.out.println(e);
		}
		//counter = c;
	}
	public void run(){
		//get client's IP
		String destIP = client.getInetAddress().toString();
		//get client's Port
		int destPort = client.getPort();
		//		int readflag = 0;//if the program is in while(readr.ready)loop, it's 1

		System.out.println("IP: " + destIP);
		System.out.println("PORT: " + destPort);

		try{
			PrintStream outstream = new PrintStream(client.getOutputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
			int i = 0;
			boolean timeout = false;
			String inputFilePath = "";
			String responseFile = "";
			String information = "";
			while(i == 0 || this.keepAlive){

				do{
					try{
						information = reader.readLine();

						if(i == 0 && information != null){
							inputFilePath = getFilePath(information);
							i++;
						}else if(information!= null &&information.toUpperCase().contains("KEEP")){
							this.keepAlive = true;
							System.out.println("it's gonna keeping alive");
						}
						if(information == null){
							break;
						}else{
							System.out.println(information);
						}
					}catch(SocketTimeoutException e){
						System.out.println("TIME OUT!!");
						outstream.close();
						timeout = true;
						break;
					}
				}while(reader.ready());
				i = 0;

				if(timeout){
					System.out.println("break cuz it's time out");
					break;
				}
				if(information == null){
					break;
				}

				if(inputFilePath.equals("/")){
					this.type = "html";
					responseFile = INDEX_FILE_ROOT + "index.html";
				}else if(inputFilePath.contains("/") == false){//this condition is for "localhhost:80/test.html"
					responseFile = INDEX_FILE_ROOT + inputFilePath;
				}else if(checkDirectory(inputFilePath) == false){
					responseFile = INDEX_FILE_ROOT + "wrong_dir.html";
				}else{
					responseFile = INDEX_FILE_ROOT + inputFilePath;
				}

				File file = new File(responseFile);
				System.out.println(this.threadNum + ": " + file.getName() + " is requested");
				sendfile(outstream, file);
				outstream.flush();
				if(!this.keepAlive){
					outstream.close();
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
			String response;			
			if(file.exists()){				
				DataInputStream in = new DataInputStream(new FileInputStream(file));
				int len = (int)file.length();
				byte buf[] = new byte[len];				
				in.readFully(buf);
				this.type = getFileType(file.getName());
				long lastMod = file.lastModified();
				Date date = new Date();
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				response = "HTTP/1.1 200 OK \r\n" +					
						"date: " + dateFormat.format(date) + "\r\n" +
						"server: Muga's HTTP Server\r\n" +
						"last-modified: " + dateFormat.format(lastMod) + "\r\n" +						
						"content-length: " + len + "\r\n" +
						"keep-alive: timeout=15, max=90\r\n" +
						"connection: keep-alive\r\n" +
						"content-type: " + this.type + "\r\n\n";						
				stream.write(response.getBytes());				
				stream.write(buf, 0, len);
				stream.flush();
				stream.write("".getBytes());
				in.close();				
			}else{
				String responseFile = INDEX_FILE_ROOT + "notfound.html";
				File err_file = new File(responseFile);
				DataInputStream in_err = new DataInputStream(new FileInputStream(err_file));

				int len2 = (int)err_file.length();
				byte buf2[] = new byte[len2];
				response = "HTTP/1.1 404 Not Found\r\n " + 
						"content-length: 22\r\n" + 
						"content-type: " + this.type + 
						"\n\n" + "<h1>404 Nicht Gefuden</h1>";
				stream.write(response.getBytes());
				stream.write(buf2, 0, len2);
				stream.flush();
				in_err.close();
			}
			System.out.println("thread number: " + this.threadNum + " in Send File");

		}catch(Exception e){
			System.out.println("Error sending file in send file method");
			//	System.exit(1);				
		}
	}
	String getFilePath(String info){
		String[] str = info.split(" ");
		if(str[1].equals("/") ){
			return str[1];
		}else{
			String filePath = str[1].substring(1);
			System.out.println("getFilePath: " + filePath);
			return filePath;
		}
	}
	String getFileType(String filename){
		String[] str = filename.split("\\.");
		System.out.println("file type: " + str[1]);

		String  ext= str[1];
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
			FileReader f = new FileReader(INDEX_FILE_ROOT + "public_direc.txt");
			BufferedReader b = new BufferedReader(f);
			String[] str;
			String line;
			int j = 0;

			while((line = b.readLine()) != null){
				//System.out.println(str);
				str = line.split(" ");//split line with space
				for(int i = 0; i < str.length; i++){
					this.public_dir[j] =  str[i];
					j++;
				}
			}
			b.close();		
		}catch(Exception e){
			System.out.println(e);
		}

	}

	boolean checkDirectory(String filePath){
		int found_flag;
		String[] direc = filePath.split("/");
		for(int i = 0; i < direc.length - 1; i++){
			found_flag = 0;
			for(int j = 0; j < public_dir.length; j++){
				if(direc[i].equals(public_dir[j])){
					found_flag = 1;
				}
			}
			if(found_flag == 0){
				System.out.println("directory " + direc[i] + " doesn't exist in public ones");
				return false;
			}else if(i == direc.length - 2){
				return true;
			}
		}
		//System.out.println("check directory:file path is " + "filePath");
		System.out.println("wrong directory is accessed now");
		return false;
	}


}//end of connection thread
