package us.dit.crmiclient.services.implementations;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.api.MethodOutcome;

import us.dit.crmiclient.services.ResourceResolver;
@Service 
public class R4ResourceResolver implements ResourceResolver
 {
    private final FhirContext fhirContext = FhirContext.forR4();
    private final IGenericClient fhirClient;

    public R4ResourceResolver(@Value("${crmiserver.url}") String serverUrl) {
        // Evita que el cliente consulte /metadata antes de cada primera petición
        fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        this.fhirClient = fhirContext.newRestfulGenericClient(serverUrl);

    }

    @Override
    public <T extends IBaseResource> T resolveCanonical(String canonical, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resolveCanonical'");
    }

    @Override
    public <T extends IBaseResource> T resolveReference(String url, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'resolveReference'");
    }

}
