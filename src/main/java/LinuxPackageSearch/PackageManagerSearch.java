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

			// Search implementation
			try {
				WebClientOptions options = new WebClientOptions();
				WebClient client = WebClient.create(vertx, options);

				Pattern packageNamePattern = Pattern.compile("Package: (.*)");
				Pattern descriptionPattern = Pattern.compile("Description(-fr)?: (.*)");
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
							// Obtain response
							HttpResponse<Buffer> response = ar.result();
							//System.out.println("URL : " + repository);
							try {
								ByteArrayInputStream responseBytes = new ByteArrayInputStream(
										response.body().getBytes());
								GZIPInputStream responseGzip = new GZIPInputStream(responseBytes);
								InputStreamReader responseContent = new InputStreamReader(responseGzip, "UTF-8");
								BufferedReader data = new BufferedReader(responseContent);

								StringBuffer sb = new StringBuffer();
								while (true) {
									String line = data.readLine();
									if (line == "" || line == null)
										break;
									sb.append(line).append("\n");
								}

								String[] blocks = sb.toString().split("\n\n");
								for (String block : blocks) {
									if (block.startsWith("Package: "+pkgName)) {
										String[] lines = block.split("\n");
										JsonObject pkgObj = new JsonObject();
										for (String line : lines) {
											Matcher m = packageNamePattern.matcher(line);
											if (m.matches()) {
												pkgObj.put("Package", (m.group(1)));
											} else {
												m = descriptionPattern.matcher(line);
												if (m.matches()) {
													pkgObj.put("Description", (m.group(2)));
												}
											}
										}
										jsonArray.add(pkgObj);
										// arrayJsonArray.add(jsonArray);
										// System.out.println("Replying back : " + jsonArray.encodePrettily());
										// message.reply(jsonArray.encodePrettily());
									}
								}

							} catch (IOException e) {
								e.printStackTrace();
							}
							if (!jsonArray.isEmpty()) {
								promise.complete(jsonArray);
								//System.out.println("Promise : " + "URL =" + repository + " Result => " + promise.future().result());
							}
							else {
								promise.complete();
							}
						} else {
							System.out.println("GET : Something went wrong " + ar.cause().getMessage());
							promise.fail(ar.cause().getMessage());
						}
					});
					// arrayJsonArray.add(jsonArray);
					// System.out.println("Replying back : " + arrayJsonArray.encodePrettily());
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
