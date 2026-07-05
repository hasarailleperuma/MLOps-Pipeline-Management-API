package hasara.mlops.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1 - Discovery Endpoint
 * GET /api/v1  -> returns API metadata
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "MLOps Pipeline Management API");
        info.put("version", "1.0");
        info.put("author", "Hasara");
        info.put("description", "RESTful API for managing ML Workspaces and Models");
        info.put("contact", "hasara@mlops.dev");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("workspaces", "/api/v1/workspaces");
        resources.put("models", "/api/v1/models");
        info.put("resources", resources);

        return Response.ok(info).build();
    }
}
