/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LinuxPackageSearch;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
//import java.io.File;
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.nio.charset.Charset;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.zip.GZIPInputStream;

/**
 *
 * @author steevy
 */
public class PackageManager extends AbstractVerticle {
	public String ubuntuRepos = "http://archive.ubuntu.com/ubuntu/dists/";
	
    @Override
    public void start(Future<Void> startFuture) {
        vertx.eventBus().consumer("linux", message -> {
            querryVerticle();
            System.out.println("Package received message: " + message.body());
        });
    }

    // Search implementation
    public void querryVerticle() {
    	WebClientOptions options = new WebClientOptions()
    			  .setUserAgent(ubuntuRepos);
    	options.setMaxRedirects(5);
        WebClient client = WebClient.create(vertx, options);

        // Send a GET request
        client
                .getAbs(ubuntuRepos + "/focal" + "/main/binary-amd64/Packages.gz")
                .followRedirects(true)
                .send(ar -> {
                    if (ar.succeeded()) {
                        // Obtain response
                        HttpResponse<Buffer> response = ar.result();
                        
                        System.out.println("GET : Received response with status code" + response.bodyAsString());
                    } else {
                        System.out.println("GET : Something went wrong " + ar.cause().getMessage());
                    }
                });

        // Send a HEAD request
        client
                .headAbs(ubuntuRepos + "/focal" + "/bionic/main/i18n/Translation-fr")
                .send(ar -> {
                    if (ar.succeeded()) {
                        // Obtain response
                        HttpResponse<Buffer> response = ar.result();

                        System.out.println("HEAD : Received response with status code" + response.bodyAsString());
                    } else {
                        System.out.println("HEAD : Something went wrong " + ar.cause().getMessage());
                    }
                });
    }

//    private Set<Package> searchPackage(String packageName, String dist) throws MalformedURLException, IOException {
//        Set<Package> packages = new HashSet<>();
//        String ubuntuCodename = "bionic";
//
//        Pattern packageNamePattern = Pattern.compile("Package: (.*)");
//        Pattern descriptionPattern = Pattern.compile("Description(-fr)?: (.*)");
//
//        Collection<URL> repos = new ArrayList<>();
//        String ubuntuRepo = "http://archive.ubuntu.com/ubuntu/dists/" + ubuntuCodename;
//
//        repos.add(new URL(ubuntuRepo + "/main/i18n/Translation-fr.gz"));
//        repos.add(new URL(ubuntuRepo + "/restricted/i18n/Translation-fr.gz"));
//        repos.add(new URL(ubuntuRepo + "/universe/i18n/Translation-fr.gz"));
//        repos.add(new URL(ubuntuRepo + "/multiverse/i18n/Translation-fr.gz"));
//        repos.add(new URL(ubuntuRepo + "/main/binary-amd64/Packages.gz"));
//        repos.add(new URL(ubuntuRepo + "/restricted/binary-amd64/Packages.gz"));
//        repos.add(new URL(ubuntuRepo + "/universe/binary-amd64/Packages.gz"));
//        repos.add(new URL(ubuntuRepo + "/multiverse/binary-amd64/Packages.gz"));
//
//        for (URL repo : repos) {
//            try {
//                LineIterator it = IOUtils.lineIterator(new GZIPInputStream(getCachedInputStream(repo)), Charset.defaultCharset());
//
//                Package p = null;
//                while (it.hasNext()) {
//                    String line = it.nextLine();
//
//                    Matcher m = packageNamePattern.matcher(line);
//                    if (m.matches()) {
//                        p = new Package(m.group(1));
//                        if (p.getName().contains(packageName)) {
//                            packages.add(p);
//                        }
//                    } else {
//                        m = descriptionPattern.matcher(line);
//                        if (m.matches()) {
//                            if (p != null) {
//                                p.setDescription(m.group(2));
//                            }
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                System.out.println("err : " + e);
//            }
//        }
//
//        return packages;
//    }
}
