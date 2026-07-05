package hasara.mlops.resource;

import hasara.mlops.DataStore;
import hasara.mlops.exception.ResourceNotFoundException;
import hasara.mlops.exception.WorkspaceNotEmptyException;
import hasara.mlops.model.MLWorkspace;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;

/**
 * Part 2 - Workspace Management
 * Manages /api/v1/workspaces
 */
@Path("/workspaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkspaceResource {

    private final DataStore store = DataStore.getInstance();

    // -------------------------------------------------------
    // GET /api/v1/workspaces
    // Returns all workspaces with Cache-Control headers
    // -------------------------------------------------------
    @GET
    public Response getAllWorkspaces() {
        Collection<MLWorkspace> workspaces = store.getWorkspaces().values();

        CacheControl cc = new CacheControl();
        cc.setMaxAge(60);           // cache for 60 seconds
        cc.setMustRevalidate(true);

        return Response.ok(workspaces).cacheControl(cc).build();
    }

    // -------------------------------------------------------
    // POST /api/v1/workspaces
    // Creates a new workspace
    // -------------------------------------------------------
    @POST
    public Response createWorkspace(MLWorkspace workspace) {
        if (workspace.getId() == null || workspace.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Workspace id is required"))
                    .build();
        }
        if (store.getWorkspaces().containsKey(workspace.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("Workspace with id '" + workspace.getId() + "' already exists"))
                    .build();
        }

        store.getWorkspaces().put(workspace.getId(), workspace);
        return Response.status(Response.Status.CREATED).entity(workspace).build();
    }

    // -------------------------------------------------------
    // GET /api/v1/workspaces/{workspaceId}
    // Returns a single workspace by ID
    // -------------------------------------------------------
    @GET
    @Path("/{workspaceId}")
    public Response getWorkspace(@PathParam("workspaceId") String workspaceId) {
        MLWorkspace ws = store.getWorkspaces().get(workspaceId);
        if (ws == null) {
            throw new ResourceNotFoundException("Workspace not found: " + workspaceId);
        }
        return Response.ok(ws).build();
    }

    // -------------------------------------------------------
    // DELETE /api/v1/workspaces/{workspaceId}
    // Deletes workspace ONLY if it has no models (Part 2 safety logic)
    // -------------------------------------------------------
    @DELETE
    @Path("/{workspaceId}")
    public Response deleteWorkspace(@PathParam("workspaceId") String workspaceId) {
        MLWorkspace ws = store.getWorkspaces().get(workspaceId);
        if (ws == null) {
            throw new ResourceNotFoundException("Workspace not found: " + workspaceId);
        }

        // Business rule: cannot delete if models still assigned
        if (ws.getModelIds() != null && !ws.getModelIds().isEmpty()) {
            throw new WorkspaceNotEmptyException(
                    "Cannot delete workspace '" + workspaceId +
                    "'. It still contains " + ws.getModelIds().size() + " model(s): " +
                    ws.getModelIds());
        }

        store.getWorkspaces().remove(workspaceId);

        Map<String, String> msg = Map.of(
                "message", "Workspace '" + workspaceId + "' deleted successfully");
        return Response.ok(msg).build();
    }

    // -------------------------------------------------------
    // HEAD /api/v1/workspaces/{workspaceId}
    // Check existence without downloading body (Part 2 question answer in practice)
    // -------------------------------------------------------
    @HEAD
    @Path("/{workspaceId}")
    public Response headWorkspace(@PathParam("workspaceId") String workspaceId) {
        if (!store.getWorkspaces().containsKey(workspaceId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    // -------------------------------------------------------
    // Helper: build a simple JSON error body
    // -------------------------------------------------------
    private Map<String, String> errorBody(String message) {
        return Map.of("error", message);
    }
}
