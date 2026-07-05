package hasara.mlops;

import hasara.mlops.resource.DiscoveryResource;
import hasara.mlops.resource.WorkspaceResource;
import hasara.mlops.resource.ModelResource;
import hasara.mlops.exception.*;
import hasara.mlops.filter.LoggingFilter;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application entry point.
 * Sets the base URI to /api/v1
 */
@ApplicationPath("/api/v1")
public class MLOpsApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(WorkspaceResource.class);
        classes.add(ModelResource.class);

        // Exception Mappers
        classes.add(WorkspaceNotEmptyExceptionMapper.class);
        classes.add(LinkedWorkspaceNotFoundExceptionMapper.class);
        classes.add(ModelDeprecatedExceptionMapper.class);
        classes.add(ResourceNotFoundExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);

        // Filters
        classes.add(LoggingFilter.class);

        return classes;
    }
}
