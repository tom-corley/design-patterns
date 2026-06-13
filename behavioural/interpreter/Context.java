package behavioural.interpreter;

import java.util.HashMap;

public class Context {
    private final HashMap<String, Boolean> variableBindings = new HashMap<>();

    public boolean getVariableValue(String key) {
        if (!variableBindings.containsKey(key)) {
            throw new IllegalArgumentException("Variable not found: " + key);
        }

        return variableBindings.get(key);
    }

    public void setVariableValue(String key, boolean value) {
        variableBindings.put(key, value);
    }
}