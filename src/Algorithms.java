import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Algorithms {
    HashMap<String, VarData> network;
    ArrayList<Query> queryList;

    Algorithms(HashMap<String, VarData> network, ArrayList<Query> queryList) {
        this.network = network;
        this.queryList = queryList;
    }

    /*
    Running the queries, and sending them according to algorithms.
        - Saves the final answer (Probability, number of sums, number of computes).
        - Sends the answer set to a function that writes into a line-by-line file.
     */
    public void run() {
        ArrayList<String> outputLines = new ArrayList<>();
        for (int i = 0; i < queryList.size(); i++) {
            if (queryList.get(i).algoType == '1') {
                outputLines.add(algo1(queryList.get(i)));
            } else if (queryList.get(i).algoType == '2') {
                outputLines.add(algo2(queryList.get(i)));
            } else if (queryList.get(i).algoType == '3') {
                outputLines.add(algo3(queryList.get(i)));
            } else {
                System.out.println("Error, no algorithm specified");
            }
        }
        createOutput(outputLines);
    }

    /*
    Writes the answers into a file
     */
    public void createOutput(ArrayList<String> outputLines) {
        try {
            FileWriter output = new FileWriter("output.txt");
            for (String line : outputLines) {
                output.write(line + "\n");
            }
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    --------- Algorithm 1 ---------
    Simple inference of the calculation, without any improvements.
     */
    public String algo1(Query query) {
        ArrayList<String> inQuery = new ArrayList<>(); // Hold's the vars names that appears in the query.
        ArrayList<String> freeVars = new ArrayList<>(); // Hold's the vars names that not appears in the query.
        ArrayList<String> freeVarsCombinations = new ArrayList<>(); // Hold's the combination of the free vars
        ArrayList<String> fullCombinations; // Hold's the combination of the free vars combined with inQuery

        double finalResult = 0;
        int sumCounter = 0;
        int computeCounter = 0;

        // fill inQuery and freeVars
        for (String key : network.keySet()) {
            if (query.lhs.containsKey(key) || query.rhs.containsKey(key)) {
                inQuery.add(key);
            } else {
                freeVars.add(key);
            }
        }

        // Gets the Query var name (In this assignment there is only one).
        String queryVarName = "";
        for (String key : query.lhs.keySet()) {
            queryVarName = key;
        }

        // The answer can be obtained directly
        if (freeVars.size() == 0) {

            finalResult = getAnswerDirectly(query, inQuery, queryVarName);

        }
        // If the answer cannot be obtained directly
        else {
            // find all the combinations for the free vars
            findCombinations(freeVarsCombinations, freeVars);

            // Store the results of Query var for each value.
            double[] results = new double[network.get(queryVarName).values.length];

            // Coping the original query By Value so we wont change the original one.
            Query newQuery = new Query((HashMap) query.lhs.clone(), (HashMap) query.rhs.clone(), query.algoType);

            int indexForQueryValue = 0; // The index where the ans is stored in 'results'.
            // Fill 'fullCombinations' - Combine 'freeVarsCombination' with 'inQuery'
            for (int i = 0; i < results.length; i++) {
                if (query.lhs.get(queryVarName).equals(network.get(queryVarName).values[i])) {
                    indexForQueryValue = i;
                }
                // Each iteration we add another value of the query var so we can normalize the answer.
                newQuery.lhs.put(queryVarName, network.get(queryVarName).values[i]);

                // Combine each combination of the free vars with in query vars. (creates full combination)
                fullCombinations = fullCombinationCalc(freeVarsCombinations, inQuery, newQuery);

                // Calc each combination
                // Summing the answers for each combination
                // Increase sum amount for each iteration
                // Summing the compute amount (we count each compute in 'calcResult' function)
                for (int j = 0; j < fullCombinations.size(); j++) {
                    double[] calcResult = calc(fullCombinations.get(j));
                    results[i] += calcResult[0];
                    computeCounter += calcResult[1] - 1;
                    if (j != 0) {
                        sumCounter++;
                    }
                }
            }
            // Summing all the values on results so we can normalize the final answer
            double denominator = 0;
            for (int i = 0; i < results.length; i++) {
                denominator += results[i];
                if (i != 0) {
                    sumCounter++;
                }
            }
            // Taking the result we looking for, and normalize the ans
            finalResult = results[indexForQueryValue] / denominator;
        }
        String finalResultStr = String.format("%.5f", finalResult);

        String output = finalResultStr + "," + sumCounter + "," + computeCounter;

        printAns(query, output, sumCounter, computeCounter);

        return output;
    }


    /*
    --------- Algorithm 2 ---------
    Variable elimination, with the removal of unnecessary variables at the beginning,
    when the elimination order of the variables is in the ABC order
     */
    public String algo2(Query query) {

        ArrayList<String> inQuery = new ArrayList<>(); // Hold's the vars names that appears in the query.
        ArrayList<String> freeVars = new ArrayList<>(); // Hold's the vars names that not appears in the query.

        String queryVarName = ""; // The query var name (for convenience)

        // fill inQuery, store query var name
        for (String key : network.keySet()) {
            if (query.rhs.containsKey(key)) {
                inQuery.add(key);
            } else if (query.lhs.containsKey(key)) {
                queryVarName = key;
            }
        }


        // Filling in freeVars according to variables that do not appear in the query,
        // but in addition contribute to the calculation.
        graphSearch(inQuery, freeVars, queryVarName);


        double finalResult = 0;
        int sumCounter = 0;
        int computeCounter = 0;

        // The answer can be obtained directly
        if (freeVars.size() == 0) {

            finalResult = getAnswerDirectly(query, inQuery, queryVarName);

        } else {
            // Sort freeVars by 'ABC'
            Collections.sort(freeVars);

            // This function runs all the operations on the factors, JOIN & ELIMINATION, and finally calculates the answer.
            // The elimination order will be as the order of freeVars.
            // The function will return the answer within an array when:
            //  - result[0] = Probability
            //  - result[1] = Number of sums
            //  - result[2] = Number of computes
            double[] result = runFactorsCalc(query, inQuery, freeVars, queryVarName);

            finalResult = result[0];
            sumCounter = (int) result[1];
            computeCounter = (int) result[2];


        }

        String finalResultStr = String.format("%.5f", finalResult);

        String output = finalResultStr + "," + sumCounter + "," + computeCounter;

        printAns(query, output, sumCounter, computeCounter);

        return output;
    }


    /*
    --------- Algorithm 3 ---------
    Variable elimination, with the removal of unnecessary variables at the beginning,
    when you heuristically determine the order of elimination of the variables.
     */
    public String algo3(Query query) {

        ArrayList<String> inQuery = new ArrayList<>(); // Hold's the vars names that appears in the query.
        ArrayList<String> freeVars = new ArrayList<>(); // Hold's the vars names that not appears in the query.
        String queryVarName = ""; // The query var name (for convenience)

        // fill inQuery, store query var name
        for (String key : network.keySet()) {
            if (query.rhs.containsKey(key)) {
                inQuery.add(key);
            } else if (query.lhs.containsKey(key)) {
                queryVarName = key;
            }
        }


        // Filling in freeVars according to variables that do not appear in the query,
        // but in addition contribute to the calculation.
        graphSearch(inQuery, freeVars, queryVarName);

        double finalResult = 0;
        int sumCounter = 0;
        int computeCounter = 0;

        // The answer can be obtained directly
        if (freeVars.size() == 0) {

            finalResult = getAnswerDirectly(query, inQuery, queryVarName);

        } else {
            // Sort freeVars by the:
            //        (number_of_parents + parents_values) +
            //            (number_of_children + children_values) +
            //                (number_of_current_var_values)
            sortByParentsAndChildrenAmount(freeVars, queryVarName);

            // This function runs all the operations on the factors, JOIN & ELIMINATION, and finally calculates the answer.
            // The elimination order will be as the order of freeVars.
            // The function will return the answer within an array when:
            //  - result[0] = Probability
            //  - result[1] = Number of sums
            //  - result[2] = Number of computes
            double[] result = runFactorsCalc(query, inQuery, freeVars, queryVarName);

            finalResult = result[0];
            sumCounter = (int) result[1];
            computeCounter = (int) result[2];

        }


        String finalResultStr = String.format("%.5f", finalResult);
        String output = finalResultStr + "," + sumCounter + "," + computeCounter;

        printAns(query, output, sumCounter, computeCounter);

        return output;
    }


    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ Help Functions @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


    public double getAnswerDirectly(Query query, ArrayList<String> inQuery, String queryVarName) {
        // Insert the Q var parents into arraylist (for comfort)
        ArrayList<String> parentsToArrayList = new ArrayList<>(Arrays.asList(network.get(queryVarName).parents));
        ArrayList<String> childrens = new ArrayList<>();
        double finalResult = 0;

        String parentsKey = "";
        for (String key : query.rhs.keySet()) {
            if (parentsToArrayList.contains(key)) {
                parentsToArrayList.set(parentsToArrayList.indexOf(key), key + "=" + query.rhs.get(key) + ",");
            }

            // If there is a child for the query variable with probability 1 or 0
            ArrayList<String> rhsVarParents = new ArrayList<>(Arrays.asList(network.get(key).parents));
            if (rhsVarParents.contains(queryVarName)) {
                childrens.add(key);
            }
        }
        boolean foundChildWithProbZeroOrOne = false;
        if (childrens.size() > 0) {
            for (String child : childrens) {
                ArrayList<String> inQueryChildParents = new ArrayList<>();
                for (int i = 0; i < network.get(child).parents.length; i++) {
                    if (inQuery.contains(network.get(child).parents[i]) || network.get(child).parents[i].equals(queryVarName)) {
                        String parentNameAndKey = network.get(child).parents[i] + "=" + getTagFromQuery(query, network.get(child).parents[i]);
                        inQueryChildParents.add(parentNameAndKey);
                    }
                }
                String childValue = query.rhs.get(child);
                HashMap<String, Double> innerCpt = new HashMap<>();
                boolean rowFitsAllParents = false;
                for (String key : network.get(child).cpt.keySet()) {
                    int counter = 0;
                    for (String childParent : inQueryChildParents) {
                        if (key.contains(childParent)) {
                            counter++;
                        }
                        if (counter == inQueryChildParents.size()) {
                            rowFitsAllParents = true;
                            break;
                        }
                    }
                    if (rowFitsAllParents == true) {
                        innerCpt = (HashMap) network.get(child).cpt.get(key);
                        break;
                    }
                }
                if (innerCpt.get(childValue) == 1) {
                    finalResult = 1;
                    foundChildWithProbZeroOrOne = true;
                } else if (innerCpt.get(childValue) == 0) {
                    finalResult = 0;
                    foundChildWithProbZeroOrOne = true;
                }
            }
        }
        if (foundChildWithProbZeroOrOne == false) {
            for (int i = 0; i < parentsToArrayList.size(); i++) {
                parentsKey += parentsToArrayList.get(i);
            }
            parentsKey = parentsKey.substring(0, parentsKey.length() - 1);

            HashMap<String, Double> innerCpt = (HashMap) network.get(queryVarName).cpt.get(parentsKey);
            finalResult = (double) innerCpt.get(getTagFromQuery(query, queryVarName));
        }
        return finalResult;
    }


    /*
    This function runs all the operations on the factors, JOIN & ELIMINATION, and finally calculates the answer.
    The different between algo2 & algo3, is the elimination order (freeVars).
    The function will return the answer within an array when:
     - result[0] = Probability
     - result[1] = Number of sums
     - result[2] = Number of computes
     */
    public double[] runFactorsCalc(Query query, ArrayList<String> inQuery, ArrayList<String> freeVars, String queryVarName) {
        int sumCounter = 0;
        int computeCounter = 0;

        freeVars.add(queryVarName);

        // Creating Factors:
        //  - local CPTs instantiated by evidence.
        //  - If an instantiated CPT becomes one-valued, we discard the factor.
        ArrayList<Factor> factors = buildFactors(query, inQuery, freeVars);

        // Ordering the factors according to the factor size && ASCII values
        Collections.sort(factors);

        for (int g = 0; g < freeVars.size(); g++) {
            String varNameToEliminate = freeVars.get(g);

            if (varNameToEliminate.equals(queryVarName)) {
                continue;
            }

            // For each hidden var (free var), we:
            //  - Join the factors that contain this var name (by factor-name).
            //  - After there is no more factors that contains the var name (Besides the current factor), Eliminate the var.
            for (int i = 0; i < factors.size(); i++) {
                Factor newFactor = factors.get(i);
                ArrayList<String> factorName = factors.get(i).factorName;

                if (!factorName.contains(varNameToEliminate)) {
                    continue;
                }
                for (int j = 0; j < factors.size(); j++) {
                    if (i == j && factors.size() > 1) {
                        continue;
                    }
                    // If we found another factor that contains the var we want to eliminate, join the two factors
                    if (factors.get(j).factorName.contains(varNameToEliminate) && factors.size() > 1) {
                        FactorAndCounter factorAndCounter = joinFactors(factors.get(i), factors.get(j), varNameToEliminate);
                        newFactor = factorAndCounter.factor;
                        computeCounter += factorAndCounter.computeCounter;

                        varNameToEliminate = factorName.get(0);
                        factors.set(i, newFactor);
                        factors.remove(j);
                    }

                    // Check if there is no more factors to join, if there isn't, eliminate that var.
                    boolean toEliminateBySum = true;
                    for (int k = 0; k < factors.size(); k++) {
                        if (k == factors.indexOf(newFactor)) {
                            continue;
                        }
                        if (factors.get(k).factorName.contains(varNameToEliminate)) {
                            toEliminateBySum = false;
                        }
                    }
                    if (toEliminateBySum == true && newFactor.factorName.size() > 1 && !varNameToEliminate.equals(queryVarName) && newFactor.factorName.contains(varNameToEliminate)) {
                        int lastIndex = factors.indexOf(newFactor);
                        FactorAndCounter factorAndCounter = eliminateVarBySum(newFactor, varNameToEliminate);
                        newFactor = factorAndCounter.factor;
                        sumCounter += factorAndCounter.sumCounter;

                        factors.set(lastIndex, newFactor);

                    }
                    Collections.sort(factors);
                }
            }
        }
        // Minus the first sum (0 + ..)
        sumCounter--;

        if (factors.size() == 2) {
            FactorAndCounter factorAndCounter = joinFactors(factors.get(0), factors.get(1), factors.get(0).factorName.get(0));
            Factor newFactor = factorAndCounter.factor;
            computeCounter += factorAndCounter.computeCounter;

            factors.set(0, newFactor);
            factors.remove(1);
        }
        if (factors.size() == 1 && factors.get(0).factorName.size() > 1) {
            for (String varNameToEliminate : freeVars) {
                if (factors.get(0).factorName.contains(varNameToEliminate) && !varNameToEliminate.equals(queryVarName)) {

                    FactorAndCounter factorAndCounter = eliminateVarBySum(factors.get(0), varNameToEliminate);
                    Factor newFactor = factorAndCounter.factor;
                    sumCounter += factorAndCounter.sumCounter;

                    factors.set(0, newFactor);
                }
            }
        }

        // Summing all the values on results so we can normalize the final answer
        double denominator = 0;
        for (String key : factors.get(0).factorTable.keySet()) {
            denominator += factors.get(0).factorTable.get(key);
            sumCounter++;
        }
        String nameAndTag = queryVarName + "=" + getTagFromQuery(query, queryVarName);

        // Taking the result we looking for, and normalize the ans
        double finalResult = factors.get(0).factorTable.get(nameAndTag) / denominator;
        double[] result = {finalResult, sumCounter, computeCounter};
        return result;
    }


    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ Help Functions for Algo 3 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    public void printFactor(ArrayList<Factor> factors) {
        System.out.println();
        for (int i = 0; i < factors.size(); i++) {
            System.out.println(factors.get(i).factorName);
            for (String key : factors.get(i).factorTable.keySet()) {
                System.out.println(key + " -- " + factors.get(i).factorTable.get(key));
            }
        }
        System.out.println();
    }

    /*
    Sort freeVars by the:
        (number_of_parents + parents_values) +
            (number_of_children + children_values) +
                (number_of_current_var_values)
     */
    public void sortByParentsAndChildrenAmount(ArrayList<String> freeVars, String queryVarName) {
        HashMap<String, Integer> parentsAndChildren = new HashMap<>();

        freeVars.add(queryVarName);

        for (String key : freeVars) {
            String[] parents = network.get(key).parents;
            int parentsAmount = network.get(key).parents.length;
            int numberOfValues = network.get(key).values.length;
            if (parents[0].equals("none")) {
                if (!parentsAndChildren.containsKey(key)) {
                    parentsAmount = 0;
                } else {
                    continue;
                }
            } else {
                int count;
                for (int i = 0; i < parents.length; i++) {
                    if (parentsAndChildren.containsKey(parents[i])) {
                        count = parentsAndChildren.get(parents[i]);
                    } else {
                        count = 0;
                    }
                    count += numberOfValues;
                    parentsAndChildren.put(parents[i], count + 1);

                    numberOfValues += network.get(parents[i]).values.length;
                }
            }
            int value = parentsAmount + numberOfValues;
            parentsAndChildren.put(key, value);
        }
        freeVars.remove(queryVarName);

        Collections.sort(freeVars, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (parentsAndChildren.get(o1) > parentsAndChildren.get(o2)) {
                    return 1;
                }
                if (parentsAndChildren.get(o1) < parentsAndChildren.get(o2)) {
                    return -1;
                }
                return 0;
            }
        });
    }

    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ Help Functions for Algo 2 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


    /*
    eliminateVarBySum + eliminateVarBySumHelper:
        Recursive function.
        Summarizes the rows for each value of the variable we want to eliminate, and deletes it from the list.
     */
    public FactorAndCounter eliminateVarBySum(Factor factor, String varNameToEliminate) {
        int sumCounter = 0;
        HashMap<String, Double> newFactorTable = new HashMap<>();
        ArrayList<String> usedKeys = new ArrayList<>();
        return eliminateVarBySumHelper(factor, varNameToEliminate, newFactorTable, sumCounter);
    }

    private FactorAndCounter eliminateVarBySumHelper(Factor factor, String varNameToEliminate, HashMap<String, Double> newFactorTable, int sumCounter) {
        // If we went through all the lines
        if (factor.factorTable.size() == 0) {
            factor.factorName.remove(varNameToEliminate);
            Factor newFactor = new Factor(factor.factorName, newFactorTable);
            FactorAndCounter factorAndCounter = new FactorAndCounter(newFactor, sumCounter, 0);
            return factorAndCounter;
        }
        String key = factor.factorTable.keySet().stream().findFirst().get();
        String[] splitedKey = key.split(",");
        String keyWithoutHidden = "";
        // all the factor keys without the hidden var
        for (int i = 0; i < splitedKey.length; i++) {
            if (!splitedKey[i].contains(varNameToEliminate)) {
                keyWithoutHidden += splitedKey[i] + ",";
            }
        }
        double sum = 0;
        String sortedKey = "";

        for (int i = 0; i < network.get(varNameToEliminate).values.length; i++) {
            String keyPerValue = keyWithoutHidden + varNameToEliminate + "=" + network.get(varNameToEliminate).values[i];
            sortedKey = sortNameValueKeys(keyPerValue);

            sum += factor.factorTable.get(sortedKey);

            sumCounter++;

            factor.factorTable.remove(sortedKey);

        }

        sumCounter--;
        newFactorTable.put(keyWithoutHidden.substring(0, keyWithoutHidden.length() - 1), sum);

        return eliminateVarBySumHelper(factor, varNameToEliminate, newFactorTable, sumCounter);
    }


    /*
    Performs a JOIN between 2 factors according to the variable we want to eliminate.
    The JOIN operation is performed by multiplying the rows, according to
    the values of the variable we want to eliminate.
     */
    public FactorAndCounter joinFactors(Factor factorA, Factor factorB, String factorNameToEliminate) {

        int computeCounter = 0;
        HashMap<String, Double> newFactorTable = new HashMap<>();

        if (factorA.factorTable.size() > factorB.factorTable.size()) {
            Factor temp = factorA;
            factorA = factorB;
            factorB = temp;
        }

        for (String keyA : factorA.factorTable.keySet()) {
            ArrayList<String> newFactorName = new ArrayList<>();
            String[] keyArray = keyA.split(",");
            String keyValue = "";
            String newKey = "";
            for (int i = 0; i < keyArray.length; i++) {
                if (keyArray[i].contains(factorNameToEliminate)) {
                    keyValue = keyArray[i];
                } else {
                    boolean contains = false;
                    for (int j = 0; j < factorB.factorName.size() && !contains; j++) {
                        if (newKey.contains(factorB.factorName.get(j))) {
                            contains = true;
                        }
                    }
                    if (contains == false) {
                        newKey += keyArray[i] + ",";
                    }
                }
            }
            for (String keyB : factorB.factorTable.keySet()) {
                if (keyB.contains(keyValue)) {
                    double multipliedValue = factorA.factorTable.get(keyA) * factorB.factorTable.get(keyB);
                    String tableKey = newKey + keyB;
                    String sortedTableKey = sortNameValueKeys(tableKey);

                    newFactorTable.put(sortedTableKey, multipliedValue);

                    computeCounter++;
                }
            }
        }
        ArrayList<String> newFactorName = new ArrayList<>();
        for (int i = 0; i < factorA.factorName.size(); i++) {
            if (!factorB.factorName.contains(factorA.factorName.get(i)) && !factorA.factorName.get(i).equals(factorNameToEliminate)) {
                newFactorName.add(factorA.factorName.get(i));
            }
        }
        newFactorName.addAll(factorB.factorName);
        Collections.sort(newFactorName);

        Factor newFactor = new Factor(newFactorName, newFactorTable);
        FactorAndCounter factorAndCounter = new FactorAndCounter(newFactor, 0, computeCounter);

        return factorAndCounter;
    }


    public ArrayList<Factor> buildFactors(Query query, ArrayList<String> inQuery, ArrayList<String> freeVars) {
        ArrayList<Factor> factors = new ArrayList<>();

        for (String key : network.keySet()) {
            if (!inQuery.contains(key) && !freeVars.contains(key)) {
                continue;
            }
            HashMap<String, Double> factorTable = new HashMap<>();
            if (network.get(key).parents[0].equals("none")) {
                // In this case the var will only have 1 value, so we can continue without hes factor
                if (inQuery.contains(key)) {
                    continue;
                }
                for (String cptKey : network.get(key).cpt.keySet()) {
                    String factorTableKey = key + "=" + cptKey;
                    Double value = (Double) network.get(key).cpt.get(cptKey);
                    factorTable.put(factorTableKey, value);
                }
            } else {
                factorTable = createFactorTable(query, inQuery, key);
                if (factorTable == null) {
                    continue;
                }
            }

            // Set Factor Name
            String firstEntry = (String) factorTable.keySet().toArray()[0];
            String[] splitNames = firstEntry.split(",");
            ArrayList<String> factorName = new ArrayList<>();
            for (int i = 0; i < splitNames.length; i++) {
                String temp = splitNames[i];
                String[] nameValue = temp.split("=");
                if (!inQuery.contains(nameValue[0])) {
                    factorName.add(nameValue[0]);
                }
            }
            Collections.sort(factorName);
            Factor factor = new Factor(factorName, factorTable);
            factors.add(factor);

        }
        return factors;
    }


    public HashMap<String, Double> createFactorTable(Query query, ArrayList<String> inQuery, String key) {
        HashMap<String, Double> factorTable = new HashMap<>();
        ArrayList<String> parentsNamesAndTags = new ArrayList<>();
        // Getting all var parents name and value that appear in the Query
        for (int j = 0; j < network.get(key).parents.length; j++) {
            String currParent = network.get(key).parents[j];
            if (inQuery.contains(currParent)) {
                parentsNamesAndTags.add(currParent + "=" + getTagFromQuery(query, currParent));
            }
        }
        // If the var have hidden parents
        if (parentsNamesAndTags.size() < network.get(key).parents.length) {
            HashMap<String, HashMap<String, Double>> tempCpt = (HashMap) network.get(key).cpt.clone();
            ArrayList<String> keysToBeRemoved = new ArrayList<>();

            // Getting all the keys that need to be deleted from the cpt (wont be in the factor)
            for (String cptKey : network.get(key).cpt.keySet()) {
                for (int j = 0; j < parentsNamesAndTags.size(); j++) {
                    if (!cptKey.contains(parentsNamesAndTags.get(j))) {
                        keysToBeRemoved.add(cptKey);
                    }
                }
            }
            // Removing all the keys that do not need to be in the factor
            for (int j = 0; j < keysToBeRemoved.size(); j++) {
                tempCpt.remove(keysToBeRemoved.get(j));
            }

            // Filling factorTable with keys and values
            for (String cptKey : tempCpt.keySet()) {
                if (inQuery.contains(key)) {
                    String tag = getTagFromQuery(query, key);
                    double value = tempCpt.get(cptKey).get(tag);
                    String newKey = cptKey;
                    for (int i = 0; i < parentsNamesAndTags.size(); i++) {
                        newKey = newKey.replace(parentsNamesAndTags.get(i) + ",", "");
                        newKey = newKey.replace("," + parentsNamesAndTags.get(i), "");
                    }
                    String sortedKey = sortNameValueKeys(newKey);
                    factorTable.put(sortedKey, value);
                } else {
                    for (String innerKey : tempCpt.get(cptKey).keySet()) {
                        String newKey = cptKey + "," + key + "=" + innerKey;
                        for (int i = 0; i < parentsNamesAndTags.size(); i++) {
                            newKey = newKey.replace(parentsNamesAndTags.get(i) + ",", "");
                            newKey = newKey.replace("," + parentsNamesAndTags.get(i), "");
                        }
                        String sortedKey = sortNameValueKeys(newKey);
                        double value = tempCpt.get(cptKey).get(innerKey);
                        factorTable.put(sortedKey, value);
                    }
                }
            }
        } else {
            if (!inQuery.contains(key)) {
                HashMap<String, HashMap<String, Double>> tempCpt = (HashMap) network.get(key).cpt.clone();
                ArrayList<String> keysToBeRemoved = new ArrayList<>();

                // Getting all the keys that need to be deleted from the cpt (wont be in the factor)
                for (String cptKey : network.get(key).cpt.keySet()) {
                    for (int j = 0; j < parentsNamesAndTags.size(); j++) {
                        if (!cptKey.contains(parentsNamesAndTags.get(j))) {
                            keysToBeRemoved.add(cptKey);
                        }
                    }
                }
                // Removing all the keys that do not need to be in the factor
                for (int j = 0; j < keysToBeRemoved.size(); j++) {
                    tempCpt.remove(keysToBeRemoved.get(j));
                }
                for (String cptKey : tempCpt.keySet()) {

                    for (String innerKey : tempCpt.get(cptKey).keySet()) {
                        String newKey = cptKey + "," + key + "=" + innerKey;
                        for (int i = 0; i < parentsNamesAndTags.size(); i++) {
                            newKey = newKey.replace(parentsNamesAndTags.get(i) + ",", "");
                        }
                        String sortedKey = sortNameValueKeys(newKey);
                        double value = tempCpt.get(cptKey).get(innerKey);
                        factorTable.put(sortedKey, value);

                    }
                }

            } else {
                return null;
            }
        }
        return factorTable;
    }


    public String sortNameValueKeys(String key) {
        String[] splitKey = key.split(",");

        Set<String> temp = new HashSet<String>(Arrays.asList(splitKey));
        splitKey = temp.toArray(new String[temp.size()]);

        Arrays.sort(splitKey);

        String sortedKey = "";
        for (int i = 0; i < splitKey.length; i++) {
            sortedKey += splitKey[i] + ",";
        }
        sortedKey = sortedKey.substring(0, sortedKey.length() - 1);
        return sortedKey;
    }


    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ Help Functions for Algo 1 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


    // Fill 'fullCombinations' - Combine 'freeVarsCombination' with 'inQuery'
    public ArrayList<String> fullCombinationCalc(ArrayList<String> freeVarsCombinations, ArrayList<String> inQuery, Query query) {
        ArrayList<String> fullCombinations = new ArrayList<>();
        for (int i = 0; i < freeVarsCombinations.size(); i++) {
            String nameValueVar = "";
            for (int j = 0; j < inQuery.size(); j++) {
                String tag = getTagFromQuery(query, inQuery.get(j));
                nameValueVar += inQuery.get(j) + "=" + tag + ",";
            }
            fullCombinations.add(nameValueVar + freeVarsCombinations.get(i));
        }
        return fullCombinations;
    }


    public double[] calc(String q) {
        double[] result = new double[2];
        result[0] = 1;
        String[] splitedQ = q.split(",");
        for (int i = 0; i < splitedQ.length; i++) {
            String[] temp = splitedQ[i].split("=");
            String varName = temp[0];
            String varTag = temp[1];

            if (network.get(varName).parents[0].equals("none")) {
                result[0] *= (double) network.get(varName).cpt.get(varTag);
                result[1]++;
            } else {
                String parentsNamesAndTags = "";
                for (int j = 0; j < network.get(varName).parents.length; j++) {
                    for (int k = 0; k < splitedQ.length; k++) {
                        String[] temp2 = splitedQ[k].split("=");
                        String parName = temp2[0];
                        String parTag = temp2[1];
                        if (network.get(varName).parents[j].equals(parName)) {
                            parentsNamesAndTags += parName + "=" + parTag + ",";
                        }
                    }
                }
                parentsNamesAndTags = parentsNamesAndTags.substring(0, parentsNamesAndTags.length() - 1);

                HashMap<String, Double> innerCpt = (HashMap<String, Double>) network.get(varName).cpt.get(parentsNamesAndTags);
                result[0] *= innerCpt.get(varTag);
                result[1]++;
            }
        }
        return result;
    }


    public void findCombinations(ArrayList<String> freeVarsCombinations, ArrayList<String> freeVars) {
        int[] ind = new int[freeVars.size()];
        findCombinationsHelper(freeVarsCombinations, freeVars, ind);
    }


    public void findCombinationsHelper(ArrayList<String> freeVarsCombinations, ArrayList<String> freeVars, int[] ind) {
        boolean done = true;
        for (int i = 0; i < ind.length; i++) {
            if (ind[i] < network.get(freeVars.get(i)).values.length) {
                done = false;
                break;
            }
        }
        if (done) {
            return;
        }
        String combination = "";
        for (int i = 0; i < ind.length; i++) {
            if (i == ind.length - 1) {
                combination += freeVars.get(i) + "=" + network.get(freeVars.get(i)).values[ind[i]];
            } else {
                combination += freeVars.get(i) + "=" + network.get(freeVars.get(i)).values[ind[i]] + ",";
            }
        }
        if (!freeVarsCombinations.contains(combination)) {
            freeVarsCombinations.add(combination);
        }
        for (int i = 0; i < ind.length; i++) {
            if (ind[i] < network.get(freeVars.get(i)).values.length - 1) {
                ind[i]++;
                findCombinationsHelper(freeVarsCombinations, freeVars, ind);
                ind[i]--;
            }
        }
    }


    public String getTagFromQuery(Query query, String varName) {
        String tag;
        if (query.lhs.containsKey(varName)) {
            tag = query.lhs.get(varName);
        } else {
            tag = query.rhs.get(varName);
        }
        return tag;
    }

    public void graphSearch(ArrayList<String> inQuery, ArrayList<String> freeVars, String queryVarName) {
        inQuery.add(queryVarName);
        for (int i = 0; i < inQuery.size(); i++) {
            graphSearchHelper(inQuery, freeVars, inQuery.get(i), queryVarName);
        }
        inQuery.remove(queryVarName);
    }

    private void graphSearchHelper(ArrayList<String> inQuery, ArrayList<String> freeVars, String var, String queryVarName) {
        if (network.get(var).parents[0].equals("none")) {
            return;
        }
        for (int i = 0; i < network.get(var).parents.length; i++) {
            if (!inQuery.contains(network.get(var).parents[i]) && !freeVars.contains(network.get(var).parents[i])) {
                freeVars.add(network.get(var).parents[i]);
            }
            if (!freeVars.contains(queryVarName)) {
                ArrayList<String> parentsToArrayList = new ArrayList<>(Arrays.asList(network.get(var).parents));
                if (parentsToArrayList.contains(queryVarName)) {
                    freeVars.add(queryVarName);
                }
            }
            graphSearchHelper(inQuery, freeVars, network.get(var).parents[i], queryVarName);
        }
    }

    public void printAns(Query query, String output, int sumCounter, int computeCounter) {
        System.out.print("Algo " + query.algoType + " -- ");
        for (String qkey : query.lhs.keySet()) {
            System.out.print(qkey + "=" + query.lhs.get(qkey));
        }
        System.out.print("|");
        for (String qkey : query.rhs.keySet()) {
            System.out.print(qkey + "=" + query.rhs.get(qkey) + ",");
        }
        System.out.println();
        System.out.println(output);
        System.out.println();
    }


}
