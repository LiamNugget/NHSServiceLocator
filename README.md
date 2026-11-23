# NHS Service Locator

A Java desktop app for finding nearby healthcare services by UK postcode. Filter by service type, keyword, and distance, then open a result to see its contact details and CQC record.

## Setup

Install **JDK 21** and **Maven**, then place both datasets in the project's `data/` folder.

| File | Contents |
| --- | --- |
| `data/healthcare_services_2026.csv` | CQC healthcare service listings |
| `data/uk_postcode_coordinates_2026.csv` | UK postcode coordinates |

Git ignores these CSVs, so add them locally after cloning the repository.

## Run

From the folder containing `pom.xml`, run the following command.

```shell
mvn compile exec:java
```

## Search

Enter a postcode such as `E1 7BS`, choose a service type and radius, then select **Search**. Double-click a result or select **View Details** to see more.
