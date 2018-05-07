import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.FileReader;

public class Diagnostico {

	private final String DATAFILE = "data/disease_data.data";
	
	private Connection connection = null;

	private void showMenu() {

		int option = -1;
		do {
			System.out.println("Bienvenido a sistema de diagn�stico\n");
			System.out.println("Selecciona una opci�n:\n");
			System.out.println("\t1. Creaci�n de base de datos y carga de datos.");
			System.out.println("\t2. Realizar diagn�stico.");
			System.out.println("\t3. Listar s�ntomas de una enfermedad.");
			System.out.println("\t4. Listar enfermedades y sus c�digos asociados.");
			System.out.println("\t5. Listar s�ntomas existentes en la BD y su tipo sem�ntico.");
			System.out.println("\t6. Mostrar estad�sticas de la base de datos.");
			System.out.println("\t7. Salir.");
			try {
				option = readInt();
				switch (option) {
				case 1:
					crearBD();
					break;
				case 2:
					realizarDiagnostico();
					break;
				case 3:
					listarSintomasEnfermedad();
					break;
				case 4:
					listarEnfermedadesYCodigosAsociados();
					break;
				case 5:
					listarSintomasYTiposSemanticos();
					break;
				case 6:
					mostrarEstadisticasBD();
					break;
				case 7:
					exit();
					break;
				}
			} catch (Exception e) {
				System.err.println("Opci�n introducida no v�lida!");
			}
		} while (option != 7);
		exit();
	}

	private void exit() {
		try{
			if (connection != null){
				connection.close();
			}

			System.out.println("Desonectado de la base de datos");
		}

		catch (Exception e){
			System.err.println("Error al desconectar de la BD: " + e.getMessage());
		}

		System.out.println("Saliendo.. ¡hasta otra!");
		System.exit(0);
	}

	private void conectar() {
		// implementar
		
		try{
			String drv = "com.mysql.jdbc.Driver";
			Class.forName(drv);
			String serverAddress = "localhost:3306";
			String db = "diagnostico";
			String user = "diagnostico";
			String pass = "diagnostico_pwd";
			String url = "jdbc:mysql://" + serverAddress + "/" + db;
			connection = DriverManager.getConnection(url, user, pass);
			System.out.println("Conectado a la base de datos!");
			connection.setAutoCommit(true);
			System.out.println("Conectado a la base de datos!");
		} catch (SQLException esql) {
			System.err.println("Mensaje: " + esql.getMessage());
			System.err.println("Codigo: " + esql.getErrorCode());
			System.err.println("Estado SQL: " + esql.getSQLState());
		} catch (ClassNotFoundException e) {
			System.err.println("Error al cargar el driver.");
		}
		
		catch (Exception e){
			System.err.println("Error al conectar a la BD: " + e.getMessage());
		}
	}

	private void crearBD() {
		// implementar
		if (connection==null){
			conectar();
		}

		for (String aux : readData()) {
			String[] parts = aux.split("=");
			String[] nameCode = parts[0].split(":");
			String name = nameCode[0];
			String[] codesVoc = nameCode[1].split(";");
			for (int i = 0; i < codesVoc.length; i++){
				String[] codeN = codesVoc[i].split("@");
				String codeDis = codeN[0];
				String vocDis = codeN[1];
			}
			String[] sintomas = parts[1].split(";");
			for (int j = 0; j < sintomas.length; j++){
				String[] codesSin = sintomas[j].split(":");
				String sint = codesSin[0];
				String codeSin = codesSin[1];
				String semSin = codesSin[2];
			}
		}


	}

	private void realizarDiagnostico() {
		// implementar
		if (connection == null){
			conectar();
		}
	}

	private void listarSintomasEnfermedad() {
		// implementar
	}

	private void listarEnfermedadesYCodigosAsociados() {
		// implementar
	}

	private void listarSintomasYTiposSemanticos() {
		// implementar
	}

	private void mostrarEstadisticasBD() {
		// implementar
	}

	/**
	 * M�todo para leer n�meros enteros de teclado.
	 * 
	 * @return Devuelve el n�mero le�do.
	 * @throws Exception
	 *             Puede lanzar excepci�n.
	 */
	private int readInt() throws Exception {
		try {
			System.out.print("> ");
			return Integer.parseInt(new BufferedReader(new InputStreamReader(System.in)).readLine());
		} catch (Exception e) {
			throw new Exception("Not number");
		}
	}

	/**
	 * M�todo para leer cadenas de teclado.
	 * 
	 * @return Devuelve la cadena le�da.
	 * @throws Exception
	 *             Puede lanzar excepci�n.
	 */
	private String readString() throws Exception {
		try {
			System.out.print("> ");
			return new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (Exception e) {
			throw new Exception("Error reading line");
		}
	}

	/**
	 * M�todo para leer el fichero que contiene los datos.
	 * 
	 * @return Devuelve una lista de String con el contenido.
	 * @throws Exception
	 *             Puede lanzar excepci�n.
	 */
	private LinkedList<String> readData() throws Exception {
		LinkedList<String> data = new LinkedList<String>();
		BufferedReader bL = new BufferedReader(new FileReader(DATAFILE));
		while (bL.ready()) {
			data.add(bL.readLine());
		}
		bL.close();
		return data;
	}

	public static void main(String args[]) {
		new Diagnostico().showMenu();
	}
}
