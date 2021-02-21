import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Ex1 {
    static HashMap<String, VarData> network = new HashMap<>();
    static ArrayList<Query> queryList = new ArrayList<>();

    static String data;

    public static void buildNetwork(Scanner sc) {
        String name = "";
        String[] values = {};
        String[] parents = {};
        HashMap<String, Object> cpt = new HashMap<>();

        while (sc.hasNextLine()) {
            data = sc.nextLine();
            if (data.contains("Queries")) {
                break;
            }
            if (data.contains("Var ")) {
//                name = data.charAt(data.length()-1) +"";
                name = data.substring(3).trim();
            }
            if (data.contains("Values:")) {
                values = data.substring(8).split(",");
            }
            if (data.contains("Parents:")) {
                parents = data.substring(9).split(",");
            }
            if(data.contains("CPT:")) {
                while (sc.hasNextLine()) {
                    data = sc.nextLine();
                    if (data.isEmpty()) {
                        VarData varData = new VarData(values, parents, cpt);
                        network.put(name, varData);
                        cpt = new HashMap<>();
                        break;
                    }
                    String[] rowData = data.split("=", 2);

                    String parentsValue = rowData[0];
                    String myValue = rowData[1];

                    String[] splitMyValue = myValue.split("=");
                    String[] splitParentsValue = parentsValue.split(",");
                    if (parentsValue.equals("")) {
                        double sumValues = 0;
                        for(int i=0 ; i<splitMyValue.length ; i++) {
                            String[] temp = splitMyValue[i].split(",");
                            double parsedDouble = Double.parseDouble(temp[1]);
                            cpt.put(temp[0], parsedDouble);
                            sumValues += parsedDouble;
                        }
                        if (values.length != cpt.size()) {
                            for (int i=0 ; i<values.length ; i++) {
                                if (!cpt.containsKey(values[i])) {
                                    double p = 1-sumValues;
                                    cpt.put(values[i], p);
                                }
                            }
                        }
                    }
                    else {
                        HashMap<String, HashMap<String, Double>> myCpt = new HashMap<>();
                        HashMap<String, Double> valuesCpt = new HashMap<>();
                        double sumValues = 0;
                        for(int i=0 ; i < splitMyValue.length ; i++) {
                            String[] temp = splitMyValue[i].split(",");
                            double parsedDouble = Double.parseDouble(temp[1]);
                            valuesCpt.put(temp[0], parsedDouble);
                            sumValues += parsedDouble;

                        }
                        if (values.length != valuesCpt.size()) {
                            for (int i=0 ; i<values.length ; i++) {
                                if (!valuesCpt.containsKey(values[i])) {
                                    double p = 1-sumValues;
                                    valuesCpt.put(values[i], p);
                                }
                            }
                        }

                        String parentsPattern = "";
                        for (int i = 0; i < parents.length; i++) {
                            if (i == parents.length-1) {
                                parentsPattern += parents[i] + "=" + splitParentsValue[i];
                            }else {
                                parentsPattern += parents[i] + "=" + splitParentsValue[i] + "," ;
                            }
                        }
                        cpt.put(parentsPattern, valuesCpt);
                    }
                }
            }
        }
    }

    public static void buildQueries(Scanner sc) {
        String[] lhs = {};
        String[] rhs = {};
        char algoType = ' ';
        while (sc.hasNextLine()) {
            data = sc.nextLine();
            algoType = data.charAt(data.length()-1);
            String p = data.substring(2, data.length()-3);
            String[] splitP = p.split("\\|");
            String lhsStr = splitP[0];
            if (splitP.length > 1) {
                String rhsStr = splitP[1];
                rhs = rhsStr.split(",");
            }

            lhs = lhsStr.split(",");

            HashMap<String, String> lhsQn = new HashMap<>();
            HashMap<String, String> rhsQn = new HashMap<>();

            for(int i=0 ; i<lhs.length ; i++) {
                String[] splitStr = lhs[i].split("=");
                lhsQn.put(splitStr[0], splitStr[1]);
            }
            for(int i=0 ; i<rhs.length ; i++) {
                String[] splitStr = rhs[i].split("=");
                rhsQn.put(splitStr[0], splitStr[1]);
            }

            Query query = new Query(lhsQn, rhsQn, algoType);
            queryList.add(query);
        }
    }

    public static void main(String[] args) {
        try {
            File input = new File("input.txt");
            Scanner sc = new Scanner(input);
            while (sc.hasNextLine()) {
                data = sc.nextLine();
                // Network section - build the Bayesian network.
                if (data.contains("Network")) {
                    buildNetwork(sc);
                }
                // Queries Section - build the Queries
                if (data.contains("Queries")) {

                    buildQueries(sc);
                }
            }
            sc.close();
            //print();

            Algorithms algorithms = new Algorithms(network, queryList);
            algorithms.run();


        } catch (FileNotFoundException e) {
            System.out.println("Could not find the requested file.");
            e.printStackTrace();
        }
    }

    public static void print() {
        for (String key : network.keySet()) {
            System.out.println(key);
            for (int j=0 ; j<network.get(key).parents.length ; j++) {
                System.out.print(network.get(key).parents[j] + ", ");
            }
            System.out.println();
            for (int j=0 ; j<network.get(key).values.length ; j++) {
                System.out.print(network.get(key).values[j] + ", ");
            }
            System.out.println();

            for (String cptKey : network.get(key).cpt.keySet()) {
                System.out.println(cptKey + " -- " + network.get(key).cpt.get(cptKey));
            }
            System.out.println();
            System.out.println();
        }
        for (int i=0 ; i < queryList.size() ; i++) {
            Query query = queryList.get(i);
            for (String qkey : query.lhs.keySet()) {
                System.out.print(qkey + "=" + query.lhs.get(qkey));
            }
            System.out.print("|");
            for (String qkey : query.rhs.keySet()) {
                System.out.print(qkey + "=" + query.rhs.get(qkey) + ",");
            }
            System.out.println();
        }
        System.out.println();
    }
}
