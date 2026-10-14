# Servicio cliente CRMI

Cliente para interactuar con el servicio CRMI (Clinical Research Management Infrastructure).

Este cliente podrá usado como una API y permitirá a los desarrolladores de aplicaciones de investigación clínica interactuar con el servicio CRMI para publicar, buscar y descargar flujos de trabajo clínicos.

## Requisitos

- JDK 17 o superior.
- Maven 3.8+ (solo para compilar la librería MSBPMN).
- Git.

El proyecto se compila con el wrapper de Gradle incluido (`gradlew`), así que no hace falta instalar Gradle.

## Librería MSBPMN (transformación PlanDefinition → BPMN)

`BpmnBuilder` usa el módulo `transformation` de [MSBPMN](https://github.com/ASS-MUIT/MSBPMN) para
transformar un `PlanDefinition` FHIR R4 en un modelo BPMN. Esa librería no está publicada en Maven Central,
así que hay que compilarla e instalarla en el repositorio Maven local (`~/.m2`) antes de compilar este proyecto.

### 1. Descargar y compilar MSBPMN

```bash
git clone https://github.com/ASS-MUIT/MSBPMN.git
cd MSBPMN
mvn clean install -pl transformation -am -DskipTests -Dgpg.skip
```

- `-pl transformation -am` compila solo el módulo `transformation` y el POM padre, que también tiene que
  estar instalado en `~/.m2`.
- `-DskipTests` es opcional: quítalo si quieres ejecutar los tests de la librería.
- `-Dgpg.skip` evita que Maven intente firmar los artefactos.

Al terminar debe existir:

```
~/.m2/repository/science/aist/msbpmn/transformation/1.0-SNAPSHOT/transformation-1.0-SNAPSHOT.jar
```

Usa la rama `main` de ese repositorio, que incluye los cambios necesarios para funcionar con Spring Boot 4.
Están descritos en su README, en la sección "Compatibilidad con Spring 6/7".

### 2. Dependencias en este cliente

Esto ya está configurado en [build.gradle](build.gradle). Se resume aquí por si quieres usar la librería en otro proyecto:

```groovy
repositories {
    mavenLocal()      // aquí está la librería MSBPMN compilada en el paso anterior
    mavenCentral()
}

ext {
    hapiFhirVersion = '8.12.0'
}
// MSBPMN usa javax.xml.bind (JAXB 2.3): evita que el BOM de Spring Boot suba jaxb-runtime a 4.x (jakarta)
ext['glassfish-jaxb.version'] = '2.3.9'

dependencies {
    implementation(group: 'science.aist.msbpmn', name: 'transformation', version: '1.0-SNAPSHOT') {
        // se usa la versión de HAPI FHIR declarada abajo
        exclude group: 'ca.uhn.hapi.fhir', module: 'hapi-fhir-structures-r4'
    }
    implementation "ca.uhn.hapi.fhir:hapi-fhir-base:${hapiFhirVersion}"
    implementation "ca.uhn.hapi.fhir:hapi-fhir-structures-r4:${hapiFhirVersion}"
}
```

Si no fijas `glassfish-jaxb.version`, el contexto arranca pero la transformación falla con
*"Implementation of JAXB-API has not been found on module path or classpath"*.

### 3. Registrar los beans de la librería

Los beans de MSBPMN se definen en `TransformationConfiguration`, que hay que importar en la aplicación.
Este proyecto lo hace en [CrmiclientApplication.java](src/main/java/us/dit/crmiclient/CrmiclientApplication.java):

```java
@SpringBootApplication
@Import(TransformationConfiguration.class)
public class CrmiclientApplication { ... }
```

A partir de ahí, `BpmnBuilder` recibe por inyección el bean `planDefinitionToBpmnStringTransformer`
(`Transformer<PlanDefinition, String>`).

### 4. Requisitos del PlanDefinition

**Todas** las acciones del `PlanDefinition` deben tener `id`, también las anidadas. Si falta alguno, la
transformación lanza un `NullPointerException` en `PlanDefinitionActionStartEventCondition`.

## Probarlo

```bash
./gradlew test --tests "us.dit.crmiclient.services.BpmnBuilderTest"
```

En Windows usa `gradlew.bat`. El test transforma un PlanDefinition de ejemplo (order set de riesgo de suicidio)
y muestra el BPMN resultante por la salida estándar. Si se ha actualizado MSBPMN en `~/.m2`, añade
`--refresh-dependencies` para que Gradle use el nuevo SNAPSHOT.
