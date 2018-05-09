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
		String s;
		PreparedStatement p = null;
		try {
			if(connection==null) {
				conectar();
			}
			
			PreparedStatement pst = connection.prepareStatement("CREATE SCHEMA IF NOT EXISTS `diagnostico`  DEFAULT CHARACTER SET utf8;");
			pst.executeUpdate();

			/*s = "CREATE SCHEMA `diagnostico`;";
			p = connection.prepareStatement(s);
			p.executeUpdate();
			p.close();*/

			//CREACION DE TABLAS

			// Tabla disease:
			String disease = "CREATE TABLE IF NOT EXISTS `diagnostico`.`disease`("
					+ "`disease_id` INT NOT NULL," 
					+ "`name` VARCHAR(255) NOT NULL," 
					+ "PRIMARY KEY (`disease_id`));";
			p = connection.prepareStatement(disease);
			p.executeUpdate();
			p.close();	

			// Tabla symptom:
			String symptom ="CREATE TABLE IF NOT EXISTS `diagnostico`.`symptom` ("+
					"`cui` VARCHAR(25) NOT NULL," +
					"`name` VARCHAR(255) NOT NULL,"+
					"PRIMARY KEY (`cui`));";

			p = connection.prepareStatement(symptom);
			p.executeUpdate();
			p.close();	

			// Tabla source
			String source = "CREATE TABLE IF NOT EXISTS `diagnostico`.`source` ( "+
					"`source_id` INT NOT NULL," +
					"`name` VARCHAR(255) NOT NULL," + 
					"PRIMARY KEY (`source_id`));";
			p = connection.prepareStatement(source);
			p.executeUpdate();
			p.close();	

			// Tabla code
			String code="CREATE TABLE IF NOT EXISTS `diagnostico`.`code` ("+
					"`code` VARCHAR(255) NOT NULL,"+
					"`source_id_c` INT NOT NULL," +
					"PRIMARY KEY (`code`),"+
					"INDEX `source_id_c_idx` (`source_id_c` ASC)," +
					"CONSTRAINT `source_id_c`" +
					" FOREIGN KEY (`source_id_c`)" +
					" REFERENCES `diagnostico`.`source` (`source_id`)" +
					" ON DELETE NO ACTION"+
					" ON UPDATE NO ACTION);";
			p = connection.prepareStatement(code);
			p.executeUpdate();
			p.close();	

			// Tabla semantic_type
			String semantic_type = "CREATE TABLE IF NOT EXISTS `diagnostico`.`semantic_type` (" +
					"`semantic_type_id` INT NOT NULL," +
					"`cui` VARCHAR(45) NOT NULL," +
					"PRIMARY KEY (`semantic_type_id`));";
			p = connection.prepareStatement(semantic_type);
			p.executeUpdate();
			p.close();	

			// Tabla symptom_semantic_type
			String symptom_semantic_type = "CREATE TABLE IF NOT EXISTS `diagnostico`.`symptom_semantic_type` (" +
					"`cui_sst` VARCHAR(25) NOT NULL," +
					"`semantic_type_id_sst` INT NOT NULL," +
					"INDEX (`cui_sst` ASC)," +
					"INDEX `semantic_type_id_sst_idx` (`semantic_type_id_sst` ASC)," +
					" CONSTRAINT `cui_sst`"+
					" FOREIGN KEY (`cui_sst`)"+
					" REFERENCES `diagnostico`.`symptom` (`cui`)"+
					" ON DELETE NO ACTION"+
					" ON UPDATE NO ACTION,"+
					" CONSTRAINT `semantic_type_id_sst`"+
					" FOREIGN KEY (`semantic_type_id_sst`)"+
					" REFERENCES `diagnostico`.`semantic_type` (`semantic_type_id`)"+
					" ON DELETE NO ACTION"+
					" ON UPDATE NO ACTION);";
			p = connection.prepareStatement(symptom_semantic_type);
			p.executeUpdate();
			p.close();	

			// Tabla disease_symptom
			String disease_symptom = "CREATE TABLE IF NOT EXISTS `diagnostico`.`disease_symptom` (" +
					"`disease_id_ds` INT NOT NULL," +
					"`cui_ds` VARCHAR(45) NOT NULL," +
					"INDEX `disease_id_ds_idx` (`disease_id_ds` ASC)," +
					"INDEX `cui_ds_idx` (`cui_ds` ASC)," +
					" CONSTRAINT `disease_id_ds`" +
					" FOREIGN KEY (`disease_id_ds`)" +
					" REFERENCES `diagnostico`.`disease` (`disease_id`)"+
					" ON DELETE NO ACTION"+
					" ON UPDATE NO ACTION,"+
					" CONSTRAINT `cui_ds`" +
					" FOREIGN KEY (`cui_ds`)" +
					" REFERENCES `diagnostico`.`symptom` (`cui`)" +
					" ON DELETE NO ACTION" +
					" ON UPDATE NO ACTION);";
			p = connection.prepareStatement(disease_symptom);
			p.executeUpdate();
			p.close();	



			//Tabla disease_has_code
			String disease_has_code = "CREATE TABLE IF NOT EXISTS `diagnostico`.`disease_has_code` (" +
					"`disease_id_dhc` INT NULL," +
					"`code_dhc` VARCHAR(255) NULL," +
					"`source_id_dhc` INT NULL," +
					"INDEX `disease_id_dhc_idx` (`disease_id_dhc` ASC),"+
					"INDEX `code_dhc_idx` (`code_dhc` ASC)," +
					"INDEX `source_id_dhc_idx` (`source_id_dhc` ASC),"+
					" CONSTRAINT `disease_id_dhc`" +
					" FOREIGN KEY (`disease_id_dhc`)"+
					" REFERENCES `diagnostico`.`disease` (`disease_id`)" +
					" ON DELETE NO ACTION"+
					" ON UPDATE NO ACTION," +
					" CONSTRAINT `code_dhc`" +
					" FOREIGN KEY (`code_dhc`)"+
					" REFERENCES `diagnostico`.`code` (`code`)"+
					" ON DELETE NO ACTION" +
					" ON UPDATE NO ACTION,"+
					" CONSTRAINT `source_id_dhc`"+
					" FOREIGN KEY (`source_id_dhc`)" +
					" REFERENCES `diagnostico`.`source` (`source_id`)"+
					" ON DELETE NO ACTION"+
					" ON UPDATE NO ACTION);";
			p = connection.prepareStatement(disease_has_code);
			p.executeUpdate();
			p.close();	



			//Obtencion de los datos a traves del archivo DATA

						
						LinkedList<String> list = readData();
						String []enfermedades;
						String []codVoc;
						String []codigo;
						String[]enfSint;//array de enfermedades y sintomas
			
			
						for (int i = 0; i < list.size(); i++) {
							enfSint = list.get(i).split("=",2);
							//COMIENZO PARTE IZQUIERDA ARBOL
							enfermedades=enfSint[0].split(":");
							codVoc=enfermedades[1].split(";");
			
							for(int j=0;j<codVoc.length;j++){
								//conseguimos codigos y vocabularios
								codigo=codVoc[j].split("@");
							}
							//FIN PARTE IZQUIERDA ARBOL
			
							//COMIENZO PARTE DERECHA ARBOL
							String [] sintomas;
							String [] elementos;
							sintomas = enfSint[1].split(";");
							for (int j=0; j<sintomas.length;j++){
								System.out.println(sintomas[j]);
								elementos= sintomas[j].split(":");
								
						}
							
							
			/*
			 * A la salida de los bucles, los datos se distribuyen:
			 * 		enfermedades = 	contiene el nombre de todas las enfermedades (0-10 --> 11 enfermedades)
			 * 		codVoc = 		tiene el codigo y el vocabulario de cada enfermedad.
			 * 		codigo = 		posiciones pares: codigo
			 * 				 		posiciones impares: vocabulario
			 * 		elementos =		(diferencia entre elementos = 3)
			 * 						contiene el sintoma, su codigo y tipo semantico. 
			 * 
			 * Cada iteración ira haciendo esta separaciones por lo que debemos introducir en cada una
			 * de las tablas los datos necesarios de cada array.
			 */

						}
			
			
						
		}catch(SQLException ex) {
			System.err.println(ex.getMessage());
		}

	}

		private void realizarDiagnostico() {
		// implementar
		if (connection == null){
			conectar();
		}
	}
	
	private void diagnosticoAux(){
		
		if (connection == null){
			conectar();
		}
		
	}

	private void listarSintomasEnfermedad() {
		// implementar
		
		int option = -1;
		
		do {
			sintomasEnfermedadAux();
			System.out.println("\tPor favor, introduzca la enfermedad.\n\tPara salir del menú de opciones pulse 0");

			try {
				Statement st = connection.createStatement();
				option = readInt();

				switch (option) {
				
				default:
					ResultSet rs = st.executeQuery("SELECT EN.nombre FROM enfermedad EN, trata TR, medicamento M"
							+ " WHERE EN.id = TR.id_enfermedad AND M.id = TR.id_medicamento AND M.id ="+option);

					System.out.println("\n\tLa enfermedad introducida consta de los siguientes sintomas:\n");

					while (rs.next()) {
						String sintomas = rs.getString("EN.nombre");
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
		
		if (connection == null){
			conectar();
		}
		
		try{
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT  id, nombre FROM medicamento");
			System.out.println("\n\tMedicamentos: \n");

			while (rs.next()) {
				int id = rs.getInt("id");
				String nombre = rs.getString("nombre");
				System.out.println("\tID: " + id + "\tFármaco: " + nombre);
			}

			System.out.println("\n");
			st.close();
		}
		catch(Exception e){
			System.err.println("Error al seleccionar a la BD: " + e.getMessage());
		}
		
	}
	private void listarEnfermedadesYCodigosAsociados() {
		// implementar
	}

	private void enfermedadesAux(){
		
		if (connection == null){
			conectar();
		}
		
	}

	private void listarSintomasYTiposSemanticos() {
		// implementar
	}
	
	private void enfermedadesAux(){
		
		if (connection == null){
			conectar();
		}
		
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
