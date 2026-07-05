package hasara.mlops.exception;

public class ModelDeprecatedException extends RuntimeException {
    public ModelDeprecatedException(String message) { super(message); }
}
