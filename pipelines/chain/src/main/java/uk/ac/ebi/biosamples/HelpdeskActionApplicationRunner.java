/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.auth.services.SamplesAuthChangeHandler;
import uk.ac.ebi.biosamples.auth.services.StaticAuthChangeHandler;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SampleStatus;
import uk.ac.ebi.biosamples.helpdesk.services.*;

/**
 * Application runner for executing various helpdesk actions via command line arguments.
 *
 * <p>This class provides a command-line interface for performing various sample management
 * operations. All operations require an --action argument, and many require additional arguments
 * for file paths, accessions, or other configuration values.
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>
 * --action=restoreIPKSamples --inputFile=path/to/samples.list --outputFile=results.txt
 * --action=changeStatusOfSamplesFromFile --inputFile=path/to/samples.txt --status=SUPPRESSED
 * --action=updateSampleRelationships --inputFile=path/to/mapping.xlsx
 * --action=addExternalReferenceCurations --accession=SAMEA115414646
 * --action=makeSamplesPublicFromStaticSamplesList --inputFile=path/to/samples.txt
 * --action=crawlSamplesAndChangeAuthInfoFromAAPToWebin --domain=example.com --webinId=WEBIN-12345
 * --action=authChangeHandlerStaticSampleList --inputFile=path/to/samples.txt --aapDomain=example.com
 * --webinIdCurrent=Webin-12345 --webinIdToChange=Webin-67890
 * --action=authChangeHandlerStaticSampleList --accessions=SAMEA7936841,SAMEA7936942
 * --aapDomain=example.com --webinIdCurrent=Webin-12345 --webinIdToChange=Webin-67890
 * --action=makeSamplesEnaComplaint --inputFile=path/to/samples.txt
 * --action=bulkAccessionSamples --count=500 --outputFile=path/to/accessions.txt
 * --action=fixSraAccessionFromFile --inputFile=path/to/accessions.txt
 * --action=dumpSamplesByWebinId --webinId=Webin-12345 --outputFile=path/to/samples.txt
 * --action=dumpSamplesFromExcel --inputFile=path/to/samples.xlsx --outputFile=path/to/samples.jsonl
 * --action=fetchMicrobeSyntheticCommunitySamples --size=20
 * </pre>
 *
 * <p><b>Available Actions:</b>
 *
 * <ul>
 *   <li><b>authChangeHandlerStaticSampleList</b> - Processes sample authentication changes from a
 *       list of samples. Required arguments:
 *       <ul>
 *         <li>--aapDomain: The AAP domain to change from
 *         <li>--webinIdCurrent: The current Webin submission account ID to change from
 *         <li>--webinIdToChange: The Webin submission account ID to change to
 *       </ul>
 *       Also requires one of:
 *       <ul>
 *         <li>--inputFile: Path to the input file containing sample accessions (one per line)
 *         <li>--accessions: Comma-separated list of sample accessions (e.g.,
 *             SAMEA7936841,SAMEA7936942)
 *       </ul>
 *   <li><b>makeSamplesEnaComplaint</b> - Makes samples ENA compliant by reading each sample's
 *       geo_loc_name and setting geographic location attributes. Required arguments:
 *       <ul>
 *         <li>--inputFile: Path to the input file containing sample accessions (SAMN accessions
 *             will be extracted)
 *       </ul>
 *       Uses geo_loc_name: single value sets "geographic location (country and/or sea)"; format
 *       "Country: Region" (e.g. USA:Virginia, China: Jilong, Tibet) sets country/sea and
 *       "geographic location (region and locality)".
 *   <li><b>restoreIPKSamples</b> - Restores IPK samples from a file containing accessions. Required
 *       arguments:
 *       <ul>
 *         <li>--inputFile: Path to the input file containing sample accessions (one per line)
 *         <li>--outputFile: Path to the output file where update results will be written (optional,
 *             defaults to "updateResults.txt")
 *       </ul>
 *   <li><b>changeStatusOfSamplesFromFile</b> - Changes the status of samples listed in a file.
 *       Required arguments:
 *       <ul>
 *         <li>--inputFile: Path to the input file containing sample accessions (one per line)
 *         <li>--status: The status to set (optional, if not provided no status change will occur).
 *             Valid values: DRAFT, PRIVATE, PUBLIC, CANCELLED, SUPPRESSED, KILLED
 *             (case-insensitive)
 *       </ul>
 *   <li><b>updateSampleRelationships</b> - Updates sample relationships from an Excel file.
 *       Required arguments:
 *       <ul>
 *         <li>--inputFile: Path to the Excel file (.xlsx) containing parent-child sample mappings
 *       </ul>
 *   <li><b>addExternalReferenceCurations</b> - Adds external reference curations to a specific
 *       sample. Required arguments:
 *       <ul>
 *         <li>--accession: The sample accession to process (e.g., SAMEA115414646)
 *       </ul>
 *   <li><b>makeSamplesPrivate</b> - Makes filtered samples private based on project filter. No
 *       additional arguments required.
 *   <li><b>makeSamplesPublicFromStaticSamplesList</b> - Makes samples public from a file containing
 *       accessions. Required arguments:
 *       <ul>
 *         <li>--inputFile: Path to the input file containing sample accessions (one per line)
 *       </ul>
 *   <li><b>crawlSamplesAndChangeAuthInfoFromAAPToWebin</b> - Crawls samples from a domain and
 *       changes authentication from AAP to Webin. Required arguments:
 *       <ul>
 *         <li>--domain: The AAP domain to crawl samples from
 *         <li>--webinId: The Webin submission account ID
 *       </ul>
 *   <li><b>bulkAccessionSamples</b> - Creates a number of samples with names TARACORAL1,
 *       TARACORAL2, ... (release 100 years in future), accessions them via V2 bulk-accession in
 *       batches of 200, and writes sample name → accession map to a file. Required arguments:
 *       <ul>
 *         <li>--count: Number of samples to create and accession (positive integer)
 *         <li>--outputFile: Path to output file for name→accession lines (tab-separated)
 *       </ul>
 *   <li><b>fixSraAccessionFromFile</b> - Reads a tab-separated file (e.g. TARACORAL1
 *       SAMEA121392853), fetches each sample by SAMEA accession, removes the wrongly set SRA
 *       accession attribute, and persists the updated sample via V2. Required arguments:
 *       <ul>
 *         <li>--inputFile: Path to file with lines "sampleName\taccession"
 *       </ul>
 *   <li><b>dumpSamplesByWebinId</b> - Searches samples by webinSubmissionAccountId and dumps
 *       matching samples to an output file. Required arguments:
 *       <ul>
 *         <li>--webinId: The webin submission account id (e.g. Webin-12345)
 *         <li>--outputFile: Path to output file; each line is a full sample JSON object
 *       </ul>
 *   <li><b>dumpSamplesFromExcel</b> - Reads the sampleId column from an Excel sheet, fetches
 *       samples by accession in batches of 1000 (with retries), and writes full sample JSON lines
 *       to output. Required arguments:
 *       <ul>
 *         <li>--inputFile: Path to source Excel file containing sampleId column
 *         <li>--outputFile: Path to output JSONL file
 *       </ul>
 *   <li><b>fetchMicrobeSyntheticCommunitySamples</b> - Fetches samples from
 *       https://www.ebi.ac.uk/biosamples/samples?filter=attr:project+name:MICROBE&amp;filter=attr:sample%20type:synthetic%20community
 *       into an ArrayList while following HAL cursor next links. Optional arguments:
 *       <ul>
 *         <li>--size: Cursor page size for the initial request (defaults to 20)
 *       </ul>
 * </ul>
 *
 * @author BioSamples Team
 * @since 2021
 */
@Component
@Slf4j
public class HelpdeskActionApplicationRunner implements ApplicationRunner {
  @Autowired SamplesEnaSpatiotemporalComplainceHandler samplesEnaSpatiotemporalComplainceHandler;
  @Autowired SamplesStatusUpdater samplesStatusUpdater;
  @Autowired StaticAuthChangeHandler staticAuthChangeHandler;
  @Autowired SamplesRelationshipHandler samplesRelationshipHandler;
  @Autowired SamplesExternalReferenceHandler samplesExternalReferenceHandler;
  @Autowired SamplesRestoreIPK samplesRestoreIPK;
  @Autowired SamplesAuthChangeHandler samplesAuthChangeHandler;
  @Autowired BulkAccessionService bulkAccessionService;
  @Autowired FixSraAccessionFromFileService fixSraAccessionFromFileService;
  @Autowired DumpSamplesByWebinIdService dumpSamplesByWebinIdService;
  @Autowired DumpSamplesFromExcelService dumpSamplesFromExcelService;
  @Autowired MicrobeSampleTypeHandler microbeSampleTypeHandler;

  @Override
  public void run(ApplicationArguments args) {
    try {
      final String action =
          args.containsOption("action") ? args.getOptionValues("action").get(0) : "";

      switch (action) {
        case "authChangeHandlerStaticSampleList" -> {
          log.info("Executing action: authChangeHandlerStaticSampleList");
          final String aapDomain =
              getRequiredArgument(args, "aapDomain", "authChangeHandlerStaticSampleList", true);
          final String webinIdCurrent =
              getRequiredArgument(args, "webinIdCurrent", "authChangeHandlerStaticSampleList");
          final String webinIdToChange =
              getRequiredArgument(args, "webinIdToChange", "authChangeHandlerStaticSampleList");
          List<String> samples;

          if (args.containsOption("inputFile")) {
            final String inputFile =
                getRequiredArgument(args, "inputFile", "authChangeHandlerStaticSampleList");

            log.info("Reading samples from file: {}", inputFile);

            samples = readAccessionsFromFile(inputFile);
          } else if (args.containsOption("accessions")) {
            final String accessionsArg =
                getRequiredArgument(args, "accessions", "authChangeHandlerStaticSampleList");

            log.info("Processing comma-separated accessions: {}", accessionsArg);

            samples =
                Arrays.stream(accessionsArg.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
          } else {
            throw new IllegalArgumentException(
                "Action 'authChangeHandlerStaticSampleList' requires either --inputFile or --accessions argument.");
          }

          log.info("Number of samples to process: {}", samples.size());
          staticAuthChangeHandler.parseListOfSamplesAndProcessSampleAuthentication(
              samples, aapDomain, webinIdCurrent, webinIdToChange);
        }

        case "makeSamplesEnaComplaint" -> {
          log.info("Executing action: makeSamplesEnaComplaint");
          final String inputFile =
              getRequiredArgument(args, "inputFile", "makeSamplesEnaComplaint");

          log.info("Input file: {}", inputFile);
          samplesEnaSpatiotemporalComplainceHandler.updateSamnSampleGeographicLocationFromFile(
              inputFile);
        }

        case "changeStatusOfSamplesFromFile" -> {
          log.info("Executing action: changeStatusOfSamplesFromFile");
          final String inputFile =
              getRequiredArgument(args, "inputFile", "changeStatusOfSamplesFromFile");

          SampleStatus status = null;

          if (args.containsOption("status")) {
            final String statusArg = args.getOptionValues("status").get(0);
            log.info("Status argument provided: {}", statusArg);

            status = parseSampleStatus(statusArg);
            log.info("Parsed status: {}", status);
          } else {
            log.info(
                "No status argument provided, samples will be processed without status change");
          }

          log.info("Input file: {}", inputFile);

          final List<String> accessions =
              samplesStatusUpdater.parseFileAndGetSampleAccessionList(inputFile);

          log.info("Number of accessions to process: {}", accessions.size());
          samplesStatusUpdater.processSamples(accessions, status);
        }

        case "updateSampleRelationships" -> {
          log.info("Executing action: updateSampleRelationships");
          final String inputFile =
              getRequiredArgument(args, "inputFile", "updateSampleRelationships");

          log.info("Input file: {}", inputFile);
          samplesRelationshipHandler.processFile(inputFile);
        }

        case "addExternalReferenceCurations" -> {
          log.info("Executing action: addExternalReferenceCurations");
          final String accession =
              getRequiredArgument(args, "accession", "addExternalReferenceCurations");

          log.info("Processing sample accession: {}", accession);
          samplesExternalReferenceHandler.processSample(accession);
        }

        case "makeSamplesPublicFromStaticSamplesList" -> {
          log.info("Executing action: makeSamplesPublicFromStaticSamplesList");
          final String inputFile =
              getRequiredArgument(args, "inputFile", "makeSamplesPublicFromStaticSamplesList");

          log.info("Input file: {}", inputFile);

          final List<String> toBePublicSampleAccessions =
              samplesStatusUpdater.parseFileAndGetSampleAccessionList(inputFile);

          log.info("Number of accessions to make public: {}", toBePublicSampleAccessions.size());
          samplesStatusUpdater.processSamples(toBePublicSampleAccessions, SampleStatus.PUBLIC);
        }

        case "crawlSamplesAndChangeAuthInfoFromAAPToWebin" -> {
          log.info("Executing action: crawlSamplesAndChangeAuthInfoFromAAPToWebin");
          final String domain =
              getRequiredArgument(args, "domain", "crawlSamplesAndChangeAuthInfoFromAAPToWebin");
          final String webinId =
              getRequiredArgument(args, "webinId", "crawlSamplesAndChangeAuthInfoFromAAPToWebin");

          log.info("Domain: {}, WebinId: {}", domain, webinId);

          for (EntityModel<Sample> sample : samplesAuthChangeHandler.getSamples(domain)) {
            samplesAuthChangeHandler.handleAuth(sample, domain, webinId);
          }
        }

        case "bulkAccessionSamples" -> {
          log.info("Executing action: bulkAccessionSamples");
          final String countArg = getRequiredArgument(args, "count", "bulkAccessionSamples");
          final String outputFile = getRequiredArgument(args, "outputFile", "bulkAccessionSamples");
          final int count = parsePositiveInt(countArg, "count");

          log.info("Count: {}, Output file: {}", count, outputFile);
          bulkAccessionService.createAndAccessionSamples(count, outputFile);
        }

        case "dumpSamplesByWebinId" -> {
          log.info("Executing action: dumpSamplesByWebinId");
          final String webinId = getRequiredArgument(args, "webinId", "dumpSamplesByWebinId");
          final String outputFile = getRequiredArgument(args, "outputFile", "dumpSamplesByWebinId");

          log.info("WebinId: {}, Output file: {}", webinId, outputFile);
          dumpSamplesByWebinIdService.dumpSamplesByWebinId(webinId, outputFile);
        }

        case "dumpSamplesFromExcel" -> {
          log.info("Executing action: dumpSamplesFromExcel");
          final String inputFile = getRequiredArgument(args, "inputFile", "dumpSamplesFromExcel");
          final String outputFile = getRequiredArgument(args, "outputFile", "dumpSamplesFromExcel");

          log.info("Input file: {}, Output file: {}", inputFile, outputFile);
          dumpSamplesFromExcelService.dumpFromExcelSampleIdColumn(inputFile, outputFile);
        }

        case "fetchMicrobeSyntheticCommunitySamples" -> {
          log.info("Executing action: fetchMicrobeSyntheticCommunitySamples");
          final int size =
              args.containsOption("size")
                  ? parsePositiveInt(args.getOptionValues("size").get(0), "size")
                  : 20;

          final List<Sample> samples = microbeSampleTypeHandler.fetchSamples(size);

          log.info("Collected {} samples into ArrayList", samples.size());

          for (final Sample sample : samples) {
            microbeSampleTypeHandler.updateSample(sample);
          }
        }

        /*NOTE: don't use the below action */
        /*case "fixSraAccessionFromFile" -> {
          log.info("Executing action: fixSraAccessionFromFile");
          final String inputFile = getRequiredArgument(args, "inputFile", "fixSraAccessionFromFile");

          log.info("Input file: {}", inputFile);
          fixSraAccessionFromFileService.fixSraAccessionFromFile(inputFile);
        }*/

        default -> {
          log.warn("No valid --action argument provided. Nothing will run.");
          log.warn(
              "Available actions: authChangeHandlerStaticSampleList, makeSamplesEnaComplaint, "
                  + "restoreIPKSamples, changeStatusOfSamplesFromFile, updateSampleRelationships, "
                  + "addExternalReferenceCurations, makeSamplesPrivate, "
                  + "makeSamplesPublicFromStaticSamplesList, crawlSamplesAndChangeAuthInfoFromAAPToWebin, "
                  + "bulkAccessionSamples, fixSraAccessionFromFile, dumpSamplesByWebinId, "
                  + "dumpSamplesFromExcel, fetchMicrobeSyntheticCommunitySamples");
        }
      }

    } catch (final Exception e) {
      log.error("Operation failed", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves a required command line argument and throws an exception if it's missing.
   *
   * @param args the application arguments
   * @param argumentName the name of the required argument (without -- prefix)
   * @param actionName the name of the action requiring this argument (for error messages)
   * @return the argument value
   * @throws IllegalArgumentException if the argument is missing
   */
  private String getRequiredArgument(
      ApplicationArguments args, String argumentName, String actionName) {
    final List<String> values = args.getOptionValues(argumentName);
    if (!args.containsOption(argumentName)
        || values == null
        || values.isEmpty()
        || values.get(0).trim().isEmpty()) {
      throw new IllegalArgumentException(
          String.format(
              "Action '%s' requires --%s argument. Please provide a valid value.",
              actionName, argumentName));
    }

    return values.get(0).trim();
  }

  private String getRequiredArgument(
      ApplicationArguments args, String argumentName, String actionName, boolean optional) {
    final List<String> values = args.getOptionValues(argumentName);

    if (!optional) {
      if (!args.containsOption(argumentName)
          || values == null
          || values.isEmpty()
          || values.get(0).trim().isEmpty()) {
        throw new IllegalArgumentException(
            String.format(
                "Action '%s' requires --%s argument. Please provide a valid value.",
                actionName, argumentName));
      }

      return values.get(0).trim();
    } else {
      return null;
    }
  }

  /**
   * Reads sample accessions from a file (one per line).
   *
   * @param filePath the path to the file containing accessions
   * @return a list of accessions read from the file
   * @throws RuntimeException if the file cannot be read
   */
  private List<String> readAccessionsFromFile(final String filePath) {
    final List<String> accessions = new ArrayList<>();

    try (final BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;

      while ((line = br.readLine()) != null) {
        final String trimmed = line.trim();
        if (!trimmed.isEmpty()) {
          accessions.add(trimmed);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file: " + filePath, e);
    }
    return accessions;
  }

  /**
   * Parses a string to a positive integer (e.g. for --count).
   *
   * @param value the string to parse
   * @param argumentName argument name for error messages
   * @return the parsed positive integer
   * @throws IllegalArgumentException if the value is not a positive integer
   */
  private int parsePositiveInt(final String value, final String argumentName) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(argumentName + " cannot be null or empty");
    }
    try {
      final int n = Integer.parseInt(value.trim());
      if (n <= 0) {
        throw new IllegalArgumentException(argumentName + " must be positive, got: " + n);
      }
      return n;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          argumentName + " must be a valid positive integer, got: " + value, e);
    }
  }

  /**
   * Parses a string to a SampleStatus enum value (case-insensitive).
   *
   * @param statusStr the status string to parse
   * @return the corresponding SampleStatus enum value
   * @throws IllegalArgumentException if the status string is not a valid SampleStatus value
   */
  private SampleStatus parseSampleStatus(final String statusStr) {
    if (statusStr == null || statusStr.trim().isEmpty()) {
      throw new IllegalArgumentException("Status cannot be null or empty");
    }

    try {
      return SampleStatus.valueOf(statusStr.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid status '%s'. Valid values are: %s",
              statusStr, Arrays.toString(SampleStatus.values())),
          e);
    }
  }
}
