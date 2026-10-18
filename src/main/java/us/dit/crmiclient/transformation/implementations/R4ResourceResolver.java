package us.dit.crmiclient.transformation.implementations;

import org.hl7.fhir.instance.model.api.IBaseResource;

import us.dit.crmiclient.transformation.ResourceResolver;

public class R4ResourceResolver implements ResourceResolver
 {

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
