package us.dit.crmiclient.model.dtos;

public record PlanSearchFilter(
    String title,
    String status,              // active, draft, retired
    String publisher,
    String topic) {

}
