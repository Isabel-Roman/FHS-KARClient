package us.dit.crmiclient.model.dtos;

public record PlanSummary( 
    String id,
    String canonicalUrl,
    String version,
    String title,
    String status,
    String publisher,
    String description,
    String lastUpdated
){}