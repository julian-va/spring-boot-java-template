package jvacloud.spring_boot_java_template;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "jvacloud.spring_boot_java_template",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

  @ArchTest
  static final ArchRule domain_should_not_depend_on_infrastructure =
      noClasses().that().resideInAPackage("..domain..")
          .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule use_cases_should_only_depend_on_domain =
      classes().that().resideInAPackage("..domain.usecase..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "..domain..",
              "java..",
              "org.slf4j..",
              "org.springframework.stereotype..",
              "org.springframework.transaction.annotation.."
          )
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule providers_should_not_depend_on_domain =
      noClasses().that().resideInAPackage("..infrastructure.provider..")
          .should().dependOnClassesThat().resideInAPackage("..domain..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule entrypoints_should_not_depend_on_adapters_or_providers =
      noClasses().that().resideInAPackage("..infrastructure.entrypoint..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("..infrastructure.adapter..", "..infrastructure.provider..")
          .allowEmptyShould(true);
}
