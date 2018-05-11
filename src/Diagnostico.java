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
		PreparedStatement p = null;
		try {
			if(conn==null) {
				conectar();
			}
			
			PreparedStatement pst = conn.prepareStatement("DROP DATABASE diagnostico;");
			pst.executeUpdate();
			PreparedStatement ps = conn.prepareStatement("CREATE SCHEMA diagnostico;");
			ps.executeUpdate();

			//CREACION DE TABLAS

			// Tabla disease:

			String disease = "CREATE TABLE diagnostico.disease (disease_id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) UNIQUE)";
			p = conn.prepareStatement(disease);
			p.executeUpdate();
			p.close();	
			

			// Tabla symptom:
			String symptom ="CREATE TABLE diagnostico.symptom (cui VARCHAR(25) PRIMARY KEY, name VARCHAR(255) UNIQUE)";
			p = conn.prepareStatement(symptom);
			p.executeUpdate();
			p.close();	

			// Tabla source
			String source = "CREATE TABLE diagnostico.source (source_id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) UNIQUE)";
			p = conn.prepareStatement(source);
			p.executeUpdate();
			p.close();	

			// Tabla code
			String code="CREATE TABLE diagnostico.code (code VARCHAR(255), source_id INT, " +
					"PRIMARY KEY (code, source_id), " +
					"FOREIGN KEY (source_id) REFERENCES source(source_id) ON UPDATE RESTRICT ON DELETE RESTRICT)";
			p = conn.prepareStatement(code);
			p.executeUpdate();
			p.close();	

			// Tabla semantic_type
			String semantic_type = "CREATE TABLE diagnostico.semantic_type (semantic_type_id INT PRIMARY KEY AUTO_INCREMENT,cui VARCHAR(45) UNIQUE)";
			p = conn.prepareStatement(semantic_type);
			p.executeUpdate();
			p.close();	

			// Tabla symptom_semantic_type
			String symptom_semantic_type = "CREATE TABLE diagnostico.symptom_semantic_type (cui VARCHAR(25), semantic_type_id INT, " +
					"PRIMARY KEY (cui, semantic_type_id), " +
					"FOREIGN KEY (cui) REFERENCES symptom(cui) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
					"FOREIGN KEY (semantic_type_id) REFERENCES semantic_type(semantic_type_id) ON UPDATE RESTRICT ON DELETE RESTRICT)";
			p = conn.prepareStatement(symptom_semantic_type);
			p.executeUpdate();
			p.close();	
			

			// Tabla disease_symptom
			String disease_symptom = "CREATE TABLE diagnostico.disease_symptom (disease_id INT, cui VARCHAR(25)," +
					"PRIMARY KEY (disease_id, cui)," +
					"FOREIGN KEY (disease_id) REFERENCES disease(disease_id) ON UPDATE RESTRICT ON DELETE RESTRICT," +
					"FOREIGN KEY (cui) REFERENCES symptom(cui) ON UPDATE RESTRICT ON DELETE RESTRICT)";
			p = conn.prepareStatement(disease_symptom);
			p.executeUpdate();
			p.close();	



			//Tabla disease_has_code
			String disease_has_code = "CREATE TABLE diagnostico.disease_has_code (disease_id INT, code VARCHAR(255), source_id INT, " +
					"PRIMARY KEY (disease_id, code, source_id), " +
					"FOREIGN KEY (disease_id) REFERENCES disease(disease_id) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
					"FOREIGN KEY (code) REFERENCES code(code) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
					"FOREIGN KEY (source_id) REFERENCES code(source_id) ON UPDATE RESTRICT ON DELETE RESTRICT)";
			p = conn.prepareStatement(disease_has_code);
			p.executeUpdate();
			p.close();	

			
			//Obtencion de los datos segun DATA
			
			LinkedList<String> list = readData();

			String []codVoc = null;
			LinkedList<String> sourceAnadidos = new LinkedList<String>();
			LinkedList<String> codeAnadidos = new LinkedList<String>();
			HashMap<Integer,String> souAnad = new HashMap<Integer,String>();
			HashMap<String,Integer> souAnadKey = new HashMap<String,Integer>();
			HashMap<Integer,String> semAdded = new HashMap<Integer,String>();
			HashMap<String,Integer> semAddedKey = new HashMap<String,Integer>();
			HashMap<Integer,String> sinAdded = new HashMap<Integer,String>();
			HashMap<Integer,String> sstAdded = new HashMap<Integer,String>();
			
			
			
			for(int i=0; i<list.size()-1;i++) {
				HashMap<Integer,String> dSAdded = new HashMap<Integer,String>();
				String []enfSint = null;
				String aDividir = list.get(i);
				enfSint = aDividir.split("=",2);

				String enfermedad=enfSint[0].split(":")[0];
				PreparedStatement pstenf = conn.prepareCall("INSERT INTO `diagnostico`.`disease` (`disease_id`, `name`) VALUES (?,?);");
				pstenf.setInt(1, i+1);
				pstenf.setString(2, enfermedad);
				pstenf.executeUpdate();
				pstenf.close();

				String sintomas = enfSint[0].split(":")[1];
				codVoc = sintomas.split(";");
				
				for(int j=0;j<codVoc.length-1;j++){

					String cod = codVoc[j].split("@")[0];
					String voc = codVoc[j].split("@")[1];
					if(!sourceAnadidos.contains(voc)) {
						PreparedStatement pstSource = conn.prepareCall("INSERT INTO `diagnostico`.`source` (`source_id`, `name`) VALUES (?,?);");
						pstSource.setInt(1, sourceAnadidos.size()+1);
						pstSource.setString(2, voc);
						pstSource.executeUpdate();
						pstSource.close();
						sourceAnadidos.add(voc);
						souAnad.put(souAnad.size()+1, voc);
						souAnadKey.put(voc, souAnad.size());
						
					}
					
					if(!codeAnadidos.contains(cod)) {

						int id = souAnadKey.get(voc);
						PreparedStatement pstCode = conn.prepareCall("INSERT INTO `diagnostico`.`code` (`code`, `source_id`) VALUES (?,?);");
						pstCode.setString(1, cod);
						pstCode.setInt(2, id);
						pstCode.executeUpdate();
						pstCode.close();
						codeAnadidos.add(cod);
						PreparedStatement pstDHC = conn.prepareCall("INSERT INTO `diagnostico`.`disease_has_code` (`disease_id`, `code`, `source_id`) VALUES (?,?,?);");
						pstDHC.setInt(1, 1);
						pstDHC.setString(2, cod);
						pstDHC.setInt(3, id);
						pstDHC.executeUpdate();
						pstDHC.close();
					}
				}
				enfSint = aDividir.split("=",2);
				String []sinCodSem=enfSint[1].split(";");

				
				for(int k=0;k<sinCodSem.length-1;k++){

					String sin = sinCodSem[k].split(":")[0];

					String cod = sinCodSem[k].split(":")[1];

					String sem = sinCodSem[k].split(":")[2];

					


					if(!semAdded.containsValue(sem)) {
						PreparedStatement pstSem = conn.prepareCall("INSERT INTO `diagnostico`.`semantic_type` (`semantic_type_id`, `cui`) VALUES (?,?);");
						pstSem.setInt(1, semAdded.size()+1);
						pstSem.setString(2, sem);
						pstSem.executeUpdate();
						pstSem.close();
						semAdded.put(semAdded.size()+1, sem);
						semAddedKey.put(sem, semAdded.size());
						
					}
					
					if(!sinAdded.containsValue(sem)) {
						PreparedStatement pstSin = conn.prepareCall("INSERT INTO `diagnostico`.`symptom` (`cui`, `name`) VALUES (?,?);");
						pstSin.setString(1, sem);
						pstSin.setString(2, sin);
						pstSin.executeUpdate();
						pstSin.close();
						sinAdded.put(sinAdded.size()+1, sem);
					}
					if(!sstAdded.containsValue(sem)) {
					int id = semAddedKey.get(sem);
					PreparedStatement pstSST = conn.prepareCall("INSERT INTO `diagnostico`.`symptom_semantic_type` (`cui`, `semantic_type_id`) VALUES (?,?);");
					pstSST.setString(1, sem);
					pstSST.setInt(2, id);
					pstSST.executeUpdate();
					pstSST.close();
					sstAdded.put(sstAdded.size()+1, sem);
					}
					if(!dSAdded.containsValue(sem)) {
						PreparedStatement pstDS = conn.prepareCall("INSERT INTO `diagnostico`.`disease_symptom` (`disease_id`, `cui`) VALUES (?,?);");
						pstDS.setInt(1, i+1);
						pstDS.setString(2, sem);
						pstDS.executeUpdate();
						pstDS.close();
						dSAdded.put(dSAdded.size()+1, sem);
					}
				}
				
				
			}

						
		}catch(SQLException ex) {
			System.err.println(ex.getMessage());
		}

	}

		private void realizarDiagnostico() {
		// implementar
			
		int option = -1;
			
			do {
				diagnosticoAux();
				System.out.println("\tPor favor, introduzca el codigo asociado de los sintomas que padezca.\n\tPara salir del menú de opciones pulse 0");

				try {
					Statement st = connection.createStatement();
					option = readInt();

					switch (option) {
					
					default:
						ResultSet rs = st.executeQuery("SELECT EN.nombre FROM enfermedad EN, trata TR, medicamento M"
								+ " WHERE EN.id = TR.id_enfermedad AND M.id = TR.id_medicamento AND M.id ="+option);

						System.out.println("\n\tLa enfermedad introducida consta de los siguientes sintomas:\n");

						while (rs.next()) {
							String enfermedades = rs.getString("EN.nombre");
							System.out.println("\t " + enfermedades);
						}

						System.out.println("\n");
						st.close();
						break;
					}


				} catch (Exception e) {
					System.err.println("Opción introducida no válida!");
				}
			}
			while (option != 0);
	}
	
	private void diagnosticoAux(){
		
			if(connection==null){
			conectar();
		}

		try{
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT  id, nombre FROM sintoma");
			System.out.println("\n\tSintomas: \n");

			while (rs.next()) {
				int id = rs.getInt("id");
				String nombre = rs.getString("nombre");
				System.out.println("\tID: " + id + "\Sintoma: " + nombre);
			}

			System.out.println("\n");
			st.close();
		}

		catch(Exception e){
			System.err.println("Error al seleccionar a la BD: " + e.getMessage());
		}
		
	}

	private void listarSintomasEnfermedad() {
		// implementar
		
		int option = -1;
		
		do {
			sintomasEnfermedadAux();
			System.out.println("\tPor favor, introduzca el ID de la enfermedad.\n\tPara salir del menú de opciones pulse 0");

			try {
				Statement st = connection.createStatement();
				option = readInt();

				switch (option) {
				
				default:
					ResultSet rs = st.executeQuery("SELECT name FROM disease");

					System.out.println("\n\tLa enfermedad introducida consta de los siguientes sintomas:\n");

					while (rs.next()) {
						String sintomas = rs.getString("name");
						System.out.println("\t " + sintomas);
					}

					System.out.println("\n");
					st.close();
					break;
				}


			} catch (Exception e) {
				System.err.println("Opción introducida no válida!");
			}
		}
		while (option != 0);
}

	private void sintomasEnfermedadAux(){
		
		if(connection==null){
			conectar();
		}

		try{
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT  disease_id, name, FROM disease +");
			//		+ " WHERE EN.id = SIN.id");
			System.out.println("\n\tEnfermedades: \n");

			while (rs.next()) {
				int id = rs.getInt("disease_id");
				String nombre = rs.getString("name");
				System.out.println("\tID: " + id + "\n\tEnfermedad: " + nombre + "\n");
			}

			st.close();
		}
		catch(Exception e){
			System.err.println("Error al seleccionar a la BD: " + e.getMessage());
		}
		
		
	}
	private void listarEnfermedadesYCodigosAsociados() {
		// implementar

			if(connection==null){
			conectar();
		}

		try{
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT  disease_id, name, FROM disease +");
			//		+ " WHERE EN.id = SIN.id");
			System.out.println("\n\tEnfermedades: \n");

			while (rs.next()) {
				int id = rs.getInt("disease_id");
				String nombre = rs.getString("name");
				System.out.println("\tID: " + id + "\n\tEnfermedad: " + nombre + "\n");
			}

			st.close();
		}
		catch(Exception e){
			System.err.println("Error al seleccionar a la BD: " + e.getMessage());
		}
	}

	private void listarSintomasYTiposSemanticos() {
		// implementar
		
		
		if(connection==null){
			conectar();
		}

		try{
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT  EN.id, EN.nombre, SIN.nombre FROM enfermedad EN , sintomas SIN WHERE EN.id = SIN.id");
			System.out.println("\n\tEnfermedades: \n");

			while (rs.next()) {
				int id = rs.getInt("EN.id");
				String nombre = rs.getString("EN.nombre");
				String sintomas = rs.getString("SIN.nombre");
				System.out.println("\tID: " + id + "\n\tEnfermedad: " + nombre + "\n\tSintomas: " + sintomas + "\n");
			}

			st.close();
		}
		catch(Exception e){
			System.err.println("Error al seleccionar a la BD: " + e.getMessage());
		}
	}

	private void mostrarEstadisticasBD() {
		// implementar
		
				
		if(connection==null){
			conectar();
		}
		
		try{
			
			int contadorEnfermedades = 0;
			String numEnfermedades= "SELECT (disease.disease_id)"
					+ "FROM Disease;";
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(numEnfermedades);
			
		
			while(rs.next()) {
				String numero = rs.getString(1);
				contadorEnfermedades++;
			}
			
			System.out.println("El numero de enfermedades es: "+ contadorEnfermedades);
			
			int contadorSintomas = 0;
			String numSintomas= "SELECT (symptom.cui)"
					+ "FROM Symptom;";
			Statement st1 = connection.createStatement();
			ResultSet rs1 = st1.executeQuery(numSintomas);
			
			while(rs1.next()) {
				String numero = rs1.getString(1);
				contadorSintomas++;
			}
			
			System.out.println("El numero de sintomas es: " + contadorSintomas);
			
		}
		catch(SQLException ex){
			System.err.println(ex.getMessage());
		}
		
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
