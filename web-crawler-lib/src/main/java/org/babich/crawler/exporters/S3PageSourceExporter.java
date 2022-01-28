/*
 * @author Vadim Babich
 */
package org.babich.crawler.exporters;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.messages.CrawlerStarted;
import org.babich.crawler.api.messages.PageProcessingComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;

import static org.babich.crawler.metrics.Utils.loadProperties;

/**
 * This is an extension for saving original pages to Amazon S3 storage.
 * This is configured through the properties file. The path to the properties file is passed to the class constructor.
 * <p>An example</p>
 * <pre>
 * eventListeners:
 *     ...
 *   - !!org.babich.crawler.exporters.S3PageSourceExporter { propertyFile : './web-crawler-lib/src/main/resources/aws.properties' }
 * </pre>
 *
 * aws.properties content:
 * <pre>
 *  aws.bucketName=crawler
 *  aws.keyName=test
 *  aws.accessKeyId=minio
 *  aws.secretKey=minio123
 *  aws.enabled=false
 * </pre>
 */
public class S3PageSourceExporter {

    private static final Logger logger = LoggerFactory.getLogger(S3PageSourceExporter.class);
    private static final String KEY_DELIMITER = "/";

    private final S3ExporterConfig config;
    private final AmazonS3 s3Client;

    private String crawlerName = "none";
    private String keyName;


    interface RegistryConfig {
        String prefix();

        String get(String key);
    }

    interface S3ExporterConfig extends AWSCredentials, RegistryConfig {

        default boolean isEnabled(){
            return getString(this, "enabled")
                    .map(Boolean::parseBoolean)
                    .orElse(false);
        }

        @Override
        default String getAWSAccessKeyId() {
            return getString(this, "accessKeyId")
                    .orElseThrow(() -> new IllegalArgumentException("AWSAccessKeyId cannot be empty."));
        }

        @Override
        default String getAWSSecretKey() {
            return getString(this, "secretKey")
                    .orElseThrow(() -> new IllegalArgumentException("AWSSecretKey cannot be empty."));
        }

        static Optional<String> getString(RegistryConfig config, String property) {
            String prefixedProperty = prefixedProperty(config, property);
            return Optional.ofNullable(config.get(prefixedProperty));
        }

        static String prefixedProperty(RegistryConfig config, String property) {
            return config.prefix() + '.' + property;
        }

        @Override
        default String prefix() {
            return "aws";
        }

        default String getBucketName() {
            return getString(this, "bucketName").orElse("crawler");
        }

        default String getKeyName() {
            return getString(this, "keyName").orElse(StringUtils.EMPTY);
        }

        default String getUri() {
            return getString(this, "uri").orElse("http://localhost:9000");
        }

        default String getRegion() {
            return getString(this, "region").orElse("eu-central-1");
        }

    }

    S3PageSourceExporter() {
        config = null;
        s3Client = null;
    }

    public S3PageSourceExporter(String propertyFile) {
        this.config = new S3ExporterConfig() {
            final Properties properties = loadProperties(propertyFile);

            {
                logger.info("Amazon S3 properties: {} ", properties.toString());
            }

            @Override
            public String get(String key) {
                return properties.getProperty(key);
            }
        };

        try {
            s3Client = init(this.config);
        }catch (Exception e){
            logger.error("S3 page source exported cannot be initialized", e);
            throw e;
        }
    }

    private AmazonS3 init(S3ExporterConfig config) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

        AWSCredentials credentials = new BasicAWSCredentials(config.getAWSAccessKeyId(), config.getAWSSecretKey());
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder
                .EndpointConfiguration(config.getUri(), Regions.fromName(config.getRegion()).getName());

        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    @Subscribe
    public void onStart(CrawlerStarted message) {
        String folderName = "sources";
        this.crawlerName = (String) message.getPayload();
        String dataTime = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());

        this.keyName = String.join(KEY_DELIMITER, config.getKeyName(), crawlerName, folderName, dataTime);
        executeRequest(this::createBucketIfNecessary);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onProcess(PageProcessingComplete message) {
        Page page = message.getPage();
        getURI(page.getPageSource()).map(Path::toFile).ifPresent(this::uploadObject);
    }

    private Optional<Path> getURI(String path) {
        try {
            return Optional.of(Paths.get(new URI(path)));
        } catch (URISyntaxException e) {
            if (logger.isDebugEnabled()) {
                logger.error("Cannot parse page source uri.", e);
            }
            return Optional.empty();
        }
    }

    void uploadObject(File file) {
        if (logger.isDebugEnabled()) {
            logger.debug("Uploading a new object to S3 from a file. {}", file.getAbsolutePath());
        }

        executeRequest(() ->
                s3Client.putObject(
                        new PutObjectRequest(config.getBucketName(), getKeyNameFor(file), file)
                )
        );
    }

    private String getKeyNameFor(File file) {
        return keyName + KEY_DELIMITER + file.getName() + ".src";
    }

    private void createBucketIfNecessary() {
        String bucketName = config.getBucketName();
        if (!s3Client.doesBucketExist(bucketName)) {
            s3Client.createBucket(bucketName);
        }
    }

    void executeRequest(Runnable runnable) {
        if(!config.isEnabled()) {
            logger.debug("S3 service is disabled.");
            return;
        }

        try {
            runnable.run();
        } catch (AmazonServiceException ase) {
            logger.error("Caught an AmazonServiceException, which means your request made it "
                            + "to Amazon S3, but was rejected with an error response for some reason. " +
                            "\n Error Message:    {}" +
                            "\n HTTP Status Code: {}" +
                            "\n AWS Error Code:   {}" +
                            "\n Error Type:       {}" +
                            "\n Request ID:       {}",
                    ase.getMessage(), ase.getStatusCode(), ase.getErrorCode(), ase.getErrorType(), ase.getRequestId());
        } catch (AmazonClientException ace) {
            logger.error("Caught an AmazonClientException, which means the client encountered " +
                    "an internal error while trying to communicate with S3, " +
                    "such as not being able to access the network." +
                    "\n Error Message: {}", ace.getMessage());
        }
    }
}
