import java.util.ArrayList;
import java.util.HashMap;

public class Factor implements Comparable{

    ArrayList<String> factorName;
    HashMap<String, Double> factorTable;

    Factor(ArrayList<String> varName, HashMap<String, Double> factorTable) {
        this.factorName = varName;
        this.factorTable = factorTable;
    }


    @Override
    public int compareTo(Object factor) {
        Factor other = (Factor) factor;

        if (this.factorTable.size() > other.factorTable.size()) {
            return 1;
        }
        else if (this.factorTable.size() < other.factorTable.size()) {
            return -1;
        } else {
            int thisAsciiValue = 0;
            int otherAsciiValue = 0;

            for (int i=0 ; i<factorName.size() ; i++) {
                for (int j = 0; j < factorName.get(i).length(); j++) {
                    thisAsciiValue += factorName.get(i).charAt(j);
                }
            }
            for (int i=0 ; i<other.factorName.size() ; i++) {
                for (int j = 0; j < other.factorName.get(i).length(); j++) {
                    otherAsciiValue += other.factorName.get(i).charAt(j);
                }
            }

            if (thisAsciiValue > otherAsciiValue) {
                return 1;
            }
            else if (thisAsciiValue < otherAsciiValue) {
                return -1;
            }
            return 0;
        }
    }
}
