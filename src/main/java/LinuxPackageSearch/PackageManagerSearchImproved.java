/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LinuxPackageSearch;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import java.util.List;
import java.util.Scanner;
import java.util.Set;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 *
 * @author steevy
 */
public class PackageManagerSearchImproved extends AbstractVerticle {

    public static final String UBUNTU_REPOS = "http://archive.ubuntu.com/ubuntu/dists/";
    private String[] paths = {
        "/main/i18n/Translation-fr.gz",
        "/restricted/i18n/Translation-fr.gz",
        "/universe/i18n/Translation-fr.gz",
        "/multiverse/i18n/Translation-fr.gz",
        "/main/binary-amd64/Packages.gz",
        "/restricted/binary-amd64/Packages.gz",
        "/universe/binary-amd64/Packages.gz",
        "/multiverse/binary-amd64/Packages.gz"};

    @Override
    public void start(Future<Void> startFuture) {

        EventBus eb = vertx.eventBus();

        @SuppressWarnings("unchecked")
        MessageConsumer<JsonObject> consumer = eb.consumer("kit.applications.ubuntu.package.search", message -> {
            String pkgName = message.body().getString("name").toLowerCase();
            String codeName = message.body().getString("UbuntuCodeName").toLowerCase();
            List<Future> futures = new ArrayList<>();

            // Research implementation
            try {
                WebClientOptions options = new WebClientOptions();
                WebClient client = WebClient.create(vertx, options);

                for (String archive : paths) {
                    Promise<Set<Package>> promise = Promise.promise();
                    futures.add(promise.future());
                    String repository = UBUNTU_REPOS + codeName + archive;

                    // Send GET requests
                    client.getAbs(repository).followRedirects(true).send(ar -> {
                        @SuppressWarnings("unchecked")
                        Set<Package> setOfPackages = new HashSet();
                        if (ar.succeeded()) {

                            HttpResponse<Buffer> response = ar.result();
                            try {
                                ByteArrayInputStream responseBytes = new ByteArrayInputStream(response.body().getBytes());
                                GZIPInputStream responseGzip = new GZIPInputStream(responseBytes);

                                setOfPackages = gzipDataParser(responseGzip, pkgName, codeName, new HashSet<Package>());

                            } catch (IOException e) {
                                System.out.println("Exception handled : " + e);
                                promise.fail(e);
                            }
                            promise.complete(setOfPackages);
                        } else {
                        	System.out.println("MethodGET : Something went wrong " + ar.cause().toString());
                            promise.fail(ar.cause());
                        }
                    });
                }
                CompositeFuture.all(futures).setHandler(ar -> {
                    Set<Package> packages = new HashSet<>();
                    futures.forEach(fut -> {
                        if (fut.succeeded()) {
                            packages.addAll((Set<Package>) fut.result());
                        } else {
                        	System.out.println("futures failed " + fut + " : " + fut.result());
                            message.fail(1, "qv9d7m");
                        }
                    });

                    JsonArray jsonArray = new JsonArray();
                    packages.stream().limit(100).forEach(pkg -> {
                        jsonArray.add(new JsonObject()
                                .put("name", pkg.name)
                                .put("version", pkg.version)
                                .put("description", pkg.description));
                    });
                    message.reply(jsonArray);
                });

            } catch (Exception e) {
                message.fail(1, "qv9d8n");
            }
            System.out.println("Received pkgName toSearch: " + message.body());

        });

        consumer.completionHandler(res -> {
            if (!res.succeeded()) {
            	System.out.println("Failed : " + res.failed());
            }
        });
    }

    public Set<Package> gzipDataParser(GZIPInputStream responseGzip, String pkgName, String codeName, HashSet<Package> setOfPackages) throws UnsupportedEncodingException {

        InputStreamReader responseContent = new InputStreamReader(responseGzip, "UTF-8");
        BufferedReader data = new BufferedReader(responseContent);

        Pattern blockNamePattern = Pattern.compile("^" + pkgName + "(.*)");
        Pattern blockDescriptionPattern = Pattern.compile("Description(-fr)?: (.*)");
        Pattern blockVersionPattern = Pattern.compile("Version: (.*)");

        try (Scanner scanner = new Scanner(data).useDelimiter("Package: ")) {
            while (scanner.hasNext()) {

                String block = scanner.next();
                Package pkg = new Package();

                if (block.startsWith((pkgName))) {

                    Matcher m = blockNamePattern.matcher(block);

                    if (m.find()) {
                        pkg.name = (m.group(0));
                        m = blockVersionPattern.matcher(block);

                        if (m.find()) {
                            JsonObject version = new JsonObject();
                            version.put(codeName, (m.group(1)));
                            pkg.version = version;
                            m = blockDescriptionPattern.matcher(block);

                            if (m.find()) {
                                pkg.description = (m.group(2));
                                setOfPackages.add(pkg);
                            }
                        }
                    }
                }
            }
        }
        
        return setOfPackages;
    }

}
