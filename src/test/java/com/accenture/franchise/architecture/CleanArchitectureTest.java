package com.accenture.franchise.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica por test la regla de dependencia: las capas internas no conocen a
 * las externas. Un import equivocado rompe la build en lugar de degradar la
 * arquitectura en silencio.
 */
@AnalyzeClasses(packages = "com.accenture.franchise", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureTest {

    @ArchTest
    // el dominio no depende de la aplicacion ni de la infraestructura
    static final ArchRule domainIsIndependent = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    // la aplicacion no depende de la infraestructura
    static final ArchRule applicationDoesNotKnowInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    @ArchTest
    // dominio y aplicacion no conocen Spring ni Mongo
    static final ArchRule innerLayersAreFrameworkAgnostic = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "com.mongodb..", "jakarta.validation..");

    @ArchTest
    // los controladores solo hablan con casos de uso, nunca con el puerto de salida
    static final ArchRule controllersUseTheUseCase = noClasses()
            .that().resideInAPackage("..adapter.in.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..domain.port.out..");

    @ArchTest
    // los documentos de Mongo no salen de su adaptador
    static final ArchRule documentsStayInTheAdapter = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.mongo..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.out.mongo.document..");

    @Test
    @DisplayName("el puerto de salida se implementa en la capa de infraestructura")
    void portIsImplementedByInfrastructure() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.accenture.franchise");

        classes()
                .that().implement("com.accenture.franchise.domain.port.out.FranchiseRepositoryPort")
                .should().resideInAPackage("..infrastructure..")
                .check(classes);
    }
}
