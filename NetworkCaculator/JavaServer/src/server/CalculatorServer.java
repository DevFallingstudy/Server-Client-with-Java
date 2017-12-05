package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class CalculatorServer {
	private static int mPort = 5508;

	public static void main(String[] args) throws IOException {
		System.out.println("Server is Running...");
		ServerSocket mySock = new ServerSocket(mPort);
		while (true) {
			Socket sock = mySock.accept();
			MyCalServer server = new MyCalServer(sock);

			Thread serverThread = new Thread(server);
			serverThread.start();
		}
	}
}

class MyCalServer implements Runnable {
	Socket sock;

	public MyCalServer(Socket s) {
		try {
			System.out.println("# SYSTEM # HOST CONNECTED!");
			this.sock = s;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		BufferedReader reader = null;
		BufferedWriter writer = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			
			while(true) {
				System.out.println("Waiting connection...");
				String str_formula = reader.readLine();
				System.out.println(str_formula);
				if (str_formula == null) {
					Thread.currentThread().interrupt();
					return;
				}else if (str_formula.equals("CONNECTED")){
					break;
				}else {
					return;
				}
			}
			
			while (true) {
				System.out.println("Waiting formula...");
				String str_formula = reader.readLine();
				
				if (str_formula == null || str_formula.trim().isEmpty()) {
					break;
					
				}
				str_formula = str_formula.trim();
				if (str_formula.equals("EXIT")) {
					sock.close();
					break;
				} else {
					String result = calculateFormula(str_formula);
					writer.write(result);
					writer.flush();
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private String calculateFormula(String formula) {
		// System.out.println(formula);
		try {
			String[] str_arr = formula.trim().split(" ");
			System.out.println("Cal start!");
			if (str_arr.length != 3) {
				System.out.println("ERROR LENGTH" + str_arr.length);
				return "ERROR LENGTH\n";
			} else {
				double first = Double.parseDouble(str_arr[0]);
				double second = Double.parseDouble(str_arr[2]);
				String op = str_arr[1];
				double result = 0.0;

				if (op.equals("+")) {
					result = first + second;
				} else if (op.equals("-")) {
					result = first - second;
				} else if (op.equals("*")) {
					result = first * second;
				} else if (op.equals("/")) {
					if (second == 0d) {
						System.out.println("ERROR ZERO");
						return "ERROR ZERO\n";
					} else {
						result = first / second;
					}
				} else {
					System.out.println("ERROR OP");
					return "ERROR OP\n";
				}

				System.out.println("RESULT " + result);
				result = Math.round(result * 100d) / 100d;
				return "RESULT " + result + "\n";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "NONE";
		}
	}
}
