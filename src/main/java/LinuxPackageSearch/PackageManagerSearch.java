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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

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
public class PackageManagerSearch extends AbstractVerticle {
	public static final String UBUNTU_REPOS = "http://archive.ubuntu.com/ubuntu/dists/";
	private String[] ubuntuPkg = { "/main/i18n/Translation-fr.gz",
			"/restricted/i18n/Translation-fr.gz",
			"/universe/i18n/Translation-fr.gz",
			"/multiverse/i18n/Translation-fr.gz",
			"/main/binary-amd64/Packages.gz",
			"/restricted/binary-amd64/Packages.gz",
			"/universe/binary-amd64/Packages.gz",
			"/multiverse/binary-amd64/Packages.gz" };

	@Override
	public void start(Future<Void> startFuture) {

		EventBus eb = vertx.eventBus();

		MessageConsumer<JsonObject> consumer = eb.consumer("kit.applications.ubuntu.package.search", message -> {
			String pkgName = message.body().getString("Package").toLowerCase();
			String codeName = message.body().getString("UbuntuCodeName").toLowerCase();
			List<Future> futures = new ArrayList<>();

			// Research implementation
			try {
				WebClientOptions options = new WebClientOptions();
				WebClient client = WebClient.create(vertx, options);

				for (int i = 0; i < ubuntuPkg.length; i++) {

					Promise<Set<Package>> promise = Promise.promise();
					futures.add(promise.future());
					String repository = UBUNTU_REPOS + codeName + ubuntuPkg[i];
					// Send a GET request
					System.out.println("Link : " + repository);
					client.getAbs(repository).followRedirects(true).send(ar -> {
						Set<Package> setOfPackages = new HashSet();
						
						if (ar.succeeded()) {

							HttpResponse<Buffer> response = ar.result();
							// System.out.println("URL : " + repository);
							try {
								ByteArrayInputStream responseBytes = new ByteArrayInputStream(
										response.body().getBytes());
								GZIPInputStream responseGzip = new GZIPInputStream(responseBytes);
								InputStreamReader responseContent = new InputStreamReader(responseGzip, "UTF-8");
								BufferedReader data = new BufferedReader(responseContent);

								Pattern blockNamePattern = Pattern.compile("^" + pkgName + "(.*)");
								Pattern blockDescriptionPattern = Pattern.compile("Description(-fr)?: (.*)");

								Scanner scanner = new Scanner(data).useDelimiter("Package: ");
								while (scanner.hasNext()) {

									String block = scanner.next();
									Matcher m = blockNamePattern.matcher(block);
									Package pkg = new Package();

									if (block.startsWith((pkgName))) {

										if (m.find()) {
											pkg.name = (m.group(0));
											m = blockDescriptionPattern.matcher(block);

											if (m.find()) {
												pkg.description = (m.group(2));
												setOfPackages.add(pkg);
												//System.out.println("LINK : " + repository);
												//System.out.println(setOfPackages + "\n\n");
											}
										}
									}
								}
								scanner.close();

							} catch (IOException e) {
								e.printStackTrace();
							}
							promise.complete(setOfPackages);
						} else {
							System.out.println("Something went wrong " + ar.cause().getMessage());
							promise.fail(ar.cause().getMessage());
						}
					});
				}
				CompositeFuture.all(futures).onComplete(ar -> {
					Set<Package> set = new HashSet<Package>();
					futures.forEach(fut -> {
						if (fut.succeeded()) {
							set.addAll((Set<Package>) fut.result());
						} else {
							System.out.println("futures failed " + fut + " : " + fut.result());
						}
					});
					
					JsonArray jsonArray = new JsonArray();
				    for (Package pkg : set) {
				    	jsonArray.add(new JsonObject()
				                .put("name", pkg.name)
				                .put("description", pkg.description));
				    }
					System.out.println(jsonArray.encodePrettily());
					//message.reply(set.toString());
				});

			} catch (Exception e) {
				message.fail(-1, e.toString());
			}
			System.out.println("Received pkgName toSearch: " + message.body());

		});

		consumer.completionHandler(res -> {
			if (!res.succeeded()) {
				System.out.println("Failed : " + res.failed());
			}
		});

	}

}
