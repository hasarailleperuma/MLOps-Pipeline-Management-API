package hasara.mlops;

import hasara.mlops.model.MLWorkspace;
import hasara.mlops.model.MachineLearningModel;
import hasara.mlops.model.EvaluationMetric;

import java.util.*;

/**
 * Singleton in-memory data store.
 * Uses HashMaps as the database (no SQL allowed per coursework spec).
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // workspaceId -> MLWorkspace
    private final Map<String, MLWorkspace> workspaces = new LinkedHashMap<>();

    // modelId -> MachineLearningModel
    private final Map<String, MachineLearningModel> models = new LinkedHashMap<>();

    // modelId -> list of EvaluationMetrics
    private final Map<String, List<EvaluationMetric>> metrics = new LinkedHashMap<>();

    private DataStore() {
        // Seed with sample data
        MLWorkspace ws1 = new MLWorkspace("WS-VISION-01", "Computer Vision Lab", 500);
        MLWorkspace ws2 = new MLWorkspace("WS-NLP-02", "NLP Research Team", 300);
        workspaces.put(ws1.getId(), ws1);
        workspaces.put(ws2.getId(), ws2);

        MachineLearningModel m1 = new MachineLearningModel(
                "MOD-8832", "TensorFlow", "DEPLOYED", 0.92, "WS-VISION-01");
        MachineLearningModel m2 = new MachineLearningModel(
                "MOD-1021", "PyTorch", "TRAINING", 0.75, "WS-NLP-02");
        models.put(m1.getId(), m1);
        models.put(m2.getId(), m2);

        ws1.getModelIds().add(m1.getId());
        ws2.getModelIds().add(m2.getId());

        metrics.put(m1.getId(), new ArrayList<>());
        metrics.put(m2.getId(), new ArrayList<>());
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    public Map<String, MLWorkspace> getWorkspaces() { return workspaces; }
    public Map<String, MachineLearningModel> getModels() { return models; }
    public Map<String, List<EvaluationMetric>> getMetrics() { return metrics; }
}
