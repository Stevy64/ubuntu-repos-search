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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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
			"/universe/binary-amd64/Packages.gz" };

	@Override
	public void start(Future<Void> startFuture) {

		EventBus eb = vertx.eventBus();

		MessageConsumer<String> consumer = eb.consumer("linux", message -> {
			String pkgName = message.body().toString();

			// Search implementation
			try {
				WebClientOptions options = new WebClientOptions();
				WebClient client = WebClient.create(vertx, options);

				String repository = ubuntuRepos + ubuntuCodeName[0] + ubuntuPkg[0];

				Pattern packageNamePattern = Pattern.compile("Package: (.*)");
				Pattern descriptionPattern = Pattern.compile("Description(-fr)?: (.*)");
				JsonArray jsonArray = new JsonArray();

				// Send a GET request
				client.getAbs(repository).followRedirects(true).send(ar -> {
					if (ar.succeeded()) {
						// Obtain response
						HttpResponse<Buffer> response = ar.result();
						System.out.println("URL : " + repository);
						try {
							ByteArrayInputStream responseBytes = new ByteArrayInputStream(response.body().getBytes());
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
								if (block.startsWith("Package") && block.contains(pkgName)) {
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
									//System.out.println("Reply back : " + jsonArray.encodePrettily());
									//message.reply(jsonArray.encodePrettily());
								}
								//System.out.println("Reply back : " + jsonArray.encodePrettily());
								//message.reply(jsonArray.encodePrettily());
							}
							message.reply(jsonArray.encodePrettily());

						} catch (IOException e) {
							e.printStackTrace();
						}

					} else {
						System.out.println("GET : Something went wrong " + ar.cause().getMessage());
					}
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
