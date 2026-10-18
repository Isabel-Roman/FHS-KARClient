package us.dit.crmiclient.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.fhir.parser.DataFormatException;
import us.dit.crmiclient.services.CrmiFacade;
import us.dit.crmiclient.transformation.InvalidPlanDefinitionException;

/**
 * Interfaz REST para ejecutar las operaciones de la fachada CRMI.
 */
@RestController
@RequestMapping("/planDefinition")
public class CrmiController {
    private static final Logger logger = LoggerFactory.getLogger(CrmiController.class);

    private final CrmiFacade crmiFacade;

    public CrmiController(CrmiFacade crmiFacade) {
        this.crmiFacade = crmiFacade;
    }

    /**
     * Publica un PlanDefinition (JSON FHIR en el cuerpo): genera su BPMN, lo publica
     * como Library y publica el PlanDefinition en el servidor CRMI.
     */
    @PostMapping(consumes = { "application/json", "application/fhir+json" })
    public ResponseEntity<Void> publishPlan(@RequestBody String rawPlanDefinition) {
        logger.info("Petición de publicación de PlanDefinition");
        crmiFacade.publishPlan(rawPlanDefinition);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Un cuerpo que no es un PlanDefinition FHIR válido es un error del cliente.
     */
    @ExceptionHandler(DataFormatException.class)
    public ResponseEntity<String> handleInvalidResource(DataFormatException e) {
        logger.warn("PlanDefinition no válido: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /**
     * Un PlanDefinition bien formado pero que no cumple las reglas del proyecto también es un
     * error del cliente.
     */
    @ExceptionHandler(InvalidPlanDefinitionException.class)
    public ResponseEntity<String> handleUnsupportedPlan(InvalidPlanDefinitionException e) {
        logger.warn("PlanDefinition no transformable: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
