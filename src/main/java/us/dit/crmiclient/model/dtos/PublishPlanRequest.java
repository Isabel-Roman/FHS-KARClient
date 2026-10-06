package us.dit.crmiclient.model.dtos;
import java.util.List;
public record PublishPlanRequest(    byte[] diagramBytes,         // Contenido del fichero .bpmn / .dmn / .cmmn
    String fileName,             // Nombre del fichero original (ej: proceso_glucemia.bpmn)
    String title,                // Título legible del proceso
    String version,              // Versión semántica (ej: "1.0.0")
    String publisher,            // Servicio u organización responsable
    String description,          // Descripción clínica del proceso
    String purpose,              // Objetivo del proceso
    List<String> topics          // Categorías/temáticas (ej: ["treatment", "diabetes"])
) {}
