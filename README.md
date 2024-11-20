# Cuke-To-ReportPortal

**Cuke-To-ReportPortal** is a library that is used to import cucumber json report files or `CukeTestRun` reports to ReportPortal. The library used the `cuke-report-converter` [package](https://mvnrepository.com/artifact/io.github.alexop-a/cuke-report-converter) to convert json report files to `CukeTestRun`. The import process supports parallelization over the Features that have to be imported and also on the Scenarios that belong to those features ( see properties below ).

### Installation
Add the following dependency in pom.xml file

	<dependency>
		<artifactId>cuke-to-reportportal</artifactId>
		<groupId>io.github.alexop-a</groupId>
		<version>1.0.5</version>
	</dependency>
 
## Usage
### Importing to ReportPortal

The simplest usage of the library is shown in the following example

```
RPImporterPropertyHandler propertyHandler = new RPImporterPropertyHandler();
ReportPortalImporter reportPortalImporter = new ReportPortalImporter(propertyHandler);
reportPortalImporter.importCucumberReports();
```

## Configuration

The following parameters are available:
| Property Name | Default Value  | Description |
|--|--|--|
| rp.importer.cucumberJsonFiles | | the cucumber report json files that will be imported for a launch |
| rp.importer.launch.name | | the launch name |
| rp.importer.launch.attributes | | the attributes (if any) for the launch. The attributes should be separated with semi-colon and each attribute should define the key/value pair separate with colon |
| rp.importer.launch.attachments | | the attachments (if any) for the launch. The attachments should be separated with semi-colon and each attachment should be the full path of the file |
| rp.importer.launch.description | | the description (if any) for the launch |
| rp.importer.launch.rerunOf | | defines if the specific launch is a rerun of another launch. In case that property needs to be used, it should contain the launch uuid of the initial launch |
| rp.importer.launch.mode | DEBUG | the mode of the launch (DEBUG/DEFAULT) |
| rp.importer.threads.features | 1 | the number of threads used in parallel for feature importing |
| rp.importer.threads.scenarios | 1 | the number of threads used in parallel for scenario importing. This value is applied for any of the feature thread that is running |
| rp.importer.reportPortal.projectName | | the project name on the ReportPortal instance that this launch will be imported |
| rp.importer.reportPortal.apiKey | | the api key of the project on the ReportPortal instance that this launch will be imported |
| rp.importer.reportPortal.endpoint | | the ReportPortal instance endpoint without the api path |

The above properties are initializeed by the `RPImporterPropertyHandler` class. The properties can be initialized in the following ways:
- reading properties from a properties file, either the default (`rp-cucumber-import.properties`) or from any other provided in the appropriate constructor
- supplying a `Properties` object
- by combining any of the above

*Note:* In case that both a properties file and the `Properties` object are used, then the values of the Properties object takes precedence over those  in the file.
