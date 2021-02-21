import java.util.HashMap;

public class Query {
    HashMap<String, String> lhs;
    HashMap<String, String> rhs;
    char algoType;

    Query(HashMap<String, String> lhs, HashMap<String, String> rhs, char algoType) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.algoType = algoType;
    }

}
