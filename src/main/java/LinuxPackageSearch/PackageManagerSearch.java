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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import java.util.List;
import java.util.Scanner;

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
	public String ubuntuRepos = "http://archive.ubuntu.com/ubuntu/dists/";
	public String[] ubuntuCodeName = { "focal", "bionic" };
	public String[] ubuntuPkg = {
			"/main/i18n/Translation-fr.gz",
			"/restricted/i18n/Translation-fr.gz",
			"/universe/i18n/Translation-fr.gz",
			"/multiverse/i18n/Translation-fr.gz",
			"/main/binary-amd64/Packages.gz",
			"/restricted/binary-amd64/Packages.gz",
			"/universe/binary-amd64/Packages.gz",
			"/multiverse/binary-amd64/Packages.gz"
			};

	@Override
	public void start(Future<Void> startFuture) {

		EventBus eb = vertx.eventBus();

		MessageConsumer<String> consumer = eb.consumer("linux", message -> {
			JsonArray arrayJsonArray = new JsonArray();
			String pkgName = message.body().toString();
			List<Future> futures = new ArrayList<>();

			// Research implementation
			try {
				WebClientOptions options = new WebClientOptions();
				WebClient client = WebClient.create(vertx, options);
				
				List<Promise<JsonArray>> promises = new ArrayList<>();
				for (int i = 0; i < ubuntuPkg.length; i++) {
					
					Promise<JsonArray> promise = Promise.promise();
					futures.add(promise.future());
					promises.add(promise);
					String repository = ubuntuRepos + ubuntuCodeName[1] + ubuntuPkg[i];
					// Send a GET request
					client.getAbs(repository).followRedirects(true).send(ar -> {
						JsonArray jsonArray = new JsonArray();
						if (ar.succeeded()) {
							
							HttpResponse<Buffer> response = ar.result();
							//System.out.println("URL : " + repository);
							try {
								ByteArrayInputStream responseBytes = new ByteArrayInputStream(response.body().getBytes());
								GZIPInputStream responseGzip = new GZIPInputStream(responseBytes);
								InputStreamReader responseContent = new InputStreamReader(responseGzip, "UTF-8");
								BufferedReader data = new BufferedReader(responseContent);
								
								Pattern blockNamePattern = Pattern.compile("^" + pkgName + "(.*)");
								Pattern blockDescriptionPattern = Pattern.compile("Description(-fr)?: (.*)");
								
								Scanner scanner = new Scanner(data).useDelimiter("Package: ");
								while (scanner.hasNext()) {
									
									String bloc = scanner.next();
									Matcher m = blockNamePattern.matcher(bloc);
									JsonObject pkgObj = new JsonObject();
									
									if (bloc.startsWith((pkgName))) {
										
										if (m.find()) {
											pkgObj.put("Package", (m.group(0)));
											m = blockDescriptionPattern.matcher(bloc);
											
											if (m.find()) {
												pkgObj.put("Description", (m.group(2)));
												jsonArray.add(pkgObj);
												//System.out.println("LINK : " + repository);
												//System.out.println(jsonArray.encodePrettily() + "\n\n");
											}
										}
									}
								}
								scanner.close();
								
							} catch (IOException e) {
								e.printStackTrace();
							}
							promise.complete(jsonArray);
						} else {
							System.out.println("GET : Something went wrong " + ar.cause().getMessage());
							promise.fail(ar.cause().getMessage());
						}
					});
				}
				CompositeFuture.all(futures).onComplete(ar -> {
					futures.forEach( fut -> {
					    if (fut.succeeded() && fut.result() != null) {
					    	arrayJsonArray.add(fut.result());
					    } else {
					    	System.out.println( "futures failed " + fut + " : " + fut.result() );
					    }
					  });
					message.reply(arrayJsonArray.encodePrettily());
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
