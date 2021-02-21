import java.util.HashMap;

public class VarData {
    String[] values;
    String[] parents;
    HashMap<String, Object> cpt;

    VarData(String[] values, String[] parents, HashMap<String, Object> cpt) {
        this.values = values;
        this.parents = parents;
        this.cpt = cpt;
    }

}
