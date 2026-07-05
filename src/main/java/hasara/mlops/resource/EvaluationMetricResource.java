package hasara.mlops.resource;

import hasara.mlops.DataStore;
import hasara.mlops.exception.ModelDeprecatedException;
import hasara.mlops.model.EvaluationMetric;
import hasara.mlops.model.MachineLearningModel;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Part 4 - Sub-resource for evaluation metrics.
 * Accessed via sub-resource locator in ModelResource.
 * Handles /api/v1/models/{modelId}/metrics
 *
 * @Produces is declared at CLASS level so every method inherits JSON output
 * (individual methods can override this with their own @Produces if needed).
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvaluationMetricResource {

    private final String modelId;
    private final DataStore store = DataStore.getInstance();

    public EvaluationMetricResource(String modelId) {
        this.modelId = modelId;
    }

    // -------------------------------------------------------
    // GET /api/v1/models/{modelId}/metrics
    // Retrieves all historical metrics for this model
    // -------------------------------------------------------
    @GET
    public Response getMetrics() {
        List<EvaluationMetric> metricList = store.getMetrics().get(modelId);
        return Response.ok(metricList).build();
    }

    // -------------------------------------------------------
    // POST /api/v1/models/{modelId}/metrics
    // Appends a new evaluation metric and updates model latestAccuracy
    // BLOCKED if model is DEPRECATED (403 Forbidden)
    // -------------------------------------------------------
    @POST
    public Response addMetric(EvaluationMetric metric) {
        MachineLearningModel model = store.getModels().get(modelId);

        // Part 5.3 - Block metric ingestion for deprecated models
        if ("DEPRECATED".equalsIgnoreCase(model.getStatus())) {
            throw new ModelDeprecatedException(
                    "Model '" + modelId + "' is DEPRECATED and no longer accepts new metrics.");
        }

        // Server generates the metric ID (UUID recommended per spec)
        metric.setId(UUID.randomUUID().toString());
        metric.setTimestamp(System.currentTimeMillis());

        store.getMetrics().get(modelId).add(metric);

        // Side effect: update latestAccuracy on the parent model (Part 4.2)
        model.setLatestAccuracy(metric.getAccuracyScore());

        return Response.status(Response.Status.CREATED).entity(metric).build();
    }
}
