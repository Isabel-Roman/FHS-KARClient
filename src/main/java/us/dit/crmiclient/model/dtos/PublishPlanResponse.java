package us.dit.crmiclient.model.dtos;

public record PublishPlanResponse(String status,
    String planDefinitionCanonicalUrl,
    String libraryCanonicalUrl,
    String planDefinitionId,
    String libraryId
) {}
