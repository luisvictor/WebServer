import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;

/**
 * Um servidor Web simples porém totalmente funcional.
 * O servidor atende apenas requisições do tipo GET ou CGI.
 * @author Alexandre Duarte - alexandre@ci.ufpb.br
 *
 */
public class WebServer extends Thread {
	
	private ServerSocket socketEscuta;
	
	private static int numeroRequisicao = 0;
	
	int tipo = 0;

	private Semaphore semaphoreBufferVazio;

	private Semaphore semaphoreBufferCheio;
	
	private Semaphore mutex;

	private int tamBuffer;

	private int aux;
	
	static int contadorW = 0;
	static Socket[] reqSocket;
	

	public WebServer( int porta, Socket[] reqSocket, int tamBuffer, Semaphore semaphoreBufferVazio, Semaphore semaphoreBufferCheio) throws Exception {
		this.tipo = 1;
		this.semaphoreBufferVazio = semaphoreBufferVazio;
		this.semaphoreBufferCheio = semaphoreBufferCheio;
		this.tamBuffer = tamBuffer;
		socketEscuta = new ServerSocket(porta);//cria um objeto ServerSocket
		
	}
	public WebServer(Socket[] reqSocket, int tamBuffer, Semaphore semaphoreBufferVazio, Semaphore semaphoreBufferCheio,Semaphore mutexC) throws Exception {
		
		this.tipo = 2;
		this.semaphoreBufferVazio = semaphoreBufferVazio;
		this.semaphoreBufferCheio = semaphoreBufferCheio;
		this.mutex = mutexC;
		this.tamBuffer = tamBuffer;
		
		
	}
	
	public void run () {
		
		if (tipo == 1){
			int contador = 0;
			System.out.printf("WEB Server %d \n", this.getId());
			while( true ) {			
	
				try {
					semaphoreBufferVazio.acquire();
					System.out.printf("WEB Server %d buffer acquired contador = %d \n", this.getId(), contador);
					reqSocket[contador] = this.socketEscuta.accept();
					contador = (contador+1)%tamBuffer;
					semaphoreBufferCheio.release();
					System.out.printf("WEB Server %d buffer released contador = %d \n", this.getId(), contador);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}//espera até uma conexão ser estabelecida e retorna um socket quando a conexão é estabelecida
				

					
			}
		}
		if(tipo==2){//worker
			
			System.out.printf("Worker %d \n", this.getId());
			while(true){
				try {
					
					semaphoreBufferCheio.acquire();					
						mutex.acquire();
							this.aux = contadorW;
							contadorW = (contadorW+1)%tamBuffer;
						mutex.release();
						System.out.printf("worker %d buffer acquired count = %d \n", this.getId(), this.aux);
						processaRequisicao(reqSocket[this.aux]);
					semaphoreBufferVazio.release();
					
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
			}
		}
	}
		
	
	/*processaRequisicao() 
	 * recebe um socket como parametro
	 */
	private void processaRequisicao( Socket reqSocket) throws IOException {
			
		BufferedReader doCliente = new BufferedReader(new InputStreamReader( reqSocket.getInputStream()));
		DataOutputStream paraCliente = new DataOutputStream( reqSocket.getOutputStream());
		String requisicao = doCliente.readLine();
		
		
		String req = new String("Requisicao numero [" + numeroRequisicao++ + "] = " + "\"" + requisicao + "\"");
		System.out.println( req );
		
		StringTokenizer st = new StringTokenizer(requisicao);
		String tipo = st.nextToken();
		byte[] bytes = null;
		
		
		if(tipo.equals("GET")) {
			
			try {
			
				File arquivo = new File(st.nextToken().substring(1));
	
				FileInputStream leitor = new FileInputStream (arquivo);
				bytes = new byte[(int)arquivo.length()];
				leitor.read(bytes);
				leitor.close();
			} catch( IOException e ) {
				bytes = e.getMessage().getBytes();	
			}
		} else if( tipo.equals("CGI")) {
			
			Process p = Runtime.getRuntime().exec("java -classpath bin " + st.nextToken());
			BufferedReader b = new BufferedReader( new InputStreamReader( p.getInputStream()));
			
			StringBuffer sb = new StringBuffer();
			String l;
			while((l = b.readLine())!= null) {
					sb.append(l);
					sb.append("\n");
			}
			
			bytes = sb.toString().getBytes();
			b.close();
		}
		
			
		paraCliente.writeBytes("HTTP/1.0 200 Document Follows\r\n");
		paraCliente.writeBytes("Content-Length " + bytes.length + "\r\n");
			
			
		paraCliente.writeBytes("Content-Length " + bytes.length + "\r\n");
			
		//Retorno das estatísticas da requisição
		paraCliente.writeBytes("id-requisicao " + 1 + "\r\n");
		paraCliente.writeBytes("tempo-chegada-requisicao " + 2 + "\r\n");
		paraCliente.writeBytes("cont-requisicao-agendada " + 3 + "\r\n");
		paraCliente.writeBytes("tempo-agendamento-requisicao " + 4  + "\r\n");
		paraCliente.writeBytes("cont-requisicao-concluida " + 5  + "\r\n");
		paraCliente.writeBytes("tempo-requisicao-concluida " + 6  + "\r\n");
		paraCliente.writeBytes("idade-requisicao " + 7 + "\r\n");
		paraCliente.writeBytes("tipo-requisicao " + tipo  + "\r\n");
			
		//Retorno das estatísticas do thread
		paraCliente.writeBytes("ida-thread " + 8 + "\r\n");
		paraCliente.writeBytes("cont-thread " + 9 + "\r\n");
			
		paraCliente.writeBytes("\r\n\n");
		
		paraCliente.write(bytes, 0, bytes.length);
		
		reqSocket.close();
		
	}
	
	
	public static void main(final String argv[]) throws Exception {
		//salvando argumentos
        int porta = Integer.parseInt(argv[0]);
        int numWorkers = Integer.parseInt(argv[1]);
        int tamBufferG = Integer.parseInt(argv[2]);
        String algEscalonamento = new String();
        algEscalonamento = argv[3];

        
        reqSocket = new Socket[tamBufferG];
        Semaphore semaphoreBufferVazio = new Semaphore(tamBufferG);
        Semaphore semaphoreBufferCheio = new Semaphore(0);
        Semaphore mutex = new Semaphore(1);
		
		System.out.println( "Iniciando o servidor...");
		
		WebServer servidor = new WebServer(porta,reqSocket,tamBufferG,semaphoreBufferVazio,semaphoreBufferCheio);
		WebServer worker[] = new WebServer[numWorkers];
		
		System.out.println( "Servidor no ar. Aguardando requisições.");
		
		
		servidor.start();
		for(int x = 0;x<numWorkers;x++){
			worker[x] = new WebServer(reqSocket,tamBufferG,semaphoreBufferVazio,semaphoreBufferCheio,mutex);
			worker[x].start();
		}
		System.out.println( "Servidor finalizando.");
		
	}
}
