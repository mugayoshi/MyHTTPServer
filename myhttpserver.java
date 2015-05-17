

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;


public class myhttpserver {
	static String INDEX_FILE_ROOT = "/Users/yoshikawamuga/Documents/html/";
	static int portNumber;
	//	String[] public_dir = {"html", "html2", "direc1", "direc2"};

	public static void main(String[] args) throws IOException{		
		//int portNumber = Integer.parseInt(args[0]);
		//		portNumber = getPort();//Integer.parseInt(args[0]);		
		getPort();
		ServerSocket server = null;
		int i = 0;
		try{
			server = new ServerSocket(portNumber);
			System.out.println("Server is listening on port " + server.getLocalPort());
			while(true){
				Socket client = server.accept();

				ConnectionThread ct = new ConnectionThread(client, i);
				System.out.println("client port: " + client.getPort());

				ct.start();
				i++;
			}			
		}catch(Exception e){
			System.out.println(e);
		}
	}//end of main
	static void getPort(){
		try{
			FileReader f = new FileReader(INDEX_FILE_ROOT + "portnum.txt");
			BufferedReader b = new BufferedReader(f);
			String[] str;
			String line;

			while((line = b.readLine()) != null){
				//System.out.println(str);
				str = line.split(" ");//split line with space
				if(str[0].equals("PORT")){
					portNumber =  Integer.parseInt(str[1]);
					//	System.out.println(str);
					break;
				}
			}
			b.close();		
			System.out.println("Get Port: " + portNumber);
		}catch(Exception e){
			System.out.println(e);
		}
	}
}

