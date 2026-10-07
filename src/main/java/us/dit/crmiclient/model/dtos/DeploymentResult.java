package us.dit.crmiclient.model.dtos;

public record DeploymentResult( 
    String deploymentId,
    String processDefinitionKey,
    String engineTarget,
    String deployedAt,
    String status) {

}
