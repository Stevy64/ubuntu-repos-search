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
public class PackageManagerSearchImproved extends AbstractVerticle {
	
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

		MessageConsumer<JsonObject> consumer = eb.consumer("kit.applications.case.sensitive", message -> {
			String pkgName = message.body().getString("Package").toLowerCase();
			String codeName = message.body().getString("UbuntuCodeName").toLowerCase();
			List<Future> futures = new ArrayList<>();

			// Research implementation
			try {
				WebClientOptions options = new WebClientOptions();
				WebClient client = WebClient.create(vertx, options);

				for (int i = 0; i < ubuntuPkg.length; i++) {

					Promise<JsonArray> promise = Promise.promise();
					futures.add(promise.future());
					String repository = UBUNTU_REPOS + codeName + ubuntuPkg[i];
					// Send a GET request
					System.out.println("Link : " + repository);
					client.getAbs(repository).followRedirects(true).send(ar -> {
						JsonArray jsonArray = new JsonArray();
						if (ar.succeeded()) {

							HttpResponse<Buffer> response = ar.result();
							// System.out.println("URL : " + repository);
							try {
								ByteArrayInputStream responseBytes = new ByteArrayInputStream(
										response.body().getBytes());
								GZIPInputStream responseGzip = new GZIPInputStream(responseBytes);
								InputStreamReader responseContent = new InputStreamReader(responseGzip, "UTF-8");
								BufferedReader data = new BufferedReader(responseContent);

//								Pattern blockNamePattern = Pattern.compile("^" + pkgName + "(.*)");
								
								Pattern blockNamePattern = Pattern.compile("[A-Z]");
								Scanner scanner = new Scanner(data).useDelimiter("Package: ");
								//System.out.println("Block : " + scanner.nextLine());
								
								StringBuffer sb = new StringBuffer();
						        while (true) {
						            String line = data.readLine();
						            if (line == "" || line == null) break;
						            	sb.append(line).append("\n");
						        }
						        
						        String[] blocks = sb.toString().split("\n\n");
						        for (String block : blocks) {
						        	JsonObject jsonObject = new JsonObject();
						        	//System.out.println("Contains : " + block);
						            //System.out.println("Block : " + block);
						        	String[] lines = block.split("\n");
					                for (String line : lines) {
					                	if (line.startsWith("Package: ")) {
											//System.out.println("Line : " + line);
											Matcher m = blockNamePattern.matcher(line.substring(8));
											if (m.find()) {
												System.out.println("FOUND-UPPER0 : " + m.group(0));
												System.out.println("Line : " + line);
											}
											}
										}
						        	
						        } 

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
					JsonArray arrayJsonArray = new JsonArray();
					futures.forEach(fut -> {
						if (fut.succeeded()) {
							arrayJsonArray.addAll((JsonArray) fut.result());
							System.out.println(arrayJsonArray.encodePrettily());
						} else {
							System.out.println("futures failed " + fut + " : " + fut.result());
						}
					});
					//message.reply(arrayJsonArray.encodePrettily());
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
