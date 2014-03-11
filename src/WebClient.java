import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;


public class WebClient {

	private int porta;
	
	public WebClient(int porta) {
		this.porta = porta;
	}
	
	
	private void sendGET() throws Exception {
		
		Socket servidor = new Socket("localhost",porta);
		
		int id = (int) (Math.random() * 7);
			
		PrintStream ps = new PrintStream(servidor.getOutputStream());
		InputStream is = servidor.getInputStream();
		BufferedReader reader=new BufferedReader(new InputStreamReader(is));
		String str="";
		
		
		
		//Enviando requisi��o do tipo GET
		ps.println("GET /resources/2013-2/test" + id + ".txt HTTP/1.0\r\n");
		while((str=reader.readLine())!=null) {
			System.out.println(str);
		}
		
		servidor.close();
		
	}
	private void sendCGI() throws Exception {
		
		Socket servidor = new Socket("localhost",porta);
			
		PrintStream ps = new PrintStream(servidor.getOutputStream());
		InputStream is = servidor.getInputStream();
		BufferedReader reader=new BufferedReader(new InputStreamReader(is));
		String str="";	
		
		//Enviando requisi��o do tipo CGI
		//ps.println("CGI br.ufpb.ci.so.p20132.HelloWorld HTTP/1.0\r\n");
		
		ps.println("CGI br.ufpb.ci.so.p20132.SimpleSort HTTP/1.0\r\n");
		while((str=reader.readLine())!=null) {
			System.out.println(str);
		}
		
		servidor.close();
	}
	
	

	/**
	 * M�todo para testar a funcionalidade do servidor
	 * Envia repetidamente um conjunto alternado de requisições GET e CGI e imprime o resultado na sa�da padr�o.
	 */
	public void run() {
		
		while (true) {
			
			try {
				Thread.sleep(100);
				sendGET();
			} catch (Exception e1) {
				
				e1.printStackTrace();
			}
			
			try {
				Thread.sleep(100);
				sendCGI();
			} catch (Exception e1) {
				
				e1.printStackTrace();
			}
			
			
			
		}
		
	}
	
	public static void main( String args[]) throws UnknownHostException, IOException {
		
        for (int i = 0; i < args.length; i++)
            System.out.println(args[i]);
		
		WebClient client = new WebClient(6789);
		client.run();
		
	}
	
}
