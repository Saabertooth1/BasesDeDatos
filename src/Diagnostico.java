import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

public class Diagnostico {

    private final String DATAFILE = "data/disease_data.data";

    private Connection connection = null;

    public static void main(String args[]) {
        new Diagnostico().showMenu();
    }

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
        try {
            if (connection != null) {
                connection.close();
            }

            System.out.println("Desonectado de la base de datos");
        } catch (Exception e) {
            System.err.println("Error al desconectar de la BD: " + e.getMessage());
        }

        System.out.println("Saliendo.. ¡hasta otra!");
        System.exit(0);
    }

    private void conectar() {
        // implementar

        try {
            String drv = "com.mysql.jdbc.Driver";
            Class.forName(drv);
            String serverAddress = "localhost:3306";
            String db = "diagnostico";
            String user = "bddx";
            String pass = "bddx_pwd";
            String url = "jdbc:mysql://" + serverAddress + "/" + db;
            connection = DriverManager.getConnection(url, user, pass);
            connection.setAutoCommit(true);
            System.out.println("Conectado a la base de datos!");
        } catch (SQLException esql) {
            System.err.println("Mensaje: " + esql.getMessage());
            System.err.println("Codigo: " + esql.getErrorCode());
            System.err.println("Estado SQL: " + esql.getSQLState());
        } catch (ClassNotFoundException e) {
            System.err.println("Error al cargar el driver.");
        } catch (Exception e) {
            System.err.println("Error al conectar a la BD: " + e.getMessage());
        }
    }

    private void crearBD() {
        PreparedStatement p = null;
        try {
            if (connection == null) {
                conectar();
            }

            PreparedStatement pst = connection.prepareStatement("DROP SCHEMA diagnostico;");
            pst.executeUpdate();
            PreparedStatement ps = connection.prepareStatement("CREATE SCHEMA diagnostico;");
            ps.executeUpdate();

            //CREACION DE TABLAS

            // Tabla disease:

            String disease = "CREATE TABLE diagnostico.disease (disease_id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) UNIQUE)";
            p = connection.prepareStatement(disease);
            p.executeUpdate();
            p.close();


            // Tabla symptom:
            String symptom = "CREATE TABLE diagnostico.symptom (cui VARCHAR(25) PRIMARY KEY, name VARCHAR(255) UNIQUE)";
            p = connection.prepareStatement(symptom);
            p.executeUpdate();
            p.close();

            // Tabla source
            String source = "CREATE TABLE diagnostico.source (source_id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) UNIQUE)";
            p = connection.prepareStatement(source);
            p.executeUpdate();
            p.close();

            // Tabla code
            String code = "CREATE TABLE diagnostico.code (code VARCHAR(255), source_id INT, " +
                    "PRIMARY KEY (code, source_id), " +
                    "FOREIGN KEY (source_id) REFERENCES source(source_id) ON UPDATE RESTRICT ON DELETE RESTRICT)";
            p = connection.prepareStatement(code);
            p.executeUpdate();
            p.close();

            // Tabla semantic_type
            String semantic_type = "CREATE TABLE diagnostico.semantic_type (semantic_type_id INT PRIMARY KEY AUTO_INCREMENT,cui VARCHAR(45) UNIQUE)";
            p = connection.prepareStatement(semantic_type);
            p.executeUpdate();
            p.close();

            // Tabla symptom_semantic_type
            String symptom_semantic_type = "CREATE TABLE diagnostico.symptom_semantic_type (cui VARCHAR(25), semantic_type_id INT, " +
                    "PRIMARY KEY (cui, semantic_type_id), " +
                    "FOREIGN KEY (cui) REFERENCES symptom(cui) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
                    "FOREIGN KEY (semantic_type_id) REFERENCES semantic_type(semantic_type_id) ON UPDATE RESTRICT ON DELETE RESTRICT)";
            p = connection.prepareStatement(symptom_semantic_type);
            p.executeUpdate();
            p.close();


            // Tabla disease_symptom
            String disease_symptom = "CREATE TABLE diagnostico.disease_symptom (disease_id INT, cui VARCHAR(25)," +
                    "PRIMARY KEY (disease_id, cui)," +
                    "FOREIGN KEY (disease_id) REFERENCES disease(disease_id) ON UPDATE RESTRICT ON DELETE RESTRICT," +
                    "FOREIGN KEY (cui) REFERENCES symptom(cui) ON UPDATE RESTRICT ON DELETE RESTRICT)";
            p = connection.prepareStatement(disease_symptom);
            p.executeUpdate();
            p.close();


            //Tabla disease_has_code
            String disease_has_code = "CREATE TABLE diagnostico.disease_has_code (disease_id INT, code VARCHAR(255), source_id INT, " +
                    "PRIMARY KEY (disease_id, code, source_id), " +
                    "FOREIGN KEY (disease_id) REFERENCES disease(disease_id) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
                    "FOREIGN KEY (code) REFERENCES code(code) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
                    "FOREIGN KEY (source_id) REFERENCES code(source_id) ON UPDATE RESTRICT ON DELETE RESTRICT)";
            p = connection.prepareStatement(disease_has_code);
            p.executeUpdate();
            p.close();


            //Obtencion de los datos segun DATA

            LinkedList<String> list = readData();

            String[] codVoc = null;
            LinkedList<String> sourceAnadidos = new LinkedList<String>();
            LinkedList<String> codeAnadidos = new LinkedList<String>();
            HashMap<Integer, String> souAnad = new HashMap<Integer, String>();
            HashMap<String, Integer> souAnadKey = new HashMap<String, Integer>();

            HashMap<Integer, String> semAdded = new HashMap<Integer, String>();
            HashMap<String, Integer> semAddedKey = new HashMap<String, Integer>();
            HashMap<Integer, String> sinAdded = new HashMap<Integer, String>();
            HashMap<Integer, String> sstAdded = new HashMap<Integer, String>();


            for (int i = 0; i < list.size(); i++) {
                HashMap<Integer, String> dSAdded = new HashMap<Integer, String>();

                String[] enfSint = null;
                String aDividir = list.get(i);
                enfSint = aDividir.split("=", 2);

                String enfermedad = enfSint[0].split(":")[0];
                PreparedStatement pstenf = connection.prepareCall("INSERT INTO `diagnostico`.`disease` (`disease_id`, `name`) VALUES (?,?);");
                pstenf.setInt(1, i + 1);
                pstenf.setString(2, enfermedad);
                pstenf.executeUpdate();
                pstenf.close();

                String sintomas = enfSint[0].split(":")[1];
                codVoc = sintomas.split(";");

                for (int j = 0; j < codVoc.length; j++) {

                    String cod = codVoc[j].split("@")[0];
                    String voc = codVoc[j].split("@")[1];
                    if (!sourceAnadidos.contains(voc)) {
                        PreparedStatement pstSource = connection.prepareCall("INSERT INTO `diagnostico`.`source` (`source_id`, `name`) VALUES (?,?);");
                        pstSource.setInt(1, sourceAnadidos.size() + 1);
                        pstSource.setString(2, voc);
                        pstSource.executeUpdate();
                        pstSource.close();
                        sourceAnadidos.add(voc);
                        souAnad.put(souAnad.size() + 1, voc);
                        souAnadKey.put(voc, souAnad.size());

                    }

                    if (!codeAnadidos.contains(cod)) {

                        int id = souAnadKey.get(voc);
                        PreparedStatement pstCode = connection.prepareCall("INSERT INTO `diagnostico`.`code` (`code`, `source_id`) VALUES (?,?);");
                        pstCode.setString(1, cod);
                        pstCode.setInt(2, id);
                        pstCode.executeUpdate();
                        pstCode.close();
                        codeAnadidos.add(cod);

                    }
                    PreparedStatement pstDHC = connection.prepareCall("INSERT INTO `diagnostico`.`disease_has_code` (`disease_id`, `code`, `source_id`) VALUES (?,?,?);");
                    pstDHC.setInt(1, i + 1);
                    pstDHC.setString(2, cod);
                    pstDHC.setInt(3, souAnadKey.get(voc));
                    pstDHC.executeUpdate();
                    pstDHC.close();
                }
                enfSint = aDividir.split("=", 2);
                String[] sinCodSem = enfSint[1].split(";");


                for (int k = 0; k < sinCodSem.length; k++) {

                    String sin = sinCodSem[k].split(":")[0];

                    String cod = sinCodSem[k].split(":")[1];

                    String sem = sinCodSem[k].split(":")[2];


                    if (!semAdded.containsValue(sem)) {
                        PreparedStatement pstSem = connection.prepareCall("INSERT INTO `diagnostico`.`semantic_type` (`semantic_type_id`, `cui`) VALUES (?,?);");
                        pstSem.setInt(1, semAdded.size() + 1);
                        pstSem.setString(2, sem);
                        pstSem.executeUpdate();
                        pstSem.close();
                        semAdded.put(semAdded.size() + 1, sem);
                        semAddedKey.put(sem, semAdded.size());

                    }

                    if (!sinAdded.containsValue(cod)) {
                        PreparedStatement pstSin = connection.prepareCall("INSERT INTO `diagnostico`.`symptom` (`cui`, `name`) VALUES (?,?);");
                        pstSin.setString(1, cod);
                        pstSin.setString(2, sin);
                        pstSin.executeUpdate();
                        pstSin.close();
                        sinAdded.put(sinAdded.size() + 1, cod);
                    }
                    if (!sstAdded.containsValue(cod)) {
                        int id = semAddedKey.get(sem);
                        PreparedStatement pstSST = connection.prepareCall("INSERT INTO `diagnostico`.`symptom_semantic_type` (`cui`, `semantic_type_id`) VALUES (?,?);");
                        pstSST.setString(1, cod);
                        pstSST.setInt(2, id);
                        pstSST.executeUpdate();
                        pstSST.close();
                        sstAdded.put(sstAdded.size() + 1, cod);
                    }

                    PreparedStatement pstDS = connection.prepareCall("INSERT INTO `diagnostico`.`disease_symptom` (`disease_id`, `cui`) VALUES (?,?);");
                    pstDS.setInt(1, i + 1);
                    pstDS.setString(2, cod);
                    pstDS.executeUpdate();
                    pstDS.close();
                    dSAdded.put(dSAdded.size() + 1, cod);

                }


            }
            connection.close();
            conectar();

        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    private void realizarDiagnostico() {
        if (connection == null) {
            conectar();
        }

        String option = "-1";
        ArrayList<String> symptoms = new ArrayList<String>();
        diagnosticoAux();
        do {
            try {
                Statement st = connection.createStatement();

                //Obtencion de los Síntomas

                do {
                    System.out.println("\tPor favor, introduzca el codigo asociado de los sintomas que padezca.\n");
                    String symptom = readString();
                    symptoms.add(symptom);
                    System.out.println("Desea añadir más síntomas? Y/N");
                    option = readString();
                } while (option.equalsIgnoreCase("Y"));

                //Creación del String para el query

                String query = "SELECT disease_id FROM disease_symptom WHERE ";
                String queryLong = "";

                //Concatenación de los códigos de los síntomas para el query

                if (symptoms.size() > 0) {
                    for (int i = 0; i < symptoms.size() - 1; i++) {
                        queryLong = queryLong + "cui = '" + symptoms.get(i) + "' || ";
                    }
                    queryLong = queryLong + "cui = '" + symptoms.get(symptoms.size() - 1) + "'";
                }
                query = query + queryLong;

                //Añadimos las enfermedades que coincidan con los sintomas a un ArrayList

                ResultSet rs = st.executeQuery(query);
                ArrayList<Integer> aux = new ArrayList<Integer>();
                boolean encontrado = false;
                while (rs.next()) {
                    int id = rs.getInt("disease_id");
                    aux.add(id);
                }


                ArrayList<Integer> resultados = new ArrayList<Integer>();
                
                //Seleccionamos que el número de enfermedades de los resultados coincida con el número de síntomas introducidos para seleccionar la opción correcta
                
                for (int m = 0; m < aux.size() - 1; m++) {
                    int cuenta = 0;
                    for (int n = m; n < aux.size() - 1; n++) {

                        if (aux.get(m).equals(aux.get(n))) {

                            cuenta++;

                        }

                        if (cuenta == symptoms.size()) {
                            cuenta = 0;
                            resultados.add(aux.get(m));
                            encontrado = true;

                        }

                    }

                }
                
                if (!encontrado) {
                    System.out.println("No se ha encontrado ninguna enfermedad con esos síntomas");
                } else {
                    System.out.println("\n\tSu diagnóstico es:");
                    for (int i = 0; i < resultados.size(); i++) {
                        Statement st1 = connection.createStatement();
                        ResultSet rs1 = st1.executeQuery("SELECT name FROM disease WHERE disease_id = " + resultados.get(i));
                        while (rs1.next()) {
                            System.out.println("\t" + rs1.getString("name") + "\n");
                        }
                        st1.close();
                    }
                }
                st.close();
                break;


            } catch (Exception e) {
                System.err.println("Opción introducida no válida!");
            }
        }
        while (!option.equals("0"));
    }

    private void diagnosticoAux() {
        if (connection == null) {
            conectar();
        }

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT  cui, name FROM symptom ");
            System.out.println("\n\tSintomas: \n");

            while (rs.next()) {
                String nombre = rs.getString("name");
                String sintomas = rs.getString("cui");
                System.out.println("\n\tSintoma: " + nombre + "\n\tTiposSemanticos: " + sintomas + "\n");
            }

            st.close();
        } catch (Exception e) {
            System.err.println("Error al seleccionar a la BD: " + e.getMessage());
        }

    }

    private void listarSintomasEnfermedad() {
        if (connection == null) {
            conectar();
        }
        int option = -1;
        try {
            do {
                Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery("SELECT  disease_id, name FROM disease");
                System.out.println("\n\tEnfermedades: \n");

                while (rs.next()) {
                    int id = rs.getInt("disease_id");
                    String nombre = rs.getString("name");
                    System.out.println("\tID: " + id + "\n\tEnfermedad: " + nombre + "\n");
                }
                st.close();
                System.out.println("\tPor favor, introduzca el ID de la enfermedad.\n\tPara salir del menú de opciones pulse 0");

                int entrada = readInt();

                Statement st1 = connection.createStatement();
                ResultSet rs1 = st1.executeQuery("SELECT cui FROM disease_symptom WHERE disease_id=" + entrada);

                System.out.println("\n\tLa enfermedad introducida consta de los siguientes sintomas:\n");

                while (rs1.next()) {
                    String codigo = rs1.getString("cui");
                    Statement st2 = connection.createStatement();
                    ResultSet rs2 = st2.executeQuery("SELECT name FROM symptom WHERE cui in('" + codigo + "')");
                    while (rs2.next()) {
                        String sintomas = rs2.getString("name");
                        System.out.println("\t " + sintomas);
                    }
                    st2.close();
                }

                System.out.println("\n");
                st1.close();
                break;
            }
            while (option != 0);
        } catch (Exception e) {
            System.err.println("Opción introducida no válida!" + e.getMessage());
        }

    }

    private void listarEnfermedadesYCodigosAsociados() {
        if (connection == null) {
            conectar();
        }

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT  disease_id, name FROM disease");
            System.out.println("\n\tEnfermedades:");

            while (rs.next()) {
                int id = rs.getInt("disease_id");
                String nombre = rs.getString("name");
                Statement st1 = connection.createStatement();
                ResultSet rs1 = st1.executeQuery("SELECT code, source_id FROM disease_has_code WHERE disease_id = " + id);
                System.out.println("\n\tEnfermedad: " + nombre);
                while (rs1.next()) {
                    String codigo = rs1.getString("code");
                    int sourceid = rs1.getInt("source_id");
                    Statement st2 = connection.createStatement();
                    ResultSet rs2 = st2.executeQuery("SELECT name FROM diagnostico.source WHERE source_id = " + sourceid);
                    while (rs2.next()) {
                        System.out.println("\t\tCódigo: " + codigo + " - " + rs2.getString("name"));
                    }
                    st2.close();
                }
                st1.close();
            }

            st.close();
        } catch (Exception e) {
            System.err.println("Error al seleccionar a la BD: " + e.getMessage());
        }
    }

    private void listarSintomasYTiposSemanticos() {
        if (connection == null) {
            conectar();
        }

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT  cui, name FROM symptom");

            while (rs.next()) {
                String cuiSintomas = rs.getString("cui");
                String nombre = rs.getString("name");

                Statement st1 = connection.createStatement();
                ResultSet rs1 = st1.executeQuery("SELECT  semantic_type_id FROM symptom_semantic_type WHERE cui in('" + cuiSintomas + "')");
                while (rs1.next()) {
                    int idTipoSemantico = rs1.getInt("semantic_type_id");

                    Statement st2 = connection.createStatement();
                    ResultSet rs2 = st2.executeQuery("SELECT  cui FROM semantic_type WHERE semantic_type_id=" + idTipoSemantico);
                    while (rs2.next()) {
                        String tipoSemantico = rs2.getString("cui");

                        System.out.println("\n\tSintoma: " + nombre + "\n\t Tipo semantico: " + tipoSemantico);
                    }
                    st2.close();
                }
                st1.close();
            }
            st.close();
        } catch (Exception e) {
            System.err.println("Error al seleccionar a la BD: " + e.getMessage());
        }
    }

    private void mostrarEstadisticasBD() {
        try {
            if (connection == null)
                conectar();

            PreparedStatement pst = connection.prepareStatement("SELECT count(*) FROM diagnostico.disease ");
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                System.out.println("El numero de enfermedades es: " + rs.getInt("count(*)") + "\n");

            PreparedStatement pst1 = connection.prepareStatement("SELECT count(*) FROM diagnostico.symptom ");
            ResultSet rs1 = pst1.executeQuery();
            if (rs1.next())
                System.out.println("El numero de sintomas es: " + rs1.getInt("count(*)") + "\n");


            int masSin = 0;
            int id = 0;
            for (int i = 1; i < 12; i++) {
                PreparedStatement pst2 = connection.prepareStatement("SELECT count(*) FROM diagnostico.disease_symptom WHERE disease_id = ?;");
                pst2.setInt(1, i);
                ResultSet rs2 = pst2.executeQuery();
                if (rs2.next()) {
                    if (rs2.getInt("count(*)") > masSin) {
                        masSin = rs2.getInt("count(*)");
                        id = i;
                    }
                }

            }
            PreparedStatement pst3 = connection.prepareStatement("SELECT * FROM diagnostico.disease WHERE disease_id = ?; ");
            pst3.setInt(1, id);
            ResultSet rs3 = pst3.executeQuery();
            if (rs3.next())
                System.out.println("La enfermedad con mas sintomas es: " + rs3.getString("name") + "\n");


            int menSin = 1000;
            int idMen = 0;
            for (int i = 1; i < 12; i++) {
                PreparedStatement pst4 = connection.prepareStatement("SELECT count(*) FROM diagnostico.disease_symptom WHERE disease_id = ?;");
                pst4.setInt(1, i);
                ResultSet rs4 = pst4.executeQuery();
                if (rs4.next()) {
                    if (rs4.getInt("count(*)") < menSin) {
                        menSin = rs4.getInt("count(*)");
                        idMen = i;
                    }
                }

            }
            PreparedStatement pst5 = connection.prepareStatement("SELECT * FROM diagnostico.disease WHERE disease_id = ?; ");
            pst5.setInt(1, idMen);
            ResultSet rs5 = pst5.executeQuery();
            if (rs5.next())
                System.out.println("La enfermedad con menos sintomas es: " + rs5.getString("name") + "\n");


            int nSem = 0;
            PreparedStatement pst6 = connection.prepareStatement("SELECT count(*) FROM diagnostico.semantic_type ");
            ResultSet rs6 = pst6.executeQuery();
            if (rs6.next())
                nSem = rs6.getInt("count(*)");
            for (int i = 1; i < nSem + 1; i++) {

                Hashtable<Integer, String> disId = new Hashtable<Integer, String>();
                PreparedStatement pst8 = connection.prepareStatement("SELECT * FROM diagnostico.symptom_semantic_type WHERE semantic_type_id = ?; ");
                pst8.setInt(1, i);
                ResultSet rs8 = pst8.executeQuery();
                int k = 0;
                while (rs8.next()) {
                    String cui = rs8.getString("cui");
                    PreparedStatement pst11 = connection.prepareStatement("SELECT * FROM diagnostico.symptom WHERE cui = ?; ");
                    pst11.setString(1, cui);
                    ResultSet rs11 = pst11.executeQuery();
                    String dId = null;
                    if (rs11.next())
                        dId = rs11.getString("name");
                    disId.put(k, dId);
                    k++;
                }

                PreparedStatement pst7 = connection.prepareStatement("SELECT * FROM diagnostico.semantic_type WHERE semantic_type_id = ?; ");
                pst7.setInt(1, i);
                ResultSet rs7 = pst7.executeQuery();
                String j = null;
                if (rs7.next())
                    j = rs7.getString("cui");


                System.out.println("Tipo Semantico: " + j + ", Sintomas asociados: " + disId.values() + "\n");
            }
            System.out.println("El numero total de Tipos Semanticos es: " + nSem + "\n");


            int total = 0;
            int media = 0;
            for (int i = 1; i < 12; i++) {
                PreparedStatement pst10 = connection.prepareStatement("SELECT count(*) FROM diagnostico.disease_has_code WHERE disease_id = ?; ");
                pst10.setInt(1, i);
                ResultSet rs10 = pst10.executeQuery();
                if (rs10.next())
                    total += rs10.getInt("count(*)");

            }
            media = total / 11;

            System.out.println("El numero medio de sintomas: " + media + "\n");


        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

    /**
     * M�todo para leer n�meros enteros de teclado.
     *
     * @return Devuelve el n�mero le�do.
     * @throws Exception Puede lanzar excepci�n.
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
     * @throws Exception Puede lanzar excepci�n.
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
     * @throws Exception Puede lanzar excepci�n.
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
}
