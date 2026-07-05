package hasara.mlops.resource;

import hasara.mlops.DataStore;
import hasara.mlops.exception.LinkedWorkspaceNotFoundException;
import hasara.mlops.exception.ResourceNotFoundException;
import hasara.mlops.model.MachineLearningModel;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 3 - Model Operations & Linking
 * Part 4 - Sub-resource locator for metrics
 * Manages /api/v1/models
 */
@Path("/models")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ModelResource {

    private final DataStore store = DataStore.getInstance();

    // -------------------------------------------------------
    // GET /api/v1/models  (optional ?status=DEPLOYED filter)
    // -------------------------------------------------------
    @GET
    public Response getAllModels(@QueryParam("status") String status) {
        Collection<MachineLearningModel> all = store.getModels().values();

        if (status != null && !status.isBlank()) {
            List<MachineLearningModel> filtered = all.stream()
                    .filter(m -> m.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }

        return Response.ok(new ArrayList<>(all)).build();
    }

    // -------------------------------------------------------
    // GET /api/v1/models/{modelId}
    // -------------------------------------------------------
    @GET
    @Path("/{modelId}")
    public Response getModel(@PathParam("modelId") String modelId) {
        MachineLearningModel model = store.getModels().get(modelId);
        if (model == null) {
            throw new ResourceNotFoundException("Model not found: " + modelId);
        }
        return Response.ok(model).build();
    }

    // -------------------------------------------------------
    // POST /api/v1/models
    // Server generates the ID; validates workspaceId exists
    // -------------------------------------------------------
    @POST
    public Response createModel(MachineLearningModel model) {
        // Validate workspaceId exists (Part 3 integrity check)
        String wsId = model.getWorkspaceId();
        if (wsId == null || !store.getWorkspaces().containsKey(wsId)) {
            throw new LinkedWorkspaceNotFoundException(
                    "Workspace '" + wsId + "' does not exist. Model registration rejected.");
        }

        // Server generates the ID (security & integrity best practice)
        String generatedId = "MOD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        model.setId(generatedId);

        // Default status if not provided
        if (model.getStatus() == null || model.getStatus().isBlank()) {
            model.setStatus("TRAINING");
        }

        store.getModels().put(model.getId(), model);

        // Register model ID in its workspace
        store.getWorkspaces().get(wsId).getModelIds().add(model.getId());

        // Init metrics list for this model
        store.getMetrics().put(model.getId(), new ArrayList<>());

        return Response.status(Response.Status.CREATED).entity(model).build();
    }

    // -------------------------------------------------------
    // DELETE /api/v1/models/{modelId}
    // -------------------------------------------------------
    @DELETE
    @Path("/{modelId}")
    public Response deleteModel(@PathParam("modelId") String modelId) {
        MachineLearningModel model = store.getModels().get(modelId);
        if (model == null) {
            throw new ResourceNotFoundException("Model not found: " + modelId);
        }

        // Remove model from its workspace's list
        MLWorkspaceRef ws = () -> store.getWorkspaces().get(model.getWorkspaceId());
        if (ws.get() != null) {
            ws.get().getModelIds().remove(modelId);
        }

        store.getModels().remove(modelId);
        store.getMetrics().remove(modelId);

        return Response.ok(Map.of("message", "Model '" + modelId + "' deleted successfully")).build();
    }

    // -------------------------------------------------------
    // Part 4 - Sub-resource locator: /api/v1/models/{modelId}/metrics
    // Returns an instance of EvaluationMetricResource scoped to this model
    // -------------------------------------------------------
    @Path("/{modelId}/metrics")
    public EvaluationMetricResource getMetricsResource(@PathParam("modelId") String modelId) {
        // Validate model exists before delegating
        if (!store.getModels().containsKey(modelId)) {
            throw new ResourceNotFoundException("Model not found: " + modelId);
        }
        return new EvaluationMetricResource(modelId);
    }

    // Small functional interface to avoid re-fetching workspace inline
    @FunctionalInterface
    interface MLWorkspaceRef {
        hasara.mlops.model.MLWorkspace get();
    }
}
