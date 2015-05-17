

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;


public class myhttpserver {
	static final String INDEX_FILE_ROOT = "/Users/yoshikawamuga/Documents/html/";
	static int portNumber;

	public static void main(String[] args) throws IOException{		
		getPortNum();
		ServerSocket mainServer = null;
		int numConnectionThread = 0;
		try{
			mainServer = new ServerSocket(portNumber);
			System.out.println("Server is listening on port " + mainServer.getLocalPort());
			while(true){
				Socket client = mainServer.accept();

				ConnectionThread ct = new ConnectionThread(client, numConnectionThread);
				System.out.println("client port: " + client.getPort());

				ct.start();
				numConnectionThread++;
			}			
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static void getPortNum(){
		try{
			FileReader portNumTextFile = new FileReader(INDEX_FILE_ROOT + "portnum.txt");
			BufferedReader buffReaderTextFile = new BufferedReader(portNumTextFile);
			String lineTextFile;
			String[] splitedString;
			

			while((lineTextFile = buffReaderTextFile.readLine()) != null){
				splitedString = lineTextFile.split(" ");//split line with space
				if(splitedString[0].equals("PORT")){
					portNumber =  Integer.parseInt(splitedString[1]);
					break;
				}
			}
			buffReaderTextFile.close();		
			System.out.println("Get Port: " + portNumber);
		}catch(Exception e){
			System.out.println(e);
		}
	}
}

